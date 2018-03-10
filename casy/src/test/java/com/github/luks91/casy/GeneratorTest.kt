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

import com.squareup.kotlinpoet.asTypeName
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import javax.lang.model.element.PackageElement
import javax.lang.model.util.Elements

class GeneratorTest {

    @Before
    fun setUp() {
        val elementsMock = mock(Elements::class.java)
        val packageElementMock = mock(PackageElement::class.java)
        `when`(elementsMock.getPackageOf(Mockito.any())).thenReturn(packageElementMock)
        `when`(packageElementMock.toString()).thenReturn("com.testpackage")
    }

    @Test
    fun classNameShoutMatchRoot() {
        val topicToEmitter = mapOf<String, Collection<String>>()
        val priorities = mapOf("List" to 1L, "java.lang.String" to 2L)
        val envData = EnvironmentData(
                "com.root.hehe", "Emitters",
                List::class.asTypeName(), topicToEmitter, priorities, mapOf())

        val fileSpec = generateEmittersClass(envData)
        val textBuilder = StringBuilder()
        fileSpec.writeTo(textBuilder)
        assertEquals(
            "package com.root.hehe\n" +
                    "\n" +
                    "import com.github.luks91.casy.annotations.Prioritized\n" +
                    "import java.lang.String\n" +
                    "import java.util.Collections\n" +
                    "import kotlin.collections.Collection\n" +
                    "import kotlin.collections.List\n" +
                    "import kotlin.collections.Map\n" +
                    "import kotlin.collections.Set\n" +
                    "\n" +
                    "class Emitters internal constructor(emitterList: List, emitterString: String) {\n" +
                    "   private val prioritizedList: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterList, 1)\n" +
                    "\n" +
                    "   private val prioritizedString: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterString, 2)\n" +
                    "\n" +
                    "   private val topicsToEmitters: Map<kotlin.String, Set<Prioritized<List>>>\n" +
                    "\n" +
                    "   init {\n" +
                    "      val tempMap = mutableMapOf<kotlin.String, Set<Prioritized<List>>>()\n" +
                    "      topicsToEmitters = tempMap\n" +
                    "   }\n" +
                    "\n" +
                    "   fun all(): Collection<Prioritized<List>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())\n" +
                    "\n" +
                    "   fun allBy(topics: List<kotlin.String>): Collection<Prioritized<List>> {\n" +
                    "      if (topics.isEmpty()) {\n" +
                    "         return all()\n" +
                    "      } else {\n" +
                    "         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())\n" +
                    "      }\n" +
                    "   }\n" +
                    "}\n",
                textBuilder.toString())
    }

    @Test
    fun shouldRespectPriorities() {
        val topicToEmitter = mapOf(
                "topic1" to listOf("List", "Set", "Map")
        )
        val priorities = mapOf("List" to 1L, "java.lang.String" to 2L,
                "Map" to 1L, "Set" to 2L)
        val envData = EnvironmentData(
                "com.root.heher", "Emitters2",
                List::class.asTypeName(), topicToEmitter, priorities, mapOf())

        val fileSpec = generateEmittersClass(envData)
        val textBuilder = StringBuilder()
        fileSpec.writeTo(textBuilder)
        assertEquals(
            "package com.root.heher\n" +
                    "\n" +
                    "import com.github.luks91.casy.annotations.Prioritized\n" +
                    "import java.lang.String\n" +
                    "import java.util.Collections\n" +
                    "import kotlin.collections.Collection\n" +
                    "import kotlin.collections.List\n" +
                    "import kotlin.collections.Map\n" +
                    "import kotlin.collections.Set\n" +
                    "\n" +
                    "class Emitters2 internal constructor(\n" +
                    "      emitterList: List,\n" +
                    "      emitterString: String,\n" +
                    "      emitterMap: Map,\n" +
                    "      emitterSet: Set\n" +
                    ") {\n" +
                    "   private val prioritizedList: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterList, 1)\n" +
                    "\n" +
                    "   private val prioritizedString: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterString, 2)\n" +
                    "\n" +
                    "   private val prioritizedMap: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterMap, 1)\n" +
                    "\n" +
                    "   private val prioritizedSet: Prioritized<List> = \n" +
                    "         Prioritized<List>(emitterSet, 2)\n" +
                    "\n" +
                    "   private val topicsToEmitters: Map<kotlin.String, Set<Prioritized<List>>>\n" +
                    "\n" +
                    "   init {\n" +
                    "      val tempMap = mutableMapOf<kotlin.String, Set<Prioritized<List>>>()\n" +
                    "      tempMap.put(\"topic1\", setOf(prioritizedList,\n" +
                    "            prioritizedSet,\n" +
                    "            prioritizedMap))\n" +
                    "      topicsToEmitters = tempMap\n" +
                    "   }\n" +
                    "\n" +
                    "   fun all(): Collection<Prioritized<List>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())\n" +
                    "\n" +
                    "   fun allBy(topics: List<kotlin.String>): Collection<Prioritized<List>> {\n" +
                    "      if (topics.isEmpty()) {\n" +
                    "         return all()\n" +
                    "      } else {\n" +
                    "         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())\n" +
                    "      }\n" +
                    "   }\n" +
                    "}\n",
                textBuilder.toString())
    }

    @Test
    fun shouldCorrectlyProvideEmittersForTopic() {
        val nonTopicEmitters = listOf("List", "java.lang.String")
        val topicToEmitter = mapOf(
                "topic1" to listOf("List", "Set", "Map"),
                "topic3" to listOf("List"),
                "topic6" to listOf("Set", "Map")
        )
        val priorities = mapOf("List" to 1L, "java.lang.String" to 2L,
                "Map" to 1L, "Set" to 2L)
        val envData = EnvironmentData(
                "com.root.heher", "Emitters2",
                List::class.asTypeName(), topicToEmitter, priorities, mapOf())

        val fileSpec = generateEmittersClass(envData)
        val textBuilder = StringBuilder()
        fileSpec.writeTo(textBuilder)
        assertEquals(
                "package com.root.heher\n" +
                        "\n" +
                        "import com.github.luks91.casy.annotations.Prioritized\n" +
                        "import java.lang.String\n" +
                        "import java.util.Collections\n" +
                        "import kotlin.collections.Collection\n" +
                        "import kotlin.collections.List\n" +
                        "import kotlin.collections.Map\n" +
                        "import kotlin.collections.Set\n" +
                        "\n" +
                        "class Emitters2 internal constructor(\n" +
                        "      emitterList: List,\n" +
                        "      emitterString: String,\n" +
                        "      emitterMap: Map,\n" +
                        "      emitterSet: Set\n" +
                        ") {\n" +
                        "   private val prioritizedList: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterList, 1)\n" +
                        "\n" +
                        "   private val prioritizedString: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterString, 2)\n" +
                        "\n" +
                        "   private val prioritizedMap: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterMap, 1)\n" +
                        "\n" +
                        "   private val prioritizedSet: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterSet, 2)\n" +
                        "\n" +
                        "   private val topicsToEmitters: Map<kotlin.String, Set<Prioritized<List>>>\n" +
                        "\n" +
                        "   init {\n" +
                        "      val tempMap = mutableMapOf<kotlin.String, Set<Prioritized<List>>>()\n" +
                        "      tempMap.put(\"topic1\", setOf(prioritizedList,\n" +
                        "            prioritizedSet,\n" +
                        "            prioritizedMap))\n" +
                        "      tempMap.put(\"topic3\", setOf(prioritizedList))\n" +
                        "      tempMap.put(\"topic6\", setOf(prioritizedSet,\n" +
                        "            prioritizedMap))\n" +
                        "      topicsToEmitters = tempMap\n" +
                        "   }\n" +
                        "\n" +
                        "   fun all(): Collection<Prioritized<List>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())\n" +
                        "\n" +
                        "   fun allBy(topics: List<kotlin.String>): Collection<Prioritized<List>> {\n" +
                        "      if (topics.isEmpty()) {\n" +
                        "         return all()\n" +
                        "      } else {\n" +
                        "         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())\n" +
                        "      }\n" +
                        "   }\n" +
                        "}\n",
                textBuilder.toString())
    }

    @Test
    fun shouldCorrectlyCreateGrouppingMethods() {
        val nonTopicEmitters = listOf("List", "java.lang.String")
        val topicToEmitter = mapOf(
                "topic1" to listOf("List", "Set", "Map"),
                "topic3" to listOf("List"),
                "topic6" to listOf("Set", "Map")
        )
        val priorities = mapOf("List" to 1L, "java.lang.String" to 2L,
                "Map" to 1L, "Set" to 2L)
        val groups = mapOf(
                "Alpha" to listOf("List", "Set", "Map"),
                "Beta" to listOf("java.lang.String", "Set")
        )
        val envData = EnvironmentData(
                "com.root.heher", "Emitters2",
                List::class.asTypeName(), topicToEmitter, priorities, groups)

        val fileSpec = generateEmittersClass(envData)
        val textBuilder = StringBuilder()
        fileSpec.writeTo(textBuilder)
        assertEquals(
                "package com.root.heher\n" +
                        "\n" +
                        "import com.github.luks91.casy.annotations.Prioritized\n" +
                        "import java.lang.String\n" +
                        "import java.util.Collections\n" +
                        "import kotlin.collections.Collection\n" +
                        "import kotlin.collections.List\n" +
                        "import kotlin.collections.Map\n" +
                        "import kotlin.collections.Set\n" +
                        "\n" +
                        "class Emitters2 internal constructor(\n" +
                        "      emitterList: List,\n" +
                        "      emitterString: String,\n" +
                        "      emitterMap: Map,\n" +
                        "      emitterSet: Set\n" +
                        ") {\n" +
                        "   private val prioritizedList: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterList, 1)\n" +
                        "\n" +
                        "   private val prioritizedString: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterString, 2)\n" +
                        "\n" +
                        "   private val prioritizedMap: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterMap, 1)\n" +
                        "\n" +
                        "   private val prioritizedSet: Prioritized<List> = \n" +
                        "         Prioritized<List>(emitterSet, 2)\n" +
                        "\n" +
                        "   private val topicsToEmitters: Map<kotlin.String, Set<Prioritized<List>>>\n" +
                        "\n" +
                        "   init {\n" +
                        "      val tempMap = mutableMapOf<kotlin.String, Set<Prioritized<List>>>()\n" +
                        "      tempMap.put(\"topic1\", setOf(prioritizedList,\n" +
                        "            prioritizedSet,\n" +
                        "            prioritizedMap))\n" +
                        "      tempMap.put(\"topic3\", setOf(prioritizedList))\n" +
                        "      tempMap.put(\"topic6\", setOf(prioritizedSet,\n" +
                        "            prioritizedMap))\n" +
                        "      topicsToEmitters = tempMap\n" +
                        "   }\n" +
                        "\n" +
                        "   fun all(): Collection<Prioritized<List>> = Collections.unmodifiableList(topicsToEmitters.values.flatMap { it }.distinct())\n" +
                        "\n" +
                        "   fun allBy(topics: List<kotlin.String>): Collection<Prioritized<List>> {\n" +
                        "      if (topics.isEmpty()) {\n" +
                        "         return all()\n" +
                        "      } else {\n" +
                        "         return Collections.unmodifiableList(topics.flatMap { topicsToEmitters[it] ?: setOf() }.distinct())\n" +
                        "      }\n" +
                        "   }\n" +
                        "\n" +
                        "   fun allAlpha(): Collection<Prioritized<List>> = Collections.unmodifiableSet(\n" +
                        "       setOf(\n" +
                        "           prioritizedList,\n" +
                        "           prioritizedSet,\n" +
                        "           prioritizedMap)\n" +
                        "   )\n" +
                        "\n" +
                        "   fun allBeta(): Collection<Prioritized<List>> = Collections.unmodifiableSet(\n" +
                        "       setOf(\n" +
                        "           prioritizedString,\n" +
                        "           prioritizedSet)\n" +
                        "   )\n" +
                        "}\n",
                textBuilder.toString())
    }

}