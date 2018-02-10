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
import io.leangen.geantyref.TypeFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import kotlin.properties.Delegates

class AdjacencyTest {

    private var mockEnvironment by Delegates.notNull<RoundEnvironment>()
    private var mockMessager by Delegates.notNull<Messager>()
    private var reflectionStrategy by Delegates.notNull<ReflectionStrategy>()

    private val elementToName = mutableMapOf<Element, String>()
    private val emitterToSyncsAfter = mutableMapOf<SyncEmitter, Array<String>>()
    private val emittersToTriggeredBy = mutableMapOf<SyncEmitter, Array<String>>()

    @Before
    fun setUp() {
        mockEnvironment = mock(RoundEnvironment::class.java)
        mockMessager = mock(Messager::class.java)
        elementToName.clear()
        emitterToSyncsAfter.clear()
        emittersToTriggeredBy.clear()
        reflectionStrategy = object: ReflectionStrategy {
            override fun typeNameOf(element: Element): String = elementToName[element]!!
            override fun syncsAfterFrom(emitter: SyncEmitter): Array<String> = emitterToSyncsAfter[emitter]!!
            override fun triggeredByFrom(emitter: SyncEmitter): Array<String> = emittersToTriggeredBy[emitter]!!
        }
    }

    private fun givenEmitters(vararg emitters: Emitter) {
        val elementsSet = mutableSetOf<Element>()
        for (emitter in emitters) {
            val element = emitter.element
            val annotation = emitter.annotation
            elementToName.put(element, emitter.name)
            emittersToTriggeredBy.put(annotation, arrayOf())
            emitterToSyncsAfter.put(annotation, arrayOf())
            elementsSet.add(element)
        }
        `when`(mockEnvironment.getElementsAnnotatedWith(SyncEmitter::class.java)).thenReturn(elementsSet)
    }

    private fun givenTriggeredByRelations(vararg pairs: Pair<Emitter, Array<Emitter>>) {
        for ((emitter, triggers) in pairs) {
            emittersToTriggeredBy.put(emitter.annotation, triggers.map { it.name }.toTypedArray())
        }
    }

    private fun givenSyncsAfterRelations(vararg pairs: Pair<Emitter, Array<Emitter>>) {
        for ((emitter, syncsAfter) in pairs) {
            emitterToSyncsAfter.put(emitter.annotation, syncsAfter.map { it.name }.toTypedArray())

        }
    }

    private data class Emitter(val name: String, val topics: Array<String>) {
        val annotation = with(mapOf("topics" to topics, "name" to name)) {
            TypeFactory.annotation(SyncEmitter::class.java, this)
        }

        val element = mock(Element::class.java).apply {
            val typeMirror = mock(TypeMirror::class.java)
            `when`(getAnnotation(SyncEmitter::class.java)).thenReturn(annotation)
            `when`(asType()).thenReturn(typeMirror)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Emitter

            if (name != other.name) return false
            if (!Arrays.equals(topics, other.topics)) return false
            if (annotation != other.annotation) return false
            if (element != other.element) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + Arrays.hashCode(topics)
            result = 31 * result + (annotation?.hashCode() ?: 0)
            result = 31 * result + (element?.hashCode() ?: 0)
            return result
        }
    }

    @Test
    fun shouldHandleOneEmitterOnlyCase() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))

        givenEmitters(rains)

        val calculatedMap = calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)

        assertArrayEquals(calculatedMap["rains"]!!.topics, arrayOf("fromCloud", "weather1"))
        assertEquals(calculatedMap["rains"]!!.syncsAfter, setOf<String>())
        assertEquals(calculatedMap["rains"]!!.triggers, setOf<String>())
    }

    @Test
    fun shouldMaintainEmittersTopics() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val snows = Emitter(name = "snows", topics = arrayOf("fromCloud", "weather2"))
        val thunders = Emitter(name = "thunders", topics = arrayOf("fromCloud", "weather3"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val floods = Emitter(name = "floods", topics = arrayOf("weather5"))
        val weathers = Emitter(name = "weathers", topics = arrayOf())

        givenEmitters(rains, snows, thunders, clouds, tornados, winds, floods, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds, weathers),
                snows to arrayOf(clouds),
                thunders to arrayOf(clouds),
                floods to arrayOf(rains),
                clouds to arrayOf(weathers, winds),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds)
        )

        val calculatedMap = calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)

        assertEquals(8, calculatedMap.size)

        assertArrayEquals(calculatedMap["rains"]!!.topics, arrayOf("fromCloud", "weather1"))
        assertArrayEquals(calculatedMap["snows"]!!.topics, arrayOf("fromCloud", "weather2"))
        assertArrayEquals(calculatedMap["thunders"]!!.topics, arrayOf("fromCloud", "weather3"))
        assertArrayEquals(calculatedMap["clouds"]!!.topics, arrayOf("weatherEvent"))
        assertArrayEquals(calculatedMap["tornados"]!!.topics, arrayOf("tornados, weather4"))
        assertArrayEquals(calculatedMap["winds"]!!.topics, arrayOf("winds"))
        assertArrayEquals(calculatedMap["floods"]!!.topics, arrayOf("weather5"))
        assertArrayEquals(calculatedMap["weathers"]!!.topics, arrayOf<String>())
    }

    @Test
    fun shouldBuildCorrectlyOneTreeWithTriggersOnly() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val snows = Emitter(name = "snows", topics = arrayOf("fromCloud", "weather2"))
        val thunders = Emitter(name = "thunders", topics = arrayOf("fromCloud", "weather3"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val floods = Emitter(name = "floods", topics = arrayOf("weather5"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, snows, thunders, clouds, tornados, winds, floods, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds, weathers),
                snows to arrayOf(clouds),
                thunders to arrayOf(clouds),
                floods to arrayOf(rains),
                clouds to arrayOf(weathers, winds),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds)
        )

        val calculatedMap = calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)

        assertEquals(8, calculatedMap.size)
        assertEquals(calculatedMap["rains"]!!.syncsAfter, setOf("clouds", "weathers"))
        assertEquals(calculatedMap["rains"]!!.triggers, setOf("floods"))
        assertEquals(calculatedMap["snows"]!!.syncsAfter, setOf("clouds"))
        assertEquals(calculatedMap["snows"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["thunders"]!!.syncsAfter, setOf("clouds"))
        assertEquals(calculatedMap["thunders"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["clouds"]!!.syncsAfter, setOf("weathers",  "winds"))
        assertEquals(calculatedMap["clouds"]!!.triggers, setOf("rains", "snows", "thunders"))
        assertEquals(calculatedMap["tornados"]!!.syncsAfter, setOf("weathers", "winds"))
        assertEquals(calculatedMap["tornados"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["winds"]!!.syncsAfter, setOf("weathers"))
        assertEquals(calculatedMap["winds"]!!.triggers, setOf("clouds", "tornados"))
        assertEquals(calculatedMap["floods"]!!.syncsAfter, setOf("rains"))
        assertEquals(calculatedMap["floods"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["weathers"]!!.syncsAfter, setOf<String>())
        assertEquals(calculatedMap["weathers"]!!.triggers, setOf("rains", "clouds", "tornados", "winds"))
    }

    @Test
    fun shouldBuildCorrectlyMultipleTreesWithTriggersOnly() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, clouds, tornados, winds, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds)
        )

        val calculatedMap = calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)

        assertEquals(5, calculatedMap.size)
        assertEquals(calculatedMap["rains"]!!.syncsAfter, setOf("clouds"))
        assertEquals(calculatedMap["rains"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["clouds"]!!.syncsAfter, setOf<String>())
        assertEquals(calculatedMap["clouds"]!!.triggers, setOf("rains"))
        assertEquals(calculatedMap["tornados"]!!.syncsAfter, setOf("weathers", "winds"))
        assertEquals(calculatedMap["tornados"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["winds"]!!.syncsAfter, setOf("weathers"))
        assertEquals(calculatedMap["winds"]!!.triggers, setOf("tornados"))
        assertEquals(calculatedMap["weathers"]!!.syncsAfter, setOf<String>())
        assertEquals(calculatedMap["weathers"]!!.triggers, setOf("tornados", "winds"))
    }

    @Test
    fun shouldBuildCorrectlyWithSyncsAfter() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, clouds, tornados, winds, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds)
        )

        givenSyncsAfterRelations(
                weathers to arrayOf(rains, clouds)
        )

        val calculatedMap = calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)

        assertEquals(5, calculatedMap.size)
        assertEquals(calculatedMap["rains"]!!.syncsAfter, setOf("clouds"))
        assertEquals(calculatedMap["rains"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["clouds"]!!.syncsAfter, setOf<String>())
        assertEquals(calculatedMap["clouds"]!!.triggers, setOf("rains"))
        assertEquals(calculatedMap["tornados"]!!.syncsAfter, setOf("weathers", "winds"))
        assertEquals(calculatedMap["tornados"]!!.triggers, setOf<String>())
        assertEquals(calculatedMap["winds"]!!.syncsAfter, setOf("weathers"))
        assertEquals(calculatedMap["winds"]!!.triggers, setOf("tornados"))
        assertEquals(calculatedMap["weathers"]!!.syncsAfter, setOf("rains", "clouds"))
        assertEquals(calculatedMap["weathers"]!!.triggers, setOf("tornados", "winds"))
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenSyncsAfterNotAnnotated() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(tornados, weathers)
        givenTriggeredByRelations(
                tornados to arrayOf(weathers)
        )

        givenSyncsAfterRelations(
                weathers to arrayOf(rains)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenTriggersNotAnnotated() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(tornados, weathers)
        givenTriggeredByRelations(
                tornados to arrayOf(weathers),
                weathers to arrayOf(rains)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenCycleInTriggers() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, tornados, weathers)
        givenTriggeredByRelations(
                tornados to arrayOf(weathers),
                weathers to arrayOf(rains),
                rains to arrayOf(tornados)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldDetectBigCycle() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val snows = Emitter(name = "snows", topics = arrayOf("fromCloud", "weather2"))
        val thunders = Emitter(name = "thunders", topics = arrayOf("fromCloud", "weather3"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val floods = Emitter(name = "floods", topics = arrayOf("weather5"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, snows, thunders, clouds, tornados, winds, floods, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds, weathers),
                snows to arrayOf(clouds),
                thunders to arrayOf(clouds),
                floods to arrayOf(rains),
                clouds to arrayOf(weathers, winds),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds),
                weathers to arrayOf(rains)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldDetectTwoElementCycle() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val snows = Emitter(name = "snows", topics = arrayOf("fromCloud", "weather2"))
        val thunders = Emitter(name = "thunders", topics = arrayOf("fromCloud", "weather3"))
        val clouds = Emitter(name = "clouds", topics = arrayOf("weatherEvent"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val winds = Emitter(name = "winds", topics = arrayOf("winds"))
        val floods = Emitter(name = "floods", topics = arrayOf("weather5"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, snows, thunders, clouds, tornados, winds, floods, weathers)
        givenTriggeredByRelations(
                rains to arrayOf(clouds, weathers),
                snows to arrayOf(clouds),
                thunders to arrayOf(clouds),
                floods to arrayOf(rains),
                clouds to arrayOf(weathers, winds, rains),
                winds to arrayOf(weathers),
                tornados to arrayOf(weathers, winds)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }
    @Test(expected = IllegalStateException::class)
    fun shouldDetectSelfCycle() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))

        givenEmitters(rains)
        givenTriggeredByRelations(rains to arrayOf(rains))

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenCycleInSyncsAfter() {
        val rains = Emitter(name = "rains", topics = arrayOf("fromCloud", "weather1"))
        val tornados = Emitter(name = "tornados", topics = arrayOf("tornados, weather4"))
        val weathers = Emitter(name = "weathers", topics = arrayOf("weather6"))

        givenEmitters(rains, tornados, weathers)
        givenSyncsAfterRelations(
                tornados to arrayOf(weathers),
                weathers to arrayOf(rains),
                rains to arrayOf(tornados)
        )

        calculateAdjacency(mockEnvironment, mockMessager, reflectionStrategy)
    }
}