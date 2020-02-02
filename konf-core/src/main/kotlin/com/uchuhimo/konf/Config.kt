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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.uchuhimo.konf.annotation.JavaApi
import com.uchuhimo.konf.source.DefaultLoaders
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.kvToTree
import java.util.Deque
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Config containing items and associated values.
 *
 * Config contains items, which can be loaded with [addSpec].
 * Config contains values, each of which is associated with corresponding item.
 * Values can be loaded from [source][Source] with [withSource] or [from].
 *
 * Config contains read-write access operations for item.
 * Items in config is in one of three states:
 * - Unset. Item has not associated value in this state.
 *   Use [unset] to change item to this state.
 * - Unevaluated. Item is lazy and the associated value will be evaluated when accessing.
 *   Use [lazySet] to change item to this state.
 * - Evaluated.  Item has associated value which is evaluated.
 *   Use [set] to change item to this state.
 *
 * Config is cascading.
 * Config can fork from another config by adding a new layer on it.
 * The forked config is called child config, and the original config is called parent config.
 * A config without parent config is called root config. The new layer added by child config
 * is called facade layer.
 * Config with ancestor configs has multiple layers. All set operation is executed in facade layer
 * of config.
 * Descendant config inherits items and values in ancestor configs, and can override values for
 * items in ancestor configs. Overridden values in config will affect itself and its descendant
 * configs, without affecting its ancestor configs. Loading items in config will not affect its
 * ancestor configs too. [invoke] can be used to create a root config, and [withLayer] can be used
 * to create a child config from specified config.
 *
 * All methods in Config is thread-safe.
 */
interface Config : ItemContainer {
    /**
     * Associate item with specified value without type checking.
     *
     * @param item config item
     * @param value associated value
     */
    fun rawSet(item: Item<*>, value: Any?)

    /**
     * Associate item with specified value.
     *
     * @param item config item
     * @param value associated value
     */
    operator fun <T> set(item: Item<T>, value: T)

    /**
     * Find item with specified name, and associate it with specified value.
     *
     * @param name item name
     * @param value associated value
     */
    operator fun <T> set(name: String, value: T)

    /**
     * Associate item with specified thunk, which can be used to evaluate value for the item.
     *
     * @param item config item
     * @param thunk thunk used to evaluate value for the item
     */
    fun <T> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T)

    /**
     * Find item with specified name, and associate item with specified thunk,
     * which can be used to evaluate value for the item.
     *
     * @param name item name
     * @param thunk thunk used to evaluate value for the item
     */
    fun <T> lazySet(name: String, thunk: (config: ItemContainer) -> T)

    /**
     * Discard associated value of specified item.
     *
     * @param item config item
     */
    fun unset(item: Item<*>)

    /**
     * Discard associated value of item with specified name.
     *
     * @param name item name
     */
    fun unset(name: String)

    /**
     * Remove all values from the facade layer of this config.
     */
    fun clear()

    /**
     * Whether all required items have values or not.
     *
     * @return `true` if all required items have values, `false` otherwise
     */
    fun containsRequired(): Boolean

    /**
     * Validate whether all required items have values or not. If not, throws [UnsetValueException].
     *
     * @return the current config
     */
    fun validateRequired(): Config

    /**
     * Returns a property that can read/set associated value for specified item.
     *
     * @param item config item
     * @return a property that can read/set associated value for specified item
     */
    fun <T> property(item: Item<T>): ReadWriteProperty<Any?, T>

    /**
     * Returns a property that can read/set associated value for item with specified name.
     *
     * @param name item name
     * @return a property that can read/set associated value for item with specified name
     */
    fun <T> property(name: String): ReadWriteProperty<Any?, T>

    /**
     * Name of facade layer of config.
     *
     * Layer name provides information for facade layer in a cascading config.
     */
    val name: String

    /**
     * Returns parent of this config, or `null` if this config is a root config.
     */
    val parent: Config?

    /**
     * List of config specs from all layers of this config.
     */
    val specs: List<Spec>

    /**
     * List of sources from all layers of this config.
     */
    val sources: Deque<Source>

    /**
     * Returns a config overlapped by the specified facade config.
     *
     * All operations will be applied to the facade config first,
     * and then fall back to this config when necessary.
     *
     * @param config the facade config
     * @return a config overlapped by the specified facade config
     */
    operator fun plus(config: Config): Config

    /**
     * Returns a config backing by the specified fallback config.
     *
     * All operations will be applied to this config first,
     * and then fall back to the fallback config when necessary.
     *
     * @param config the fallback config
     * @return a config backing by the specified fallback config
     */
    fun withFallback(config: Config): Config

    /**
     * Returns sub-config in the specified path.
     *
     * @param path the specified path
     * @return sub-config in the specified path
     */
    fun at(path: String): Config

    /**
     * Returns config with the specified additional prefix.
     *
     * @param prefix additional prefix
     * @return config with the specified additional prefix
     */
    fun withPrefix(prefix: String): Config

    /**
     * Load item into facade layer with the specified prefix.
     *
     * Same item cannot be added twice.
     * The item cannot have same qualified name with existed items in config.
     *
     * @param item config item
     * @param prefix prefix for the config item
     */
    fun addItem(item: Item<*>, prefix: String = "")

    /**
     * Load items in specified config spec into facade layer.
     *
     * Same config spec cannot be added twice.
     * All items in specified config spec cannot have same qualified name with existed items in config.
     *
     * @param spec config spec
     */
    fun addSpec(spec: Spec)

    /**
     * Executes the given [action] after locking the facade layer of this config.
     *
     * @param action the given action
     * @return the return value of the action.
     */
    fun <T> lock(action: () -> T): T

    /**
     * Returns a child config of this config with specified name.
     *
     * @param name name of facade layer in child config
     * @return a child config
     */
    fun withLayer(name: String = ""): Config

    /**
     * Returns a child config containing values from specified source.
     *
     * Values from specified source will be loaded into facade layer of the returned child config
     * without affecting this config.
     *
     * @param source config source
     * @return a child config containing value from specified source
     */
    fun withSource(source: Source): Config

    /**
     * Returns a child config containing values loaded by specified trigger.
     *
     * Values loaded by specified trigger will be loaded into facade layer of
     * the returned child config without affecting this config.
     *
     * @param description trigger description
     * @param trigger load trigger
     * @return a child config containing value loaded by specified trigger
     */
    fun withLoadTrigger(
        description: String,
        trigger: (
            config: Config,
            load: (source: Source) -> Unit
        ) -> Unit
    ): Config

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     *
     * @return default loaders for this config
     */
    @JavaApi
    fun from(): DefaultLoaders = from

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     */
    val from: DefaultLoaders get() = DefaultLoaders(this)

    /**
     * Returns [ObjectMapper] using to map from source to value in config.
     */
    val mapper: ObjectMapper

    /**
     * Returns a map in key-value format for this config.
     *
     * The returned map contains all items in this config, with item name as key and
     * associated value as value.
     * This map can be loaded into config as [com.uchuhimo.konf.source.base.KVSource] using
     * `config.from.map.kv(map)`.
     */
    fun toMap(): Map<String, Any>

    /**
     * Convert this config to a tree node.
     *
     * @return a tree node
     */
    fun toTree(): TreeNode {
        return toMap().kvToTree()
    }

    /**
     * Enables the specified feature and returns this config.
     *
     * @param feature the specified feature
     * @return this config
     */
    fun enable(feature: Feature): Config

    /**
     * Disables the specified feature and returns this config.
     *
     * @param feature the specified feature
     * @return this config
     */
    fun disable(feature: Feature): Config

    /**
     * Check whether the specified feature is enabled or not.
     *
     * @param feature the specified feature
     * @return whether the specified feature is enabled or not
     */
    fun isEnabled(feature: Feature): Boolean

    companion object {
        /**
         * Create a new root config.
         *
         * @return a new root config
         */
        operator fun invoke(): Config = BaseConfig()

        /**
         * Create a new root config and initiate it.
         *
         * @param init initial action
         * @return a new root config
         */
        operator fun invoke(init: Config.() -> Unit): Config = Config().apply(init)
    }
}

/**
 * Returns a property that can read/set associated value casted from config.
 *
 * @return a property that can read/set associated value casted from config
 */
inline fun <reified T> Config.cast() =
    object : RequiredConfigProperty<T>(this.withPrefix("root").withLayer(), name = "root") {}

/**
 * Returns a value casted from config.
 *
 * @return a value casted from config
 */
inline fun <reified T> Config.toValue(): T {
    val value by cast<T>()
    return value
}

/**
 * Returns a property that can read/set associated value for specified required item.
 *
 * @param prefix prefix for the config item
 * @param name item name without prefix
 * @param description description for this item
 * @return a property that can read/set associated value for specified required item
 */
inline fun <reified T> Config.required(
    prefix: String = "",
    name: String? = null,
    description: String = ""
) =
    object : RequiredConfigProperty<T>(this, prefix, name, description, null is T) {}

open class RequiredConfigProperty<T>(
    private val config: Config,
    private val prefix: String = "",
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(RequiredConfigProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadWriteProperty<Any?, T> {
        val item = object : RequiredItem<T>(Spec.dummy, name
            ?: property.name, description, type, nullable) {}
        config.addItem(item, prefix)
        return config.property(item)
    }
}

/**
 * Returns a property that can read/set associated value for specified optional item.
 *
 * @param default default value returned before associating this item with specified value
 * @param prefix prefix for the config item
 * @param name item name without prefix
 * @param description description for this item
 * @return a property that can read/set associated value for specified optional item
 */
inline fun <reified T> Config.optional(
    default: T,
    prefix: String = "",
    name: String? = null,
    description: String = ""
) =
    object : OptionalConfigProperty<T>(this, default, prefix, name, description, null is T) {}

open class OptionalConfigProperty<T>(
    private val config: Config,
    private val default: T,
    private val prefix: String = "",
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(OptionalConfigProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadWriteProperty<Any?, T> {
        val item = object : OptionalItem<T>(Spec.dummy, name
            ?: property.name, default, description, type, nullable) {}
        config.addItem(item, prefix)
        return config.property(item)
    }
}

/**
 * Returns a property that can read/set associated value for specified lazy item.
 *
 * @param prefix prefix for the config item
 * @param name item name without prefix
 * @param description description for this item
 * @param thunk thunk used to evaluate value for this item
 * @return a property that can read/set associated value for specified lazy item
 */
inline fun <reified T> Config.lazy(
    prefix: String = "",
    name: String? = null,
    description: String = "",
    noinline thunk: (config: ItemContainer) -> T
) =
    object : LazyConfigProperty<T>(this, thunk, prefix, name, description, null is T) {}

open class LazyConfigProperty<T>(
    private val config: Config,
    private val thunk: (config: ItemContainer) -> T,
    private val prefix: String = "",
    private val name: String? = null,
    private val description: String = "",
    private val nullable: Boolean = false
) {
    @Suppress("LeakingThis")
    private val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
        .findSuperType(LazyConfigProperty::class.java).bindings.typeParameters[0]

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>):
        ReadWriteProperty<Any?, T> {
        val item = object : LazyItem<T>(Spec.dummy, name
            ?: property.name, thunk, description, type, nullable) {}
        config.addItem(item, prefix)
        return config.property(item)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated(message = "Use method in Config.", replaceWith = ReplaceWith("toTree()"))
fun Config.toTree(): TreeNode {
    return toTree()
}
