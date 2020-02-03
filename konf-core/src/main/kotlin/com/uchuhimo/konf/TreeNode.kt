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
     * Children nodes in this tree node with their names as keys.
     */
    val children: MutableMap<String, TreeNode>

    /**
     * The comments assigned to this tree node.
     */
    var comments: String

    /**
     * Associate path with specified node.
     *
     * @param path path
     * @param node associated node
     */
    operator fun set(path: Path, node: TreeNode) {
        if (path.isEmpty()) {
            throw PathConflictException(path.name)
        }
        val key = path.first()
        if (this is LeafNode) {
            throw PathConflictException(path.name)
        }
        try {
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
        } catch (_: PathConflictException) {
            throw PathConflictException(path.name)
        } finally {
            if (this is MapNode && isPlaceHolder) {
                isPlaceHolder = false
            }
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
     * Returns a node backing by specified fallback node.
     *
     * @param fallback fallback node
     * @return a node backing by specified fallback node
     */
    fun withFallback(fallback: TreeNode): TreeNode {
        fun traverseTree(facade: TreeNode, fallback: TreeNode, path: Path): TreeNode {
            if (facade is LeafNode || fallback is LeafNode) {
                return facade
            } else {
                return ContainerNode(facade.children.toMutableMap().also { map ->
                    for ((key, child) in fallback.children) {
                        if (key in facade.children) {
                            map[key] = traverseTree(facade.children.getValue(key), child, path + key)
                        } else {
                            map[key] = child
                        }
                    }
                })
            }
        }
        return traverseTree(this, fallback, "".toPath())
    }

    /**
     * Returns a node overlapped by the specified facade node.
     *
     * @param facade the facade node
     * @return a node overlapped by the specified facade node
     */
    operator fun plus(facade: TreeNode): TreeNode = facade.withFallback(this)

    /**
     * Returns a tree node containing all nodes of the original tree node
     * except the nodes contained in the given [other] tree node.
     *
     * @return a tree node
     */
    operator fun minus(other: TreeNode): TreeNode {
        fun traverseTree(left: TreeNode, right: TreeNode): TreeNode {
            if (left is LeafNode) {
                return EmptyNode
            } else {
                if (right is LeafNode) {
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
            return mutableListOf<String>().also { list ->
                fun traverseTree(node: TreeNode, path: Path) {
                    if (node is LeafNode) {
                        list.add(path.name)
                    } else {
                        node.children.forEach { (key, child) ->
                            traverseTree(child, path + key)
                        }
                    }
                }
                traverseTree(this, "".toPath())
            }
        }

    fun firstPath(predicate: (TreeNode) -> Boolean): Path? {
        fun traverseTree(node: TreeNode, path: Path): Path? {
            if (predicate(node)) {
                return path
            } else {
                node.children.forEach { (key, child) ->
                    val matchPath = traverseTree(child, path + key)
                    if (matchPath != null) {
                        return matchPath
                    }
                }
                return null
            }
        }
        return traverseTree(this, "".toPath())
    }

    /**
     * Map of all leaves indexed by paths in this tree node.
     */
    val leafByPath: Map<String, TreeNode>
        get() {
            return mutableMapOf<String, TreeNode>().also { map ->
                fun traverseTree(node: TreeNode, path: Path) {
                    if (node is LeafNode) {
                        map[path.name] = node
                    } else {
                        node.children.forEach { (key, child) ->
                            traverseTree(child, path + key)
                        }
                    }
                }
                traverseTree(this, "".toPath())
            }
        }

    fun withoutPlaceHolder(): TreeNode {
        when (this) {
            is NullNode -> return this
            is ValueNode -> return this
            is ListNode -> return this
            is MapNode -> {
                val newChildren = children.mapValues { (_, child) ->
                    child.withoutPlaceHolder()
                }
                if (newChildren.isNotEmpty() && newChildren.all { (_, child) -> child is MapNode && child.isPlaceHolder }) {
                    return ContainerNode.placeHolder()
                } else {
                    return withMap(newChildren.filterValues { !(it is MapNode && it.isPlaceHolder) })
                }
            }
            else -> return this
        }
    }

    fun isEmpty(): Boolean {
        when (this) {
            is EmptyNode -> return true
            is MapNode -> {
                return children.isEmpty() || children.all { (_, child) -> child.isEmpty() }
            }
            else -> return false
        }
    }

    fun isPlaceHolderNode(): Boolean {
        when (this) {
            is MapNode -> {
                if (isPlaceHolder) {
                    return true
                } else {
                    return children.isNotEmpty() && children.all { (_, child) -> child.isPlaceHolderNode() }
                }
            }
            else -> return false
        }
    }
}

interface LeafNode : TreeNode

interface MapNode : TreeNode {
    fun withMap(map: Map<String, TreeNode>): MapNode = throw NotImplementedError()
    var isPlaceHolder: Boolean
}

val emptyMutableMap: MutableMap<String, TreeNode> = Collections.unmodifiableMap(mutableMapOf())

interface ValueNode : LeafNode {
    val value: Any
    override val children: MutableMap<String, TreeNode>
        get() = emptyMutableMap
}

interface NullNode : LeafNode

interface ListNode : LeafNode {
    val list: List<TreeNode>
    fun withList(list: List<TreeNode>): ListNode = throw NotImplementedError()
}

/**
 * Tree node that contains children nodes.
 */
open class ContainerNode @JvmOverloads constructor(
    override val children: MutableMap<String, TreeNode>,
    override var isPlaceHolder: Boolean = false,
    override var comments: String = ""
) : MapNode {

    override fun withMap(map: Map<String, TreeNode>): MapNode {
        val isPlaceHolder = map.isEmpty() && this.isPlaceHolder
        return if (map is MutableMap<String, TreeNode>) {
            ContainerNode(map, isPlaceHolder, comments)
        } else {
            ContainerNode(map.toMutableMap(), isPlaceHolder, comments)
        }
    }

    companion object {
        fun empty(): ContainerNode = ContainerNode(mutableMapOf())
        fun placeHolder(): ContainerNode = ContainerNode(mutableMapOf(), true)
    }
}

/**
 * Tree node that represents a empty tree.
 */
object EmptyNode : LeafNode {
    override val children: MutableMap<String, TreeNode> = emptyMutableMap
    override var comments: String = ""
}
