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
 * The default implementation for [Spec].
 *
 * @param prefix common prefix for items in this config spec
 */
open class ConfigSpec @JvmOverloads constructor(
    final override val prefix: String = "",
    items: Set<Item<*>> = mutableSetOf()
) : Spec {
    init {
        checkPath(prefix)
    }

    private val _items = items as? MutableSet<Item<*>> ?: items.toMutableSet()

    override val items: Set<Item<*>> = _items

    override fun addItem(item: Item<*>) {
        if (item !in _items) {
            _items += item
        } else {
            throw RepeatedItemException(item.name)
        }
    }

    /**
     * Specify a required item in this config spec.
     *
     * @param name item name without prefix
     * @param description description for this item
     * @return a property of a required item with prefix of this config spec
     */
    inline fun <reified T> required(name: String? = null, description: String = "") =
        object : RequiredProperty<T>(this, name, description, null is T) {}

    /**
     * Specify an optional item in this config spec.
     *
     * @param default default value returned before associating this item with specified value
     * @param name item name without prefix
     * @param description description for this item
     *
     * @return a property of an optional item with prefix of this config spec
     */
    inline fun <reified T> optional(default: T, name: String? = null, description: String = "") =
        object : OptionalProperty<T>(this, default, name, description, null is T) {}

    /**
     * Specify a lazy item in this config spec.
     *
     * @param name item name without prefix
     * @param description description for this item
     * @param thunk thunk used to evaluate value for this item
     * @return a property of a lazy item with prefix of this config spec
     */
    inline fun <reified T> lazy(
        name: String? = null,
        description: String = "",
        noinline thunk: (config: ItemContainer) -> T
    ) =
        object : LazyProperty<T>(this, thunk, name, description, null is T) {}
}
