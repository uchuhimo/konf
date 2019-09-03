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

package com.uchuhimo.konf

import java.util.Collections

/**
 * Tree node that represents internal structure of config/source.
 */
interface TreeNode {
    /**
     * Check whether this tree node is a leaf node.
     *
     * @return whether this tree node is a leaf node
     */
    fun isLeaf(): Boolean = children.isEmpty()

    /**
     * Children nodes in this tree node with their names as keys.
     */
    val children: MutableMap<String, TreeNode>

    /**
     * Associate path with specified node.
     *
     * @param path path
     * @param node associated node
     */
    operator fun set(path: Path, node: TreeNode) {
        if (path.isEmpty()) {
            throw InvalidPathException(path.name)
        }
        val key = path.first()
        return if (path.size == 1) {
            children[key] = node
        } else {
            val rest = path.drop(1)
            var child = children[key]
            if (child == null) {
                child = ContainerNode(mutableMapOf())
                children[key] = child
            }
            child[rest] = node
        }
    }

    /**
     * Associate path with specified node.
     *
     * @param path path
     * @param node associated node
     */
    operator fun set(path: String, node: TreeNode) {
        set(path.toPath(), node)
    }

    /**
     * Whether this tree node contains node(s) in specified path or not.
     *
     * @param path item path
     * @return `true` if this tree node contains node(s) in specified path, `false` otherwise
     */
    operator fun contains(path: Path): Boolean {
        return if (path.isEmpty()) {
            true
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val result = children[key]
            if (result != null) {
                return rest in result
            } else {
                return false
            }
        }
    }

    /**
     * Returns tree node in specified path if this tree node contains value(s) in specified path,
     * `null` otherwise.
     *
     * @param path item path
     * @return tree node in specified path if this tree node contains value(s) in specified path,
     * `null` otherwise
     */
    fun getOrNull(path: Path): TreeNode? {
        return if (path.isEmpty()) {
            this
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val result = children[key]
            result?.getOrNull(rest)
        }
    }

    /**
     * Returns tree node in specified path if this tree node contains value(s) in specified path,
     * `null` otherwise.
     *
     * @param path item path
     * @return tree node in specified path if this tree node contains value(s) in specified path,
     * `null` otherwise
     */
    fun getOrNull(path: String): TreeNode? = getOrNull(path.toPath())

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

interface MapNode : TreeNode

interface ValueNode : TreeNode {
    val value: Any
}

interface NullNode : TreeNode

interface ListNode : TreeNode {
    override fun isLeaf(): Boolean = true

    val list: List<TreeNode>
}

/**
 * Tree node that contains children nodes.
 */
open class ContainerNode(override val children: MutableMap<String, TreeNode>) : MapNode {
    companion object {
        fun empty(): ContainerNode = ContainerNode(mutableMapOf())
    }
}

/**
 * Tree node that represents a empty tree.
 */
object EmptyNode : ContainerNode(Collections.unmodifiableMap(mutableMapOf()))
