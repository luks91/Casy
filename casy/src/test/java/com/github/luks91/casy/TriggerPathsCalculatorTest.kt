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

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerPathsCalculatorTest {

    @Test
    fun shouldCalculateCorrectlyForTwoConnectedNodes() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf()),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node1"))
        ))

        assertEquals(listOf<String>(), paths["node1"])
        assertEquals(listOf("node1"), paths["node2"])
    }

    @Test
    fun shouldCalculateCorrectlyForTwoDistinctNodes() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf()),
                "node2" to Node("node2", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf<String>(), paths["node1"])
        assertEquals(listOf<String>(), paths["node2"])
    }

    @Test
    fun shouldCalculateCorrectlyForStraightTree() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node3")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node4", "node5")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node6")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf("node7")),
                "node5" to Node("node5", arrayOf(), setOf(), setOf()),
                "node6" to Node("node6", arrayOf(), setOf(), setOf()),
                "node7" to Node("node7", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node7", "node4", "node5", "node2", "node6", "node3"), paths["node1"])
        assertEquals(listOf("node7", "node4", "node5"), paths["node2"])
        assertEquals(listOf("node6"), paths["node3"])
        assertEquals(listOf("node7"), paths["node4"])
        assertEquals(listOf<String>(), paths["node5"])
        assertEquals(listOf<String>(), paths["node6"])
        assertEquals(listOf<String>(), paths["node7"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree1() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node3")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node4", "node5")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node4")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf("node5")),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node5", "node4", "node2", "node3"), paths["node1"])
        assertEquals(listOf("node5", "node4"), paths["node2"])
        assertEquals(listOf("node5", "node4"), paths["node3"])
        assertEquals(listOf("node5"), paths["node4"])
        assertEquals(listOf<String>(), paths["node5"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree2() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node4")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node3")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node4")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node4", "node3", "node2"), paths["node1"])
        assertEquals(listOf("node4", "node3"), paths["node2"])
        assertEquals(listOf("node4"), paths["node3"])
        assertEquals(listOf<String>(), paths["node4"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree3() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node3")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node4", "node5")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf()),
                "node4" to Node("node4", arrayOf(), setOf(), setOf("node5")),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node5", "node4", "node2", "node3"), paths["node1"])
        assertEquals(listOf("node5", "node4"), paths["node2"])
        assertEquals(listOf<String>(), paths["node3"])
        assertEquals(listOf("node5"), paths["node4"])
        assertEquals(listOf<String>(), paths["node5"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree4() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node3")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node4")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node4")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node4", "node2", "node3"), paths["node1"])
        assertEquals(listOf("node4"), paths["node2"])
        assertEquals(listOf("node4"), paths["node3"])
        assertEquals(listOf<String>(), paths["node4"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree5() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node2", "node3", "node4")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node5", "node6")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node5", "node7")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf("node3", "node6")),
                "node5" to Node("node5", arrayOf(), setOf(), setOf("node7")),
                "node6" to Node("node6", arrayOf(), setOf(), setOf("node7")),
                "node7" to Node("node7", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node7", "node5", "node6", "node2", "node3", "node4"), paths["node1"])
        assertEquals(listOf("node7", "node5", "node6"), paths["node2"])
        assertEquals(listOf("node7", "node5"), paths["node3"])
        assertEquals(listOf("node7", "node5", "node3", "node6"), paths["node4"])
        assertEquals(listOf("node7"), paths["node5"])
        assertEquals(listOf("node7"), paths["node6"])
        assertEquals(listOf<String>(), paths["node7"])
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree6() {
        val paths = calculateTriggerPaths(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf("node3", "node4")),
                "node2" to Node("node2", arrayOf(), setOf(), setOf("node4")),
                "node3" to Node("node3", arrayOf(), setOf(), setOf("node5")),
                "node4" to Node("node4", arrayOf(), setOf(), setOf("node5")),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(listOf("node5", "node3", "node4"), paths["node1"])
        assertEquals(listOf("node5", "node4"), paths["node2"])
        assertEquals(listOf("node5"), paths["node3"])
        assertEquals(listOf("node5"), paths["node4"])
        assertEquals(listOf<String>(), paths["node5"])
    }
}