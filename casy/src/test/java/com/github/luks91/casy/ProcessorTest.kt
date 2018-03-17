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

import io.leangen.geantyref.TypeFactory
import com.github.luks91.casy.annotations.SyncEmitter
import com.github.luks91.casy.annotations.SyncGroup
import com.github.luks91.casy.annotations.SyncRoot
import com.google.common.base.Strings
import com.squareup.kotlinpoet.TypeName
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import kotlin.properties.Delegates

class ProcessorTest {

    private var typeElements by Delegates.notNull<MutableSet<TypeElement>>()
    private var roundEnv by Delegates.notNull<RoundEnvironment>()
    private var processingEnv by Delegates.notNull<ProcessingEnvironment>()
    private var messager by Delegates.notNull<Messager>()
    private var elements by Delegates.notNull<Elements>()
    private var testedProcesor by Delegates.notNull<Processor>()

    private var adjacency by Delegates.notNull<MutableMap<String, Node>>()
    private var triggerPaths by Delegates.notNull<MutableMap<String, List<String>>>()
    private var capturedData: EnvironmentData? = null
    private var savedPath: String = ""

    private val elementToTypeName = mutableMapOf<Element, TypeName>()
    private val elementToStringName = mutableMapOf<Element, String>()
    private val nodeToPriority = mutableMapOf<String, Long>()
    private val groups = mutableSetOf<Element>()

    private val reflectionStrategy = object : ReflectionStrategy {
        override fun typeNameOf(element: Element) = elementToTypeName[element]!!

        override fun stringTypeNameOf(element: Element) = elementToStringName[element]!!

        override fun syncsAfterFrom(emitter: SyncEmitter): Array<String> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun triggeredByFrom(emitter: SyncEmitter): Array<String> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    @Before
    fun setUp() {
        savedPath = ""
        elementToTypeName.clear()
        elementToStringName.clear()
        nodeToPriority.clear()
        typeElements = mutableSetOf()
        roundEnv = Mockito.mock(RoundEnvironment::class.java)
        processingEnv = Mockito.mock(ProcessingEnvironment::class.java)
        messager = Mockito.mock(Messager::class.java)
        elements = Mockito.mock(Elements::class.java)
        adjacency = mutableMapOf()
        groups.clear()

        triggerPaths = mutableMapOf()
        capturedData = null
        Mockito.`when`(processingEnv.messager).thenReturn(messager)
        Mockito.`when`(processingEnv.elementUtils).thenReturn(elements)
        testedProcesor = Processor(
                { roundEnvironment, funcMessager, strategy ->
                    assertEquals(roundEnv, roundEnvironment)
                    assertEquals(messager, funcMessager)
                    assertEquals(reflectionStrategy, strategy)
                    return@Processor adjacency
                },
                { triggerPaths },
                { nodeToPriority },
                { environmentData, path ->
                    capturedData = environmentData
                    savedPath = path
                }
                , reflectionStrategy)
        testedProcesor.init(processingEnv)

        val options = mapOf("kapt.kotlin.generated" to "file1")
        Mockito.`when`(processingEnv.options).thenReturn(options)
        Mockito.`when`(roundEnv.getElementsAnnotatedWith(SyncGroup::class.java)).thenReturn(groups)
    }

    @Test
    fun shouldSupportLatestSourceVersion() {
        assertEquals(SourceVersion.latest(), testedProcesor.supportedSourceVersion)
    }

    @Test
    fun shouldSupportAllAnnotationTypes() {
        val supportedAnnotations = testedProcesor.supportedAnnotationTypes
        assertEquals(3, supportedAnnotations.size)
        assertTrue(supportedAnnotations.contains(SyncEmitter::class.java.canonicalName))
        assertTrue(supportedAnnotations.contains(SyncRoot::class.java.canonicalName))
        assertTrue(supportedAnnotations.contains(SyncGroup::class.java.canonicalName))
    }

    @Test
    fun shouldFallThroughWhenNoEmitters() {
        testedProcesor.process(typeElements, roundEnv)
        Mockito.verify(roundEnv, Mockito.never()).getElementsAnnotatedWith(SyncRoot::class.java)
        Mockito.verify(messager, Mockito.never()).printMessage(Mockito.eq(Diagnostic.Kind.ERROR),
                Mockito.anyString())
        assertNull(capturedData)
    }

    @Test //TODO: this test to be removed when multiple roots are supported
    fun shouldFailWhenManyRoots() {
        adjacency.put("rains", Node("com.package", setOf(), setOf(), setOf()))
        val firstRoot = Mockito.mock(Element::class.java).apply {
            val typeMirror = Mockito.mock(TypeMirror::class.java)
            val annotation = TypeFactory.annotation(SyncRoot::class.java, mapOf())
            Mockito.`when`(getAnnotation(SyncRoot::class.java)).thenReturn(annotation)
            Mockito.`when`(asType()).thenReturn(typeMirror)
        }

        val secondRoot = Mockito.mock(Element::class.java).apply {
            val typeMirror = Mockito.mock(TypeMirror::class.java)
            val annotation = TypeFactory.annotation(SyncRoot::class.java, mapOf())
            Mockito.`when`(getAnnotation(SyncRoot::class.java)).thenReturn(annotation)
            Mockito.`when`(asType()).thenReturn(typeMirror)
        }

        Mockito.`when`(roundEnv.getElementsAnnotatedWith(SyncRoot::class.java))
                .thenReturn(setOf(firstRoot, secondRoot))

        testedProcesor.process(typeElements, roundEnv)
        Mockito.verify(roundEnv, Mockito.times(1)).getElementsAnnotatedWith(SyncRoot::class.java)
        Mockito.verify(messager, Mockito.times(1)).printMessage(
                Mockito.eq(Diagnostic.Kind.ERROR), Mockito.anyString())
        assertNull(capturedData)
    }

    @Test
    fun shouldFailWhenEmittersAndNoRoot() {
        adjacency.put("rains", Node("com.package", setOf(), setOf(), setOf()))
        testedProcesor.process(typeElements, roundEnv)
        Mockito.verify(roundEnv, Mockito.times(1)).getElementsAnnotatedWith(SyncRoot::class.java)
        Mockito.verify(messager, Mockito.times(1)).printMessage(
                Mockito.eq(Diagnostic.Kind.ERROR), Mockito.anyString())
        assertNull(capturedData)
    }

    @Test
    fun shouldProcessRootData() {
        adjacency.put("rains", Node("com.package", setOf(), setOf(), setOf()))
        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoot", "com.root.package", typeName)

        testedProcesor.process(typeElements, roundEnv)

        assertEquals("com.root.package", capturedData!!.rootPackageName)
        assertEquals("SyncRoots", capturedData!!.emittersName)
        assertEquals(typeName, capturedData!!.rootTypeName)
    }

    private fun givenRootObject(clazzName: String, packageName: String, typeName: TypeName,
                                allEmittersTopic: String = "", allNonPushTopic: String = "") {
        val firstRoot = Mockito.mock(Element::class.java).apply {
            val name = Mockito.mock(Name::class.java)
            Mockito.`when`(name.toString()).thenReturn(clazzName)
            Mockito.`when`(simpleName).thenReturn(name)

            val annotationParameters = mutableMapOf<String, String>()
            allEmittersTopic.takeIf { !Strings.isNullOrEmpty(it) }
                    ?.apply { annotationParameters.put("allEmittersTopic", this) }
            allNonPushTopic.takeIf { !Strings.isNullOrEmpty(it) }
                    ?.apply { annotationParameters.put("allNonPushEmittersTopic", this) }

            val annotation = TypeFactory.annotation(SyncRoot::class.java, annotationParameters.toMap())
            Mockito.`when`(getAnnotation(SyncRoot::class.java)).thenReturn(annotation)
            elementToTypeName.put(this, typeName)
        }

        val packageElement = Mockito.mock(PackageElement::class.java)
        Mockito.`when`(packageElement.toString()).thenReturn(packageName)
        Mockito.`when`(roundEnv.getElementsAnnotatedWith(SyncRoot::class.java))
                .thenReturn(setOf(firstRoot))
        Mockito.`when`(elements.getPackageOf(firstRoot)).thenReturn(packageElement)
    }

    @Test
    fun shouldCreateCorrectEsName() {
        adjacency.put("rains", Node("com.package", setOf(), setOf(), setOf()))
        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)

        testedProcesor.process(typeElements, roundEnv)

        assertEquals("SyncRootses", capturedData!!.emittersName)
    }

    @Test
    fun shouldBuildBaseTopicsCorrectly() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf("condition"), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))
        adjacency.put("blizzard", Node("Blizzards", setOf("condition", "danger"), setOf(), setOf()))
        adjacency.put("none", Node("Nones", setOf(), setOf(), setOf()))
        adjacency.put("cloud", Node("Clouds", setOf("flying"), setOf(), setOf()))

        triggerPaths.put("cloud", listOf("none", "winds"))
        triggerPaths.put("snows", listOf("winds"))
        triggerPaths.put("none", listOf("blizzard"))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)

        testedProcesor.process(typeElements, roundEnv)

        val topicsToEmitters = capturedData!!.topicsToEmitters
        assertEquals(3, topicsToEmitters.size)

        assertEquals(listOf("rains", "snows", "winds", "blizzard"), topicsToEmitters["condition"])
        assertEquals(listOf("blizzard"), topicsToEmitters["danger"])
        assertEquals(listOf("cloud", "none", "winds"), topicsToEmitters["flying"])
    }

    @Test
    fun shouldPassCorrectPriorities() {
        adjacency.put("rains", Node("com.package", setOf(), setOf(), setOf()))
        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)
        nodeToPriority.put("node1", 5L)
        nodeToPriority.put("node2", 10L)
        nodeToPriority.put("node3", 15L)

        testedProcesor.process(typeElements, roundEnv)

        assertEquals(nodeToPriority, capturedData!!.nodePriorities)
    }

    @Test
    fun shouldGenerateAllTopicWhenProvided() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf("condition"), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject(
                clazzName = "SyncRoots",
                packageName = "com.root.package",
                typeName = typeName,
                allEmittersTopic = "allMyFavoriteEmitters")

        testedProcesor.process(typeElements, roundEnv)

        val topicsToEmitters = capturedData!!.topicsToEmitters
        assertEquals(2, topicsToEmitters.size)

        assertEquals(listOf("rains", "snows", "winds"), topicsToEmitters["allMyFavoriteEmitters"])
    }

    @Test
    fun shouldGenerateAllNonTopicWhenProvidedWithPaths() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf(), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))
        adjacency.put("blizzard", Node("Blizzards", setOf("condition", "danger"), setOf(), setOf()))
        adjacency.put("none", Node("Nones", setOf(), setOf(), setOf()))
        adjacency.put("cloud", Node("Clouds", setOf(), setOf(), setOf()))

        triggerPaths.put("cloud", listOf("none", "winds"))
        triggerPaths.put("snows", listOf("winds"))
        triggerPaths.put("none", listOf("blizzard"))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject(
                clazzName = "SyncRoots",
                packageName = "com.root.package",
                typeName = typeName,
                allNonPushTopic = "allMyFavoriteNonPushEmitters")

        testedProcesor.process(typeElements, roundEnv)

        val topicsToEmitters = capturedData!!.topicsToEmitters
        assertEquals(3, topicsToEmitters.size)
        assertEquals(listOf("winds", "snows", "blizzard", "none", "cloud"),
                topicsToEmitters["allMyFavoriteNonPushEmitters"])
    }

    @Test
    fun shouldGenerateSingleGroupCorrectly() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf(), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))
        adjacency.put("blizzard", Node("Blizzards", setOf("condition", "danger"), setOf(), setOf()))
        adjacency.put("none", Node("Nones", setOf(), setOf(), setOf()))
        adjacency.put("cloud", Node("Clouds", setOf(), setOf(), setOf()))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)
        givenGroup("BestOfBest", "rains", "snows")
        testedProcesor.process(typeElements, roundEnv)

        val groups = capturedData!!.groups
        assertEquals(1, groups.size)
        assertEquals(listOf("rains", "snows"), groups["BestOfBest"])
    }

    private fun givenGroup(groupName: String, vararg groupNodes: String) {
        val group = Mockito.mock(TypeElement::class.java).apply {
            val name = Mockito.mock(Name::class.java)
            Mockito.`when`(name.toString()).thenReturn(groupName)
            Mockito.`when`(simpleName).thenReturn(name)

            val annotation = TypeFactory.annotation(SyncRoot::class.java, mapOf())
            Mockito.`when`(getAnnotation(SyncRoot::class.java)).thenReturn(annotation)
        }

        val groupEmitters = groupNodes.map { emitterName ->
            Mockito.mock(Element::class.java).apply {
                val name = Mockito.mock(Name::class.java)
                Mockito.`when`(name.toString()).thenReturn(emitterName)
                Mockito.`when`(simpleName).thenReturn(name)
                elementToStringName.put(this, emitterName)
            }
        }.toSet()

        groups.add(group)
        Mockito.`when`(roundEnv.getElementsAnnotatedWith(group)).thenReturn(groupEmitters)
    }

    @Test
    fun shouldGenerateMultipleGroupsCorrectly() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf(), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))
        adjacency.put("blizzard", Node("Blizzards", setOf("condition", "danger"), setOf(), setOf()))
        adjacency.put("none", Node("Nones", setOf(), setOf(), setOf()))
        adjacency.put("cloud", Node("Clouds", setOf(), setOf(), setOf()))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)
        givenGroup("BestOfBest", "rains", "snows")
        givenGroup("StillGood", "snows", "none", "cloud")
        givenGroup("Others", "blizzard")
        testedProcesor.process(typeElements, roundEnv)

        val groups = capturedData!!.groups
        assertEquals(3, groups.size)
        assertEquals(listOf("rains", "snows"), groups["BestOfBest"])
        assertEquals(listOf("snows", "none", "cloud"), groups["StillGood"])
        assertEquals(listOf("blizzard"), groups["Others"])
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrowWhenGroupMemberIsNotEmitter() {
        adjacency.put("rains", Node("Rains", setOf("condition"), setOf(), setOf()))
        adjacency.put("snows", Node("Snows", setOf(), setOf(), setOf()))
        adjacency.put("winds", Node("Winds", setOf("condition"), setOf(), setOf()))

        val typeName = Mockito.mock(TypeName::class.java)
        givenRootObject("SyncRoots", "com.root.package", typeName)
        givenGroup("BestOfBest", "somethingnew")

        testedProcesor.process(typeElements, roundEnv)
    }
}
