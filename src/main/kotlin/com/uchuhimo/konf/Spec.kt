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
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Spec {
    /**
     * Common prefix for items in this config spec.
     *
     * Default value is `""`, which means names of items in this config spec are unqualified.
     */
    val prefix: String

    /**
     * Qualify item name with prefix of this config spec.
     *
     * When prefix is empty, original item name will be returned.
     *
     * @param name item name without prefix
     * @return qualified item name
     */
    fun qualify(name: String): String = if (prefix.isEmpty()) name else "$prefix.$name"

    fun addItem(item: Item<*>)

    /**
     * List of specified items in this config spec.
     */
    val items: List<Item<*>>

    operator fun get(path: String): Spec {
        return if (path.isEmpty()) {
            this
        } else {
            if (prefix.startsWith(path)) {
                val restPrefix = prefix.removePrefix(path)
                if (restPrefix.startsWith('.')) {
                    ConfigSpec(restPrefix.removePrefix("."), items)
                } else {
                    throw NoSuchPathException(path)
                }
            } else {
                throw NoSuchPathException(path)
            }
        }
    }

    fun withPrefix(prefix: String): Spec {
        return if (prefix.isEmpty()) {
            this
        } else {
            ConfigSpec("$prefix.${this.prefix}", items)
        }
    }
}

/**
 * Specify a required item in this config spec.
 *
 * @param name item name without prefix
 * @param description description for this item
 * @return a property of a required item with prefix of this config spec
 */
inline fun <reified T : Any> Spec.required(name: String? = null, description: String = "") =
    object : RequiredProperty<T>(this, name, description) {}

open class RequiredProperty<T : Any>(
    private val spec: Spec,
    private val name: String? = null,
    private val description: String
) {
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(RequiredProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, RequiredItem<T>> {
        val item = object : RequiredItem<T>(spec, name
            ?: property.name, description, type) {}
        return object : ReadOnlyProperty<Any?, RequiredItem<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): RequiredItem<T> = item
        }
    }
}

/**
 * Specify an optional item in this config spec.
 *
 * @param default default value returned before associating this item with specified value
 * @param name item name without prefix
 * @param description description for this item
 *
 * @return a property of an optional item with prefix of this config spec
 */
inline fun <reified T : Any> Spec.optional(default: T, name: String? = null, description: String = "") =
    object : OptionalProperty<T>(this, default, name, description) {}

open class OptionalProperty<T : Any>(
    private val spec: Spec,
    private val default: T,
    private val name: String? = null,
    private val description: String
) {
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(OptionalProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, OptionalItem<T>> {
        val item = object : OptionalItem<T>(spec, name
            ?: property.name, default, description, type) {}
        return object : ReadOnlyProperty<Any?, OptionalItem<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): OptionalItem<T> = item
        }
    }
}

/**
 * Specify a lazy item in this config spec.
 *
 * @param name item name without prefix
 * @param description description for this item
 * @param thunk thunk used to evaluate value for this item
 * @return a property of a lazy item with prefix of this config spec
 */
inline fun <reified T : Any> Spec.lazy(
    name: String? = null,
    description: String = "",
    noinline thunk: (config: ItemContainer) -> T
) =
    object : LazyProperty<T>(this, thunk, name, description) {}

open class LazyProperty<T : Any>(
    private val spec: Spec,
    private val thunk: (config: ItemContainer) -> T,
    private val name: String? = null,
    private val description: String
) {
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(LazyProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, LazyItem<T>> {
        val item = object : LazyItem<T>(spec, name
            ?: property.name, thunk, description, type) {}
        return object : ReadOnlyProperty<Any?, LazyItem<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): LazyItem<T> = item
        }
    }
}
