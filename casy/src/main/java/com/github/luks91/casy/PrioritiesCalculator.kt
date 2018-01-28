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

internal fun calculateNodesPriorities(adjacency: Map<String, Node>) = mutableMapOf<String, Long>()
        .apply { adjacency.forEach { this[it.key] ?: processNode(this, adjacency, it.value) } }

private fun processNode(nodePriorities: MutableMap<String, Long>, adjacency: Map<String, Node>, node: Node) {
    var maxWeight = 0L
    node.syncsAfter.forEach {
        processNode(nodePriorities, adjacency, adjacency.get(it)!!)
        nodePriorities[it]?.let { maxWeight = Math.max(maxWeight, it) }
    }

    nodePriorities.put(node.nodeClass, maxWeight + 1)
}