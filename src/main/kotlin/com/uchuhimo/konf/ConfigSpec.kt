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
 * Config spec is specification for config.
 *
 * Config spec describes a group of items with common prefix, which can be loaded into config
 * together using [Config.addSpec].
 * Config spec also provides convenient API to specify item in it without hand-written object
 * declaration.
 *
 * @param prefix common prefix for items in this config spec
 * @see Config
 */
open class ConfigSpec(
        /**
         * Common prefix for items in this config spec.
         *
         * Default value is `""`, which means names of items in this config spec are unqualified.
         */
        val prefix: String = "") {
    private val _items = mutableListOf<Item<*>>()

    /**
     * List of specified items in this config spec.
     */
    val items: List<Item<*>> = _items

    /**
     * Specify a required item in this config spec.
     *
     * @param name item name without prefix
     * @param description description for this item
     * @return a required item with prefix of this config spec
     */
    inline fun <reified T : Any> required(name: String, description: String = "") =
            object : RequiredItem<T>(this, name, description) {}

    /**
     * Specify an optional item in this config spec.
     *
     * @param name item name without prefix
     * @param default default value returned before associating this item with specified value
     * @param description description for this item
     *
     * @return an optional item with prefix of this config spec
     */
    inline fun <reified T : Any> optional(name: String, default: T, description: String = "") =
            object : OptionalItem<T>(this, name, default, description) {}

    /**
     * Specify a lazy item in this config spec.
     *
     * @param name item name without prefix
     * @param description description for this item
     * @param thunk thunk used to evaluate value for this item
     * @return a lazy item with prefix of this config spec
     */
    inline fun <reified T : Any> lazy(
            name: String,
            description: String = "",
            noinline thunk: (config: ItemContainer) -> T) =
            object : LazyItem<T>(this, name, thunk, description) {}

    /**
     * Qualify item name with prefix of this config spec.
     *
     * When prefix is empty, original item name will be returned.
     *
     * @param name item name without prefix
     * @return qualified item name
     */
    fun qualify(name: String): String = if (prefix.isEmpty()) name else "$prefix.$name"

    internal fun addItem(item: Item<*>) {
        _items += item
    }
}
