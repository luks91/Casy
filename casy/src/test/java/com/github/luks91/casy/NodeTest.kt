package com.github.luks91.casy

import com.google.common.base.Strings
import com.squareup.kotlinpoet.TypeName
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

//Jacoco doesn't skip Kotlin generated methods for data classes.
//Adding dummy calls to cover those.
class NodeTest {
    @Test
    fun testCodeCoverageEnvironmentData() {
        val rootTypeName = Mockito.`mock`(TypeName::class.java)
        val topicsToEmitters = mapOf("one" to listOf("e1", "e2"))
        val nodePriorities = mapOf("e1" to 1L, "e2" to 2L)
        val groups = mapOf("gr1" to listOf("e1"))
        val tested = EnvironmentData(
                "packageName", "emittersName", rootTypeName,
                topicsToEmitters, nodePriorities, groups).apply {

            assertFalse(Strings.isNullOrEmpty(this.toString()))

            val (packageName) = this
            assertEquals("packageName", packageName)

            val (_, emittersName) = this
            assertEquals("emittersName", emittersName)

            val (_, _, capturedRootTypeName) = this
            assertEquals(rootTypeName, capturedRootTypeName)

            val (_, _, _, capturedTopicsToEmitters) = this
            assertEquals(topicsToEmitters, capturedTopicsToEmitters)

            val (_, _, _, _, capturedNodePriorities) = this
            assertEquals(nodePriorities, capturedNodePriorities)

            val(_, _, _, _, _, capturedGroups) = this
            assertEquals(groups, capturedGroups)
        }

        val testedCopy = tested.copy()
        assertEquals(tested, testedCopy)
        assertEquals(tested.hashCode(), testedCopy.hashCode())
    }

    @Test
    fun testCodeCoverage() {
        val nodeClass = "e1"
        val topics = listOf("topic1", "topic2")
        val syncsAfter = setOf("e2")
        val triggers = setOf("e3")
        val tested = Node(nodeClass, topics, syncsAfter, triggers).apply {
            assertFalse(Strings.isNullOrEmpty(this.toString()))

            val (capturedNodeClass) = this
            assertEquals(nodeClass, capturedNodeClass)

            val (_, capturedtopics) = this
            assertEquals(topics, capturedtopics)

            val (_, _, capturedSyncsAfter) = this
            assertEquals(syncsAfter, capturedSyncsAfter)

            val (_, _, _, capturedTriggers) = this
            assertEquals(triggers, capturedTriggers)
        }

        val testedCopy = tested.copy()
        assertEquals(tested, testedCopy)
        assertEquals(tested.hashCode(), testedCopy.hashCode())
    }
}