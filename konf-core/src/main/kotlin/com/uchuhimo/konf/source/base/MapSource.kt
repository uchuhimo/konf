/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.CommentableNode
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.SourceInfo

/**
 * Source from a hierarchical map.
 */
open class MapSource(
    val map: Map<String, Any>,
    type: String = "",
    info: SourceInfo = SourceInfo()
) : ValueSource(map, type.notEmptyOr("map"), info)

/**
 * Returns a hierarchical map for this config.
 *
 * The returned map contains all items in this config.
 * This map can be loaded into config as [com.uchuhimo.konf.source.base.MapSource] using
 * `config.from.map.hierarchical(map)`.
 */
@Suppress("UNCHECKED_CAST")
fun Config.toHierarchicalMap(): Map<String, Any> {
    return toTree().toHierarchical() as Map<String, Any>
}

/**
 * Returns a hierarchical map for this config.
 *
 * The returned map contains all items in this config.
 * This map can be loaded into config as [com.uchuhimo.konf.source.base.MapSource] using
 * `config.from.map.hierarchical(map)`.
 */
fun Config.toHierarchicalMapNode(): HierarchicalTreeNode.Map {
    return toTree().toHierarchicalNode() as HierarchicalTreeNode.Map
}

/**
 * Returns a hierarchical value for this tree node.
 *
 * The returned value contains all items in this tree node.
 */
fun TreeNode.toHierarchical(): Any = withoutPlaceHolder().toHierarchicalInternal()

/**
 * Returns a hierarchical value for this tree node.
 *
 * The returned value contains all items in this tree node.
 */
fun TreeNode.toHierarchicalNode(): HierarchicalTreeNode = withoutPlaceHolder().toHierarchicalNodeInternal()

private fun TreeNode.toHierarchicalInternal(): Any {
    return when (this) {
        is ValueNode -> value
        is ListNode -> list.map { it.toHierarchicalInternal() }
        else -> children.mapValues { (_, child) -> child.toHierarchicalInternal() }
    }
}

private fun TreeNode.toHierarchicalNodeInternal(): HierarchicalTreeNode {
    return when (this) {
        is ValueNode -> HierarchicalTreeNode.Value(value, this.comments)
        is ListNode -> HierarchicalTreeNode.List(list.map { it.toHierarchicalNodeInternal() }, this.comments)
        else -> HierarchicalTreeNode.Map(children.mapValues { (_, child) -> child.toHierarchicalNodeInternal() },
                if (this is CommentableNode) this.comments else null)
    }
}

/**
 * Represents a value of a hierarchical map.
 */
sealed class HierarchicalTreeNode {

    abstract val comments: String?

    class Value(val value: Any, override val comments: String?) : HierarchicalTreeNode()

    class Map(val map: kotlin.collections.Map<String, HierarchicalTreeNode>, override val comments: String?) : HierarchicalTreeNode()

    class List(val list: kotlin.collections.List<HierarchicalTreeNode>, override val comments: String?) : HierarchicalTreeNode()
}

/**
 * Source from an empty map.
 */
class EmptyMapSource : MapSource(emptyMap(), "empty map")
