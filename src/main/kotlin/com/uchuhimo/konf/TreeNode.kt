/*
 * Copyright 2017-2018 the original author or authors.
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

package com.uchuhimo.konf

/**
 * Tree node that represents internal structure of config/source.
 */
interface TreeNode {
    /**
     * Check whether this tree node is a leaf node.
     *
     * @return whether this tree node is a leaf node
     */
    fun isLeaf(): Boolean

    /**
     * Children nodes in this tree node with their names as keys.
     */
    val children: Map<String, TreeNode>

    /**
     * Returns a tree node containing all nodes of the original tree node
     * except the nodes contained in the given [other] tree node.
     *
     * @return a tree node
     */
    operator fun minus(other: TreeNode): TreeNode {
        fun traverseTree(left: TreeNode, right: TreeNode): TreeNode {
            if (left.isLeaf()) {
                return EmptyNode
            } else {
                if (right.isLeaf()) {
                    return EmptyNode
                } else {
                    val leftKeys = left.children.keys
                    val rightKeys = right.children.keys
                    val diffKeys = leftKeys - rightKeys
                    val sharedKeys = leftKeys.intersect(rightKeys)
                    val children = mutableMapOf<String, TreeNode>()
                    diffKeys.forEach { key ->
                        children[key] = left.children[key]!!
                    }
                    sharedKeys.forEach { key ->
                        val child = traverseTree(left.children[key]!!, right.children[key]!!)
                        if (child != EmptyNode) {
                            children[key] = child
                        }
                    }
                    return if (children.isEmpty()) {
                        EmptyNode
                    } else {
                        ContainerNode(children)
                    }
                }
            }
        }
        return traverseTree(this, other)
    }

    /**
     * List of all paths in this tree node.
     */
    val paths: List<String>
        get() {
            return mutableListOf<String>().apply {
                fun traverseTree(node: TreeNode, path: Path) {
                    if (node.isLeaf()) {
                        add(path.name)
                    } else {
                        node.children.forEach { key, child ->
                            traverseTree(child, path + key)
                        }
                    }
                }
                traverseTree(this@TreeNode, emptyList())
            }
        }
}

/**
 * Tree node that contains children nodes.
 */
open class ContainerNode(override val children: Map<String, TreeNode>) : TreeNode {
    override fun isLeaf(): Boolean = false
}

/**
 * Tree node without any child node.
 */
open class ValueNode : TreeNode {
    override fun isLeaf(): Boolean = true
    override val children: Map<String, TreeNode>
        get() = emptyMap()
}

/**
 * Tree node that represents a empty tree.
 */
object EmptyNode : ValueNode()
