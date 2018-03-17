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
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.tools.Diagnostic

internal fun calculateAdjacency(roundEnv: RoundEnvironment, messager: Messager,
                                strategy: ReflectionStrategy) =
        buildAdjacencyMap(roundEnv, strategy).apply {
            assertNoEmittersAreMissing(this, messager)
            assertNoCyclesInAdjacency(this, messager)
        }

private fun buildAdjacencyMap(roundEnv: RoundEnvironment, strategy: ReflectionStrategy):
        Map<String, Node> {
    val triggers = mutableMapOf<String, MutableSet<String>>().apply {
        roundEnv.getElementsAnnotatedWith(SyncEmitter::class.java).forEach {
            val nodeClass = strategy.stringTypeNameOf(it)
            val annotation = it.getAnnotation(SyncEmitter::class.java)
            val triggeredBy = strategy.triggeredByFrom(annotation)

            triggeredBy.forEach { trigger -> getOrPut(trigger, { mutableSetOf() }).add(nodeClass) }
        }
    }

    return mutableMapOf<String, Node>().apply {
        roundEnv.getElementsAnnotatedWith(SyncEmitter::class.java).forEach {
            val nodeClass = strategy.stringTypeNameOf(it)
            val annotation = it.getAnnotation(SyncEmitter::class.java)
            put(nodeClass, Node(
                    nodeClass, annotation.topics.toList(),
                    mutableSetOf(*strategy.syncsAfterFrom(annotation)
                            .plus(strategy.triggeredByFrom(annotation))
                    ),
                    triggers[nodeClass] ?: emptySet())
            )
        }
    }
}
private fun assertNoEmittersAreMissing(adjacency: Map<String, Node>, messager: Messager) {
    adjacency.forEach { (nodeClazz, node) ->
        node.syncsAfter.forEach { clazz ->
            if (!adjacency.containsKey(clazz)) {
                val message = "Emitter $nodeClazz is annotated as synchronizing after $clazz " +
                        "but the latter is not annotated with @${SyncEmitter::class.java.simpleName}"
                messager.printMessage(Diagnostic.Kind.ERROR, message)
                throw IllegalStateException(message)
            }
        }
    }
}

private const val CYCLE_MESSAGE_PATTERN = "SyncEmitters form a cycle through the %s dependencies: %s"

private fun assertNoCyclesInAdjacency(adjacency: Map<String, Node>, messager: Messager) =
        with(adjacency) {
            forEach { (clazz, node) ->
                assertNoCyclesIn(node, { triggers }, {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format(CYCLE_MESSAGE_PATTERN, "triggeredBy", it))
                }, listOf(clazz))
                assertNoCyclesIn(node, { syncsAfter }, {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format(CYCLE_MESSAGE_PATTERN, "syncsAfter or triggeredBy", it))
                    //triggeredBy relationship induces that an emitter is syncing after the emitter-trigger, thus
                    //both should be mentioned as potential cause of the "syncsAfter" cycle
                }, listOf(clazz))
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