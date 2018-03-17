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
import com.google.common.annotations.VisibleForTesting
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

import com.github.luks91.casy.calculateAdjacency as calculateExternalAdjacency
import com.github.luks91.casy.calculateTriggerPaths as calculateExternalTriggerPaths
import com.github.luks91.casy.calculateNodesPriorities as calculateExternalNodesPriorities

@AutoService(Processor::class)
internal class Processor @JvmOverloads constructor(
        @VisibleForTesting val calculateAdjacency:
            (RoundEnvironment, Messager, ReflectionStrategy) -> Map<String, Node> =
                { env, messager, strategy -> calculateExternalAdjacency(env, messager, strategy) },
        @VisibleForTesting val calculateTriggerPaths: (Map<String, Node>) -> Map<String, List<String>> =
            { calculateExternalTriggerPaths(it) },
        @VisibleForTesting val calculateNodesPriorities: (Map<String, Node>) -> Map<String, Long> =
            { calculateExternalNodesPriorities(it) },
        @VisibleForTesting val processEnvironmentData: (EnvironmentData, String) -> Unit =
            { data, path -> generateEmittersClass(data).writeTo(File(path)) },
        @VisibleForTesting val reflectionStrategy: ReflectionStrategy = ReflectionStrategy.default()
) : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val adjacency = calculateAdjacency(roundEnv, processingEnv.messager, reflectionStrategy)

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
        processEnvironmentData(
                EnvironmentData(
                        rootPackageName = processingEnv.elementUtils.getPackageOf(element).toString(),
                        emittersName = with(element.simpleName) {
                            "$this${if (toString().endsWith('s')) "es" else "s"}"
                        },
                        rootTypeName = reflectionStrategy.typeNameOf(element),
                        topicsToEmitters = buildTopicsToEmittersMap(adjacency,
                                calculateTriggerPaths(adjacency), annotation),
                        nodePriorities = calculateNodesPriorities(adjacency),
                        groups = buildGroupsToEmittersMap(roundEnv, adjacency)
                ), processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]!!
        )
        return true
    }

    private fun buildTopicsToEmittersMap(adjacency: Map<String, Node>,
                                         paths: Map<String, List<String>>, root: SyncRoot) =
            adjacency.flatMap { (name, node) ->
                node.topics.flatMap { topic ->
                    listOf(topic to name)
                            .plus((paths[name] ?: listOf()).map { topic to it })
                }
            }
            .distinct()
            .groupBy({ it.first }, { it.second })
            .let { map -> allPair(root, adjacency)?.let { map.plus(it) } ?: map }
            .let { map -> allNonPushPair(root, adjacency, paths)?.let { map.plus(it) } ?: map }

    private fun allPair(root: SyncRoot, adjacency: Map<String, Node>) =
            root.allEmittersTopic.takeIf { !it.isBlank() }?.let { it to adjacency.keys.toList() }

    private fun allNonPushPair(root: SyncRoot, adjacency: Map<String, Node>,
                               paths: Map<String, List<String>>): Pair<String, List<String>>? =
        root.allNonPushEmittersTopic
                .takeIf { !it.isBlank() }
                ?.let { topic ->
                    val emitters = adjacency.filter { it.value.topics.isEmpty() }.keys.toList()
                    return@let topic to emitters.flatMap { (paths[it] ?: listOf()) + it }.distinct()
                }

    private fun buildGroupsToEmittersMap(roundEnv: RoundEnvironment, adjacency: Map<String, Node>) =
            mutableMapOf<String, List<String>>().apply {
                roundEnv.getElementsAnnotatedWith(SyncGroup::class.java).forEach { group ->
                    val emitters = roundEnv.getElementsAnnotatedWith(group as TypeElement)
                            .map {
                                reflectionStrategy.stringTypeNameOf(it).apply {
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
}