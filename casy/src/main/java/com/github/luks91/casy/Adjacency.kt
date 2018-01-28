/**
 * Copyright (c) 2018-present, Casy Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.casy

import com.github.luks91.casy.annotations.SyncEmitter
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal fun calculateAdjacency(roundEnv: RoundEnvironment, messager: Messager) =
        buildAdjacencyMap(roundEnv).apply {
            assertNoEmittersAreMissing(this, messager)
            assertNoCyclesInAdjacency(this, messager)
        }

private fun buildAdjacencyMap(roundEnv: RoundEnvironment): Map<String, Node> {
    val triggers = mutableMapOf<String, MutableSet<String>>().apply {
        roundEnv.getElementsAnnotatedWith(SyncEmitter::class.java).forEach {
            val nodeClass = it.asType().asTypeName().toString()
            val annotation = it.getAnnotation(SyncEmitter::class.java)
            val triggeredBy = annotation.getClasses { triggeredBy }

            triggeredBy.forEach { trigger -> getOrPut(trigger, { mutableSetOf() }).add(nodeClass) }
        }
    }

    return mutableMapOf<String, Node>().apply {
        roundEnv.getElementsAnnotatedWith(SyncEmitter::class.java).forEach {
            val nodeClass = it.asType().asTypeName().toString()
            val annotation = it.getAnnotation(SyncEmitter::class.java)
            put(nodeClass, Node(
                    nodeClass, annotation.topics,
                    mutableSetOf(*annotation.getClasses { syncsAfter }.plus(annotation.getClasses { triggeredBy })),
                    triggers[nodeClass] ?: emptySet())
            )
        }
    }
}

private inline fun SyncEmitter.getClasses(extractValueFor: SyncEmitter.() -> Array<KClass<*>>) =
        try {
            extractValueFor(this).map { it.jvmName }.toTypedArray()
        } catch (mte: MirroredTypesException) {
            (mte.typeMirrors as List<TypeMirror>).map { it.toString() }.toTypedArray()
        }

private fun assertNoEmittersAreMissing(adjacency: Map<String, Node>, messager: Messager) {
    adjacency.forEach { (nodeClazz, node) ->
        node.syncsAfter.forEach { clazz ->
            if (!adjacency.containsKey(clazz)) {
                messager.printMessage(Diagnostic.Kind.ERROR,"Emitter $nodeClazz is annotated as synchronizing after $clazz " +
                        "but the latter is not annotated with @${SyncEmitter::class.java.simpleName}")
            }
        }

        node.triggers.forEach { clazz ->
            if (!adjacency.containsKey(clazz)) {
                messager.printMessage(Diagnostic.Kind.ERROR,"Emitter $nodeClazz is annotated as triggering $clazz " +
                        "but the latter is not annotated with @${SyncEmitter::class.java.simpleName}")
            }
        }
    }
}

private const val CYCLE_MESSAGE_PATTERN = "SyncEmitters form a cycle through the %s dependencies: %s"

private fun assertNoCyclesInAdjacency(adjacency: Map<String, Node>, messager: Messager) =
        with(adjacency) {
            forEach { (clazz, node) ->
                assertNoCyclesIn(node, { syncsAfter },
                        {
                            messager.printMessage(Diagnostic.Kind.ERROR,
                                    String.format(CYCLE_MESSAGE_PATTERN, "syncsAfter or triggeredBy", it))
                            //triggeredBy relationship induces that an emitter is syncing after the emitter-trigger, thus
                            //both should be mentioned as potential cause of the "syncsAfter" cycle
                        },
                        listOf(clazz))
                assertNoCyclesIn(node, { triggers },
                        { messager.printMessage(Diagnostic.Kind.ERROR, String.format(CYCLE_MESSAGE_PATTERN, "triggeredBy", it)) },
                        listOf(clazz))
            }
        }

private fun Map<String, Node>.assertNoCyclesIn(node: Node, traverse: Node.() -> Collection<String>,
                                               logCycle: (List<String>) -> Unit, metNodes: List<String>) {
    traverse(node).forEach { clazz ->
        if (metNodes.contains(clazz)) {
            logCycle(metNodes + clazz)
            throw IllegalStateException("Found cycle while processing @${SyncEmitter::class.java.simpleName}s. " +
                    "Please see Messages window for more details.")
        }

        assertNoCyclesIn(get(clazz)!!, traverse, logCycle, metNodes + clazz)
    }
}