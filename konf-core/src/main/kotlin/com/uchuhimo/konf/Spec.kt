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

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Config spec is specification for config.
 *
 * Config spec describes a group of items with common prefix, which can be loaded into config
 * together using [Config.addSpec].
 * Config spec also provides convenient API to specify item in it without hand-written object
 * declaration.
 *
 * @see Config
 */
interface Spec {
    /**
     * Common prefix for items in this config spec.
     *
     * An empty prefix means names of items in this config spec are unqualified.
     */
    val prefix: String

    /**
     * The description of the spec.
     */
    val description: String
        get() = ""

    /**
     * Qualify item name with prefix of this config spec.
     *
     * When prefix is empty, original item name will be returned.
     *
     * @param item the config item
     * @return qualified item name
     */
    fun qualify(item: Item<*>): String = (prefix.toPath() + item.path).name

    /**
     * Add the specified item into this config spec.
     *
     * @param item the specified item
     */
    fun addItem(item: Item<*>)

    /**
     * Set of specified items in this config spec.
     */
    val items: Set<Item<*>>

    /**
     * Add the specified inner spec into this config spec.
     *
     * @param spec the specified spec
     */
    fun addInnerSpec(spec: Spec)

    /**
     * Set of inner specs in this config spec.
     */
    val innerSpecs: Set<Spec>

    /**
     * Returns a config spec overlapped by the specified facade config spec.
     *
     * New items will be added to the facade config spec.
     *
     * @param spec the facade config spec
     * @return a config spec overlapped by the specified facade config spec
     */
    operator fun plus(spec: Spec): Spec {
        return object : Spec by spec {
            override fun addItem(item: Item<*>) {
                if (item !in this@Spec.items) {
                    spec.addItem(item)
                } else {
                    throw RepeatedItemException(item.name)
                }
            }

            override val items: Set<Item<*>>
                get() = this@Spec.items + spec.items

            override fun qualify(item: Item<*>): String {
                return if (item in spec.items) {
                    spec.qualify(item)
                } else {
                    this@Spec.qualify(item)
                }
            }
        }
    }

    /**
     * Returns a config spec backing by the specified fallback config spec.
     *
     * New items will be added to the current config spec.
     *
     * @param spec the fallback config spec
     * @return a config spec backing by the specified fallback config spec
     */
    fun withFallback(spec: Spec): Spec = spec + this

    /**
     * Returns sub-spec in the specified path.
     *
     * @param path the specified path
     * @return sub-source with specified prefix
     */
    operator fun get(path: String): Spec = get(prefix.toPath(), path.toPath())

    private fun get(prefix: Path, path: Path): Spec {
        return if (path.isEmpty()) {
            this
        } else if (prefix.size >= path.size && prefix.subList(0, path.size) == path) {
            ConfigSpec(prefix.subList(path.size, prefix.size).name, items, innerSpecs)
        } else {
            if (prefix.size < path.size && path.subList(0, prefix.size) == prefix) {
                val pathForInnerSpec = path.subList(prefix.size, path.size).name
                val filteredInnerSpecs = innerSpecs.mapNotNull { spec ->
                    try {
                        spec[pathForInnerSpec]
                    } catch (_: NoSuchPathException) {
                        null
                    }
                }
                if (filteredInnerSpecs.isEmpty()) {
                    throw NoSuchPathException(path.name)
                } else if (filteredInnerSpecs.size == 1) {
                    return filteredInnerSpecs[0]
                } else {
                    ConfigSpec("", emptySet(), filteredInnerSpecs.toMutableSet())
                }
            } else {
                throw NoSuchPathException(path.name)
            }
        }
    }

    /**
     * Returns config spec with the specified additional prefix.
     *
     * @param prefix additional prefix
     * @return config spec with the specified additional prefix
     */
    fun withPrefix(prefix: String): Spec = withPrefix(this.prefix.toPath(), prefix.toPath())

    private fun withPrefix(prefix: Path, newPrefix: Path): Spec {
        return if (newPrefix.isEmpty()) {
            this
        } else {
            ConfigSpec((newPrefix + prefix).name, items, innerSpecs, description)
        }
    }

    /**
     * Returns config spec with the specified description.
     *
     * @param description description
     * @return config spec with the specified description
     */
    fun withDescription(description: String): Spec {
        if (this.description == description)
            return this
        return ConfigSpec(prefix, items, innerSpecs, description)
    }

    companion object {
        /**
         * A dummy implementation for [Spec].
         *
         * It will swallow all items added to it. Used for items belonged to no config spec.
         */
        val dummy: Spec = object : Spec {
            override val prefix: String = ""

            override fun addItem(item: Item<*>) {}

            override val items: Set<Item<*>> = emptySet()

            override fun addInnerSpec(spec: Spec) {}

            override val innerSpecs: Set<Spec> = emptySet()
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
inline fun <reified T> Spec.required(name: String? = null, description: String = "") =
    object : RequiredProperty<T>(this, name, description, null is T) {}

open class RequiredProperty<T>(
    private val spec: Spec,
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(RequiredProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, RequiredItem<T>> {
        val item = object : RequiredItem<T>(spec, name
            ?: property.name, description, type, nullable) {}
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
inline fun <reified T> Spec.optional(default: T, name: String? = null, description: String = "") =
    object : OptionalProperty<T>(this, default, name, description, null is T) {}

open class OptionalProperty<T>(
    private val spec: Spec,
    private val default: T,
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(OptionalProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, OptionalItem<T>> {
        val item = object : OptionalItem<T>(spec, name
            ?: property.name, default, description, type, nullable) {}
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
inline fun <reified T> Spec.lazy(
    name: String? = null,
    description: String = "",
    noinline thunk: (config: ItemContainer) -> T
) =
    object : LazyProperty<T>(this, thunk, name, description, null is T) {}

open class LazyProperty<T>(
    private val spec: Spec,
    private val thunk: (config: ItemContainer) -> T,
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(LazyProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadOnlyProperty<Any?, LazyItem<T>> {
        val item = object : LazyItem<T>(spec, name
            ?: property.name, thunk, description, type, nullable) {}
        return object : ReadOnlyProperty<Any?, LazyItem<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): LazyItem<T> = item
        }
    }
}
