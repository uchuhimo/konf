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

/**
 * Container of items.
 *
 * Item container contains read-only access operations for item.
 *
 * @see Config
 */
interface ItemContainer : Iterable<Item<*>> {
    /**
     * Get associated value with specified item.
     *
     * @param item config item
     * @return associated value
     */
    operator fun <T> get(item: Item<T>): T

    /**
     * Get associated value with specified item name.
     *
     * @param name item name
     * @return associated value
     */
    operator fun <T> get(name: String): T

    /**
     * Returns associated value if specified item exists, `null` otherwise.
     *
     * @param item config item
     * @return associated value if specified item exists, `null` otherwise
     */
    fun <T> getOrNull(item: Item<T>): T?

    /**
     * Returns associated value if specified item name exists, `null` otherwise.
     *
     * @param name item name
     * @return associated value if specified item name exists, `null` otherwise
     */
    fun <T> getOrNull(name: String): T?

    /**
     * Get associated value with specified item name.
     *
     * @param name item name
     * @return associated value
     */
    operator fun <T> invoke(name: String): T = get(name)

    /**
     * Returns iterator of items in this item container.
     *
     * @return iterator of items in this item container
     */
    override operator fun iterator(): Iterator<Item<*>>

    /**
     * Whether this item container contains specified item or not.
     *
     * @param item config item
     * @return `true` if this item container contains specified item, `false` otherwise
     */
    operator fun contains(item: Item<*>): Boolean

    /**
     * Whether this item container contains item with specified name or not.
     *
     * @param name item name
     * @return `true` if this item container contains item with specified name, `false` otherwise
     */
    operator fun contains(name: String): Boolean

    /**
     * Whether this item container contains the specified path or not.
     *
     * @param path the specified path
     * @return `true` if this item container contains the specified path, `false` otherwise
     */
    operator fun contains(path: Path): Boolean

    /**
     * Returns the qualified name of the specified item.
     *
     * @param item the specified item
     * @return the qualified name of the specified item
     */
    fun nameOf(item: Item<*>): String

    /**
     * Returns the qualified path of the specified item.
     *
     * @param item the specified item
     * @return the qualified path of the specified item
     */
    fun pathOf(item: Item<*>): Path = nameOf(item).toPath()

    /**
     * List of items in this item container.
     */
    val items: List<Item<*>>
        get() = mutableListOf<Item<*>>().apply {
            addAll(this@ItemContainer.iterator().asSequence())
        }

    /**
     * List of qualified names of items in this item container.
     */
    val nameOfItems: List<String> get() = itemWithNames.map { it.second }

    /**
     * List of items with the corresponding qualified names in this item container.
     */
    val itemWithNames: List<Pair<Item<*>, String>>
}
