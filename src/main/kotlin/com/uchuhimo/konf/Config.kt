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

import com.fasterxml.jackson.databind.ObjectMapper
import com.uchuhimo.konf.annotation.JavaApi
import com.uchuhimo.konf.source.DefaultLoaders
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.loadBy
import java.util.Deque
import kotlin.properties.ReadWriteProperty

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
    fun rawSet(item: Item<*>, value: Any)

    /**
     * Associate item with specified value.
     *
     * @param item config item
     * @param value associated value
     */
    operator fun <T : Any> set(item: Item<T>, value: T)

    /**
     * Find item with specified name, and associate it with specified value.
     *
     * @param name item name
     * @param value associated value
     */
    operator fun <T : Any> set(name: String, value: T)

    /**
     * Associate item with specified thunk, which can be used to evaluate value for the item.
     *
     * @param item config item
     * @param thunk thunk used to evaluate value for the item
     */
    fun <T : Any> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T)

    /**
     * Find item with specified name, and associate item with specified thunk,
     * which can be used to evaluate value for the item.
     *
     * @param name item name
     * @param thunk thunk used to evaluate value for the item
     */
    fun <T : Any> lazySet(name: String, thunk: (config: ItemContainer) -> T)

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
     * Returns a property that can read/set associated value for specified item.
     *
     * @param item config item
     * @return a property that can read/set associated value for specified item
     */
    fun <T : Any> property(item: Item<T>): ReadWriteProperty<Any?, T>

    /**
     * Returns a property that can read/set associated value for item with specified name.
     *
     * @param name item name
     * @return a property that can read/set associated value for item with specified name
     */
    fun <T : Any> property(name: String): ReadWriteProperty<Any?, T>

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
     * Facade layer of config.
     */
    val layer: Config

    fun at(path: String): Config

    fun withPrefix(prefix: String): Config

    /**
     * Load items in specified config spec into facade layer.
     *
     * Same config spec cannot be added twice.
     * All items in specified config spec cannot have same name with existed items in config.
     *
     * @param spec config spec
     */
    fun addSpec(spec: Spec)

    /**
     * Load values from specified source into facade layer.
     *
     * @param source config source
     */
    fun addSource(source: Source)

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
    fun withSource(source: Source): Config =
        withLayer("source: ${source.description}").apply { addSource(source) }

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
    ): Config =
        loadBy(description, trigger)

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
     *
     * @return default loaders for this config
     */
    @JavaApi
    @Deprecated("use the shorter API `from` instead", ReplaceWith("from"))
    fun withSourceFrom(): DefaultLoaders = from

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     */
    val from: DefaultLoaders get() = DefaultLoaders(this)

    /**
     * Returns default loaders for this config.
     *
     * Source will be applied the given [transform] function when loaded.
     *
     * It is a fluent API for loading source from default loaders.
     *
     * @param the given transformation function
     */
    fun from(transform: (Source) -> Source): DefaultLoaders = DefaultLoaders(this, transform)

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     */
    @Deprecated("use the shorter API `from` instead", ReplaceWith("from"))
    val withSourceFrom: DefaultLoaders
        get() = from

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
