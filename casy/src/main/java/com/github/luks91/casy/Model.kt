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

import com.github.luks91.casy.annotations.SyncRoot
import java.util.Arrays
import javax.lang.model.element.Element

internal data class EnvironmentData(
        val adjacency: Map<String, Node>,
        val paths: Map<String, List<String>>,
        val nodePriorities: Map<String, Long>,
        val rootData: RootData
)

internal data class Node(val nodeClass: String,
                         val topics: Array<String>,
                         val syncsAfter: Set<String>,
                         val triggers: Set<String>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (nodeClass != other.nodeClass) return false
        if (!Arrays.equals(topics, other.topics)) return false
        if (syncsAfter != other.syncsAfter) return false
        if (triggers != other.triggers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeClass.hashCode()
        result = 31 * result + Arrays.hashCode(topics)
        result = 31 * result + syncsAfter.hashCode()
        result = 31 * result + triggers.hashCode()
        return result
    }
}

internal data class RootData(val element: Element,
                             val allEmittersTopic: String,
                             val allNonPushEmittersTopic: String) {
    companion object {
        fun from(element: Element): RootData {
            val annotation = element.getAnnotation(SyncRoot::class.java)
            return RootData(element, annotation.allEmittersTopic, annotation.allNonPushEmittersTopic)
        }
    }
}