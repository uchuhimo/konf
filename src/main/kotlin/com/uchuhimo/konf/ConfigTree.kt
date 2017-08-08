/*
 * Copyright 2017 the original author or authors.
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
 * Tree representation of config.
 */
sealed class ConfigTree {
    /**
     * Path of this tree in config.
     */
    abstract val path: List<String>

    /**
     * Returns a deep copy of this config tree.
     */
    abstract fun deepCopy(): ConfigTree

    /**
     * Whether this tree represent an item or not.
     */
    abstract val isItem: Boolean

    /**
     * Iterates this config tree in an event-driven manner.
     *
     * @param onEnterItem action after entering a path node
     * @param onLeavePath action before leaving a path node
     * @param onEnterPath action when entering an item node
     */
    fun visit(
            onEnterPath: (node: ConfigPathNode) -> Unit = { _ -> },
            onLeavePath: (node: ConfigPathNode) -> Unit = { _ -> },
            onEnterItem: (node: ConfigItemNode<*>) -> Unit = { _ -> }) {
        when (this) {
            is ConfigPathNode -> {
                onEnterPath(this)
                for (child in children) {
                    child.visit(onEnterPath, onLeavePath, onEnterItem)
                }
                onLeavePath(this)
            }
            is ConfigItemNode<*> -> {
                onEnterItem(this)
            }
        }
    }

    /**
     * Items in this config tree.
     */
    val items: Iterable<Item<*>> get() {
        val items = mutableListOf<Item<*>>()
        visit(onEnterItem = { node -> items += node.item })
        return items
    }
}

/**
 * Represents an item node in config tree.
 */
class ConfigItemNode<T : Any>(
        override val path: List<String>,
        val item: Item<T>
) : ConfigTree() {
    override fun deepCopy(): ConfigItemNode<T> = ConfigItemNode(path, item)

    override val isItem: Boolean = true
}

/**
 * Represents a path node in config tree.
 */
class ConfigPathNode(
        override val path: List<String>,
        val children: MutableList<ConfigTree>
) : ConfigTree() {
    override fun deepCopy(): ConfigPathNode =
            ConfigPathNode(path, children.mapTo(mutableListOf(), ConfigTree::deepCopy))

    override val isItem: Boolean = false
}
