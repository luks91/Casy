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

class PrioritiesCalculatorTest {

    @Test
    fun shouldCalculateCorrectlyForTwoConnectedNodes() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node1"), setOf())
        ))

        assertEquals(1, priorities["node1"]!!)
        assertEquals(2, priorities["node2"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForTwoDistinctNodes() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf(), setOf()),
                "node2" to Node("node2", arrayOf(), setOf(), setOf())
        ))

        assertEquals(1, priorities["node1"]!!)
        assertEquals(1, priorities["node2"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForStraightTree() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node3"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node4", "node5"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node6"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf("node7"), setOf()),
                "node5" to Node("node5", arrayOf(), setOf(), setOf()),
                "node6" to Node("node6", arrayOf(), setOf(), setOf()),
                "node7" to Node("node7", arrayOf(), setOf(), setOf())
        ))

        assertEquals(4, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(2, priorities["node3"]!!)
        assertEquals(2, priorities["node4"]!!)
        assertEquals(1, priorities["node5"]!!)
        assertEquals(1, priorities["node6"]!!)
        assertEquals(1, priorities["node7"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree1() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node3"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node4", "node5"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node4"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf("node5"), setOf()),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(4, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(3, priorities["node3"]!!)
        assertEquals(2, priorities["node4"]!!)
        assertEquals(1, priorities["node5"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree2() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node4"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node3"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node4"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf(), setOf())
        ))

        assertEquals(4, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(2, priorities["node3"]!!)
        assertEquals(1, priorities["node4"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree3() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node3"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node4", "node5"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf(), setOf()),
                "node4" to Node("node4", arrayOf(), setOf("node5"), setOf()),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(4, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(1, priorities["node3"]!!)
        assertEquals(2, priorities["node4"]!!)
        assertEquals(1, priorities["node5"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree4() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node3"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node4"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node4"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf(), setOf())
        ))

        assertEquals(3, priorities["node1"]!!)
        assertEquals(2, priorities["node2"]!!)
        assertEquals(2, priorities["node3"]!!)
        assertEquals(1, priorities["node4"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree5() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node2", "node3", "node4"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node5", "node6"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node5", "node7"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf("node3", "node6"), setOf()),
                "node5" to Node("node5", arrayOf(), setOf("node7"), setOf()),
                "node6" to Node("node6", arrayOf(), setOf("node7"), setOf()),
                "node7" to Node("node7", arrayOf(), setOf(), setOf())
        ))

        assertEquals(5, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(3, priorities["node3"]!!)
        assertEquals(4, priorities["node4"]!!)
        assertEquals(2, priorities["node5"]!!)
        assertEquals(2, priorities["node6"]!!)
        assertEquals(1, priorities["node7"]!!)
    }

    @Test
    fun shouldCalculateCorrectlyForComplexTree6() {
        val priorities = calculateNodesPriorities(mapOf(
                "node1" to Node("node1", arrayOf(), setOf("node3", "node4"), setOf()),
                "node2" to Node("node2", arrayOf(), setOf("node4"), setOf()),
                "node3" to Node("node3", arrayOf(), setOf("node5"), setOf()),
                "node4" to Node("node4", arrayOf(), setOf("node5"), setOf()),
                "node5" to Node("node5", arrayOf(), setOf(), setOf())
        ))

        assertEquals(3, priorities["node1"]!!)
        assertEquals(3, priorities["node2"]!!)
        assertEquals(2, priorities["node3"]!!)
        assertEquals(2, priorities["node4"]!!)
        assertEquals(1, priorities["node5"]!!)
    }
}