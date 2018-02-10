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
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal interface ReflectionStrategy {

    companion object {
        fun default() = AnnotationStrategy()
    }

    fun typeNameOf(element: Element): String
    fun syncsAfterFrom(emitter: SyncEmitter): Array<String>
    fun triggeredByFrom(emitter: SyncEmitter): Array<String>
}

internal class AnnotationStrategy : ReflectionStrategy {
    override fun typeNameOf(element: Element) = element.asType().asTypeName().toString()

    override fun syncsAfterFrom(emitter: SyncEmitter) = emitter.classesOf { syncsAfter }
    override fun triggeredByFrom(emitter: SyncEmitter) = emitter.classesOf { triggeredBy }

    private inline fun SyncEmitter.classesOf(
            extractValueFor: SyncEmitter.() -> Array<KClass<*>>): Array<String> = try {
        extractValueFor(this).map { it.jvmName }.toTypedArray()
    } catch (mte: MirroredTypesException) {
        (mte.typeMirrors as List<TypeMirror>).map { it.toString() }.toTypedArray()
    }
}