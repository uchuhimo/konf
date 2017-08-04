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

open class ConfigSpec(val prefix: String = "") {
    private val _items = mutableListOf<Item<*>>()

    val items: List<Item<*>> = _items

    inline fun <reified T : Any> required(name: String, description: String = "") =
            object : RequiredItem<T>(
                    spec = this,
                    name = name,
                    description = description
            ) {}

    inline fun <reified T : Any> optional(name: String, default: T, description: String = "") =
            object : OptionalItem<T>(
                    spec = this,
                    name = name,
                    default = default,
                    description = description
            ) {}

    inline fun <reified T : Any> lazy(
            name: String,
            description: String = "",
            placeholder: String = "",
            noinline default: (ConfigGetter) -> T) =
            object : LazyItem<T>(
                    spec = this,
                    name = name,
                    thunk = default,
                    placeholder = placeholder,
                    description = description
            ) {}

    fun qualify(name: String): String = if (prefix.isEmpty()) name else "$prefix.$name"

    internal fun addItem(item: Item<*>) {
        _items += item
    }
}
