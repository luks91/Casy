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

        val adjacency = calculateAdjacency(roundEnv, messenger)

        if (adjacency.isEmpty()) {
            return true
        }

        val rootElements = roundEnv.getElementsAnnotatedWith(SyncRoot::class.java)
        if (rootElements.size != 1) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                    "Common interface for @${SyncEmitter::class.java.simpleName} classes must be annotated as a " +
                            "${SyncRoot::class.java.simpleName}, currently are: $rootElements")
            return true
        }

        generateEmittersClass(roundEnv, processingEnv,
                EnvironmentData(
                        adjacency,
                        calculateTriggerPaths(adjacency),
                        calculateNodesPriorities(adjacency),
                        RootData.from(rootElements.first())
                )
        )
        return true
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