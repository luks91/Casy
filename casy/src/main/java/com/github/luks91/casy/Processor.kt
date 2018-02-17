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
import com.github.luks91.casy.annotations.SyncGroup
import com.github.luks91.casy.annotations.SyncRoot
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.properties.Delegates

@AutoService(Processor::class)
class Processor : AbstractProcessor() {

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messenger = processingEnv.messager

        val adjacency = calculateAdjacency(roundEnv, messenger, ReflectionStrategy.default())

        if (adjacency.isEmpty()) {
            return true
        }

        val rootElements = roundEnv.getElementsAnnotatedWith(SyncRoot::class.java)
        if (rootElements.size != 1) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                    "One common interface for @${SyncEmitter::class.java.simpleName} " +
                            "classes must be annotated as a ${SyncRoot::class.java.simpleName}, " +
                            "currently are: $rootElements")
            return true
        }

        val element = rootElements.first()
        val annotation = element.getAnnotation(SyncRoot::class.java)
        generateEmittersClass(
                EnvironmentData(
                        processingEnv.elementUtils.getPackageOf(element).toString(),
                        with(element.simpleName) { "$this${if (endsWith('s')) "es" else "s"}"},
                        element.asType().asTypeName(),
                        adjacency.filterValues { it.topics.isEmpty() }.map { it.key },
                        buildTopicsToEmittersMap(adjacency, calculateTriggerPaths(adjacency), annotation),
                        calculateNodesPriorities(adjacency),
                        buildGroupsToEmittersMap(roundEnv, adjacency)
                )
        ).writeTo(File(processingEnv.options[Processor.KAPT_KOTLIN_GENERATED_OPTION_NAME]))
        return true
    }

    private fun buildTopicsToEmittersMap(adjacency: Map<String, Node>,
                                         paths: Map<String, List<String>>, root: SyncRoot) =
            adjacency.flatMap { (name, node) ->
                        node.topics.flatMap { topic ->
                            listOf(topic to name).plus((paths[name] ?: listOf()).map { topic to it })
                        }
                    }
                    .groupBy({ it.first }, { it.second })
                    .plus((root.allEmittersTopic.takeIf { !it.isBlank() } ?: "all") to adjacency.keys)
                    .plus((root.allNonPushEmittersTopic.takeIf { !it.isBlank() } ?: "allNonTopic")
                            to adjacency.filter { it.value.topics.isEmpty() }.keys
                    )

    private fun buildGroupsToEmittersMap(roundEnv: RoundEnvironment, adjacency: Map<String, Node>) =
            mutableMapOf<String, List<String>>().apply {
                roundEnv.getElementsAnnotatedWith(SyncGroup::class.java).forEach { group ->
                    val emitters = roundEnv.getElementsAnnotatedWith(group as TypeElement)
                            .map {
                                it.asType().asTypeName().toString().apply {
                                    if (!adjacency.containsKey(this)) {
                                        val error = "$this is annotated with ${group.simpleName} " +
                                                "but not ${SyncEmitter::class.simpleName}"
                                        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, error)
                                        throw IllegalStateException(error)
                                    }
                                }
                            }
                    put(group.simpleName.toString(), emitters)
                }
            }

    override fun getSupportedAnnotationTypes() = setOf(
            SyncEmitter::class.java.canonicalName,
            SyncRoot::class.java.canonicalName,
            SyncGroup::class.java.canonicalName
    )

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        var messenger: Messager by Delegates.notNull<Messager>()
    }
}