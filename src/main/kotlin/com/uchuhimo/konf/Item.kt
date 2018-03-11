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

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory

/**
 * Item that can be contained by config.
 *
 * Item can be associated with value in config, containing metadata for the value.
 * The metadata for value includes name, path, type, description and so on.
 * Item can be used as key to operate value in config, guaranteeing type safety.
 * There are three kinds of item: [required item][RequiredItem], [optional item][OptionalItem]
 * and [lazy item][LazyItem].
 *
 * @param T type of value that can be associated with this item.
 * @param spec config spec that contains this item
 * @param name item name without prefix
 * @param description description for this item
 * @see Config
 */
sealed class Item<T : Any>(
    /**
     * Config spec that contains this item.
     */
    val spec: Spec,
    /**
     * Item name without prefix.
     */
    val name: String,
    /**
     * Description for this item.
     */
    val description: String = "",
    type: JavaType? = null
) {
    init {
        checkPath(name)
        @Suppress("LeakingThis")
        spec.addItem(this)
    }

    /**
     * Item path without prefix.
     */
    val path: Path = this.name.toPath()

    /**
     * Type of value that can be associated with this item.
     */
    @Suppress("LeakingThis")
    val type: JavaType = type ?: TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(Item::class.java).bindings.typeParameters[0]

    /**
     * Whether this is a required item or not.
     */
    open val isRequired: Boolean get() = false

    /**
     * Whether this is an optional item or not.
     */
    open val isOptional: Boolean get() = false

    /**
     * Whether this is a lazy item or not.
     */
    open val isLazy: Boolean get() = false

    /**
     * Cast this item to a required item.
     */
    val asRequiredItem: RequiredItem<T> get() = this as RequiredItem<T>

    /**
     * Cast this item to an optional item.
     */
    val asOptionalItem: OptionalItem<T> get() = this as OptionalItem<T>

    /**
     * Cast this item to a lazy item.
     */
    val asLazyItem: LazyItem<T> get() = this as LazyItem<T>
}

/**
 * Type of Item path.
 */
typealias Path = List<String>

/**
 * Returns corresponding item name of the item path.
 *
 * @receiver item path
 * @return item name
 */
val Path.name: String get() = joinToString(".")

/**
 * Returns corresponding item path of the item name.
 *
 * @receiver item name
 * @return item path
 */
fun String.toPath(): Path {
    return if (isEmpty()) {
        listOf()
    } else {
        val path = this.split('.')
        check("" !in path) { "$this is not a valid path" }
        path
    }
}

fun checkPath(path: String) {
    if (path.isNotEmpty()) {
        check("" !in path.split('.')) { "$path is not a valid path" }
    }
}

/**
 * Required item without default value.
 *
 * Required item must be set with value before retrieved in config.
 */
open class RequiredItem<T : Any> @JvmOverloads constructor(
    spec: Spec,
    name: String,
    description: String = "",
    type: JavaType? = null
) : Item<T>(spec, name, description, type) {
    override val isRequired: Boolean = true
}

/**
 * Optional item with default value.
 *
 * Before associated with specified value, default value will be returned when accessing.
 * After associated with specified value, the specified value will be returned when accessing.
 */
open class OptionalItem<T : Any> @JvmOverloads constructor(
    spec: Spec,
    name: String,
    /**
     * Default value returned before associating this item with specified value.
     */
    val default: T,
    description: String = "",
    type: JavaType? = null
) : Item<T>(spec, name, description, type) {
    override val isOptional: Boolean = true
}

/**
 * Lazy item evaluated value every time from thunk before associated with specified value.
 *
 * Before associated with specified value, value evaluated from thunk will be returned when accessing.
 * After associated with specified value, the specified value will be returned when accessing.
 * Returned value of the thunk will not be cached. The thunk will be evaluated every time
 * when needed to reflect modifying of other values in config.
 */
open class LazyItem<T : Any> @JvmOverloads constructor(
    spec: Spec,
    name: String,
    /**
     * Thunk used to evaluate value for this item.
     *
     * [ItemContainer] is provided as evaluation environment to avoid unexpected modification
     * to config.
     * Thunk will be evaluated every time when needed to reflect modifying of other values in config.
     */
    val thunk: (config: ItemContainer) -> T,
    description: String = "",
    type: JavaType? = null
) : Item<T>(spec, name, description, type) {
    override val isLazy: Boolean = true
}
