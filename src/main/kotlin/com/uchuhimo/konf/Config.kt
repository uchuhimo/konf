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

import com.fasterxml.jackson.databind.ObjectMapper
import com.uchuhimo.collections.mutableBiMapOf
import com.uchuhimo.konf.annotation.JavaApi
import com.uchuhimo.konf.source.DefaultLoaders
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.createDefaultMapper
import com.uchuhimo.konf.source.loadFromSource
import com.uchuhimo.konf.source.toCompatibleValue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Config containing items and associated values.
 *
 * Config contains items, which can be loaded with [addSpec].
 * Config contains values, each of which is associated with corresponding item.
 * Values can be loaded from [source][Source] with [withSource] or [withSourceFrom].
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
    val specs: List<ConfigSpec>

    /**
     * Facade layer of config.
     */
    val layer: Config

    /**
     * Load items in specified config spec into facade layer.
     *
     * Same config spec cannot be loaded twice.
     * All items in specified config spec cannot have same name with existed items in config.
     *
     * @param spec config spec
     */
    fun addSpec(spec: ConfigSpec)

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
    fun withSource(source: Source): Config = loadFromSource(source)

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     *
     * @return default loaders for this config
     */
    @JavaApi
    fun withSourceFrom(): DefaultLoaders = withSourceFrom

    /**
     * Returns default loaders for this config.
     *
     * It is a fluent API for loading source from default loaders.
     */
    val withSourceFrom: DefaultLoaders get() = DefaultLoaders(this)

    /**
     * Returns [ObjectMapper] using to map from source to value in config.
     */
    val mapper: ObjectMapper

    /**
     * Returns config tree for this config.
     */
    fun toTree(): ConfigTree

    /**
     * Returns a map in key-value format for this config.
     *
     * The returned map contains all items in this config, with item name as key and
     * associated value as value.
     * This map can be loaded into config as [com.uchuhimo.konf.source.base.KVSource] using
     * `config.withSourceFrom.map.kv(map)`.
     */
    fun toMap(): Map<String, Any>

    /**
     * Returns a map in key-value format for facade layer.
     *
     * The returned map contains all items in facade layer, with item name as key and
     * associated value as value.
     * This map can be loaded into config as [com.uchuhimo.konf.source.base.KVSource] using
     * `config.withSourceFrom.map.kv(map)`.
     */
    fun layerToMap(): Map<String, Any>

    companion object {
        /**
         * Create a new root config.
         *
         * @return a new root config
         */
        operator fun invoke(): Config = ConfigImpl()

        /**
         * Create a new root config and initiate it.
         *
         * @param init initial action
         * @return a new root config
         */
        operator fun invoke(init: Config.() -> Unit): Config = Config().apply(init)
    }
}

private class ConfigImpl constructor(
        override val name: String = "",
        override val parent: ConfigImpl? = null,
        override val mapper: ObjectMapper = createDefaultMapper()
) : Config {
    private val specsInLayer = mutableListOf<ConfigSpec>()
    private val valueByItem = mutableMapOf<Item<*>, ValueState>()
    private val nameByItem = mutableBiMapOf<Item<*>, String>()
    private val tree: ConfigTree = run {
        if (parent != null) {
            parent.tree.deepCopy()
        } else {
            ConfigPathNode(path = emptyList(), children = mutableListOf())
        }
    }
    private var hasChildren = false

    private val lock = ReentrantReadWriteLock()

    override fun iterator(): Iterator<Item<*>> = object : Iterator<Item<*>> {
        private var currentConfig = this@ConfigImpl
        private var current = currentConfig.nameByItem.keys.iterator()

        tailrec override fun hasNext(): Boolean {
            return if (current.hasNext()) {
                true
            } else {
                val parent = currentConfig.parent
                if (parent != null) {
                    currentConfig = parent
                    current = currentConfig.nameByItem.keys.iterator()
                    hasNext()
                } else {
                    false
                }
            }
        }

        override fun next(): Item<*> = current.next()
    }

    override fun toTree(): ConfigTree = tree.deepCopy()

    override fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val config = this@ConfigImpl
            for (item in config) {
                if (config.getOrNull(item) != null) {
                    put(item.name, config[item].toCompatibleValue(mapper))
                }
            }
        }
    }

    override fun layerToMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val config = this@ConfigImpl
            for (item in config.valueByItem.keys) {
                if (config.getOrNull(item) != null) {
                    put(item.name, config[item].toCompatibleValue(mapper))
                }
            }
        }
    }

    override fun <T : Any> get(item: Item<T>): T = getOrNull(item, errorWhenUnset = true) ?:
            throw NoSuchItemException(item.name)

    override fun <T : Any> get(name: String): T = getOrNull(name, errorWhenUnset = true) ?:
            throw NoSuchItemException(name)

    override fun <T : Any> getOrNull(item: Item<T>): T? =
            getOrNull(item, errorWhenUnset = false)

    private fun <T : Any> getOrNull(
            item: Item<T>,
            errorWhenUnset: Boolean,
            lazyContext: ItemContainer = this
    ): T? {
        val valueState = lock.read { valueByItem[item] }
        if (valueState != null) {
            @Suppress("UNCHECKED_CAST")
            return when (valueState) {
                is ValueState.Unset ->
                    if (errorWhenUnset) {
                        throw UnsetValueException(item.name)
                    } else {
                        return null
                    }
                is ValueState.Value -> valueState.value as T
                is ValueState.Lazy<*> -> {
                    val value = try {
                        valueState.thunk(lazyContext)
                    } catch (exception: UnsetValueException) {
                        if (errorWhenUnset) {
                            throw exception
                        } else {
                            return null
                        }
                    }
                    if (item.type.rawClass.isInstance(value)) {
                        value as T
                    } else {
                        throw InvalidLazySetException(
                                "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                                        " when getting ${item.name} in config")
                    }
                }
            }
        } else {
            return parent?.getOrNull(item, errorWhenUnset, lazyContext)
        }
    }

    private fun getItemOrNull(name: String): Item<*>? {
        val item = lock.read { nameByItem.inverse[name] }
        return item ?: parent?.getItemOrNull(name)
    }

    override fun <T : Any> getOrNull(name: String): T? = getOrNull(name, errorWhenUnset = false)

    private fun <T : Any> getOrNull(name: String, errorWhenUnset: Boolean): T? {
        val item = getItemOrNull(name) ?: return null
        @Suppress("UNCHECKED_CAST")
        return getOrNull(item as Item<T>, errorWhenUnset)
    }

    override fun contains(item: Item<*>): Boolean {
        return if (lock.read { valueByItem.containsKey(item) }) {
            true
        } else {
            parent?.contains(item) ?: false
        }
    }

    override fun contains(name: String): Boolean {
        return if (lock.read { nameByItem.containsValue(name) }) {
            true
        } else {
            parent?.contains(name) ?: false
        }
    }

    override fun rawSet(item: Item<*>, value: Any) {
        if (item.type.rawClass.isInstance(value)) {
            if (item in this) {
                lock.write {
                    val valueState = valueByItem[item]
                    if (valueState is ValueState.Value) {
                        valueState.value = value
                    } else {
                        valueByItem[item] = ValueState.Value(value)
                    }
                }
            } else {
                throw NoSuchItemException(item.name)
            }
        } else {
            throw ClassCastException(
                    "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                            " when setting ${item.name} in config")
        }
    }

    override fun <T : Any> set(item: Item<T>, value: T) {
        rawSet(item, value)
    }

    override fun <T : Any> set(name: String, value: T) {
        val item = getItemOrNull(name)
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            set(item as Item<T>, value)
        } else {
            throw NoSuchItemException(name)
        }
    }

    override fun <T : Any> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T) {
        if (item in this) {
            lock.write {
                val valueState = valueByItem[item]
                if (valueState is ValueState.Lazy<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (valueState as ValueState.Lazy<T>).thunk = thunk
                } else {
                    valueByItem[item] = ValueState.Lazy(thunk)
                }
            }
        } else {
            throw NoSuchItemException(item.name)
        }
    }

    override fun <T : Any> lazySet(name: String, thunk: (config: ItemContainer) -> T) {
        val item = getItemOrNull(name)
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            lazySet(item as Item<T>, thunk)
        } else {
            throw NoSuchItemException(name)
        }
    }

    override fun unset(item: Item<*>) {
        if (item in this) {
            lock.write { valueByItem[item] = ValueState.Unset }
        } else {
            throw NoSuchItemException(item.name)
        }
    }

    override fun unset(name: String) {
        val item = getItemOrNull(name)
        if (item != null) {
            unset(item)
        } else {
            throw NoSuchItemException(name)
        }
    }

    override fun <T : Any> property(item: Item<T>): ReadWriteProperty<Any?, T> {
        if (!contains(item)) {
            throw NoSuchItemException(item.name)
        }
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(item)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                    set(item, value)
        }
    }

    override fun <T : Any> property(name: String): ReadWriteProperty<Any?, T> {
        if (!contains(name)) {
            throw NoSuchItemException(name)
        }
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(name)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                    set(name, value)
        }
    }

    private tailrec fun addNode(tree: ConfigTree, path: List<String>, item: Item<*>) {
        when (tree) {
            is ConfigPathNode -> {
                if (path.isEmpty()) {
                    throw NameConflictException("${item.name} cannot be added" +
                            " since the following items has been added to config:" +
                            " ${tree.items.joinToString { it.name }}")
                }
                val matchChild = tree.children.find { it.path.last() == path[0] }
                if (matchChild != null) {
                    addNode(matchChild, path.drop(1), item)
                } else {
                    if (path.size == 1) {
                        tree.children += ConfigItemNode(tree.path + path[0], item)
                    } else {
                        val child = ConfigPathNode(tree.path + path[0], mutableListOf())
                        tree.children += child
                        addNode(child, path.drop(1), item)
                    }
                }
            }
            is ConfigItemNode<*> -> {
                throw NameConflictException("${item.name} cannot be added" +
                        " since item ${tree.item.name} has been added to config")
            }
        }
    }

    override val specs: List<ConfigSpec>
        get() = mutableListOf<ConfigSpec>().apply {
            addAll(object : Iterator<ConfigSpec> {
                private var currentConfig = this@ConfigImpl
                private var current = currentConfig.specsInLayer.iterator()

                tailrec override fun hasNext(): Boolean {
                    return if (current.hasNext()) {
                        true
                    } else {
                        val parent = currentConfig.parent
                        if (parent != null) {
                            currentConfig = parent
                            current = currentConfig.specsInLayer.iterator()
                            hasNext()
                        } else {
                            false
                        }
                    }
                }

                override fun next(): ConfigSpec = current.next()
            }.asSequence())
        }

    override val layer: Config = Layer(this)

    override fun addSpec(spec: ConfigSpec) {
        lock.write {
            if (hasChildren) {
                throw SpecFrozenException(this)
            }
            spec.items.forEach { item ->
                val name = item.name
                if (item !in this) {
                    if (name !in this) {
                        addNode(tree, item.path, item)
                        nameByItem[item] = name
                        valueByItem[item] = when (item) {
                            is OptionalItem -> ValueState.Value(item.default)
                            is RequiredItem -> ValueState.Unset
                            is LazyItem -> ValueState.Lazy(item.thunk)
                        }
                    } else {
                        throw NameConflictException("item $name has been added")
                    }
                } else {
                    throw RepeatedItemException(name)
                }
            }
            specsInLayer += spec
        }
    }

    override fun withLayer(name: String): Config {
        lock.write { hasChildren = true }
        return ConfigImpl(name, this, mapper)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Config) return false
        return toMap() == other.toMap()
    }

    override fun hashCode(): Int {
        return toMap().hashCode()
    }

    override fun toString(): String {
        return "Config(items=${toMap()})"
    }

    private sealed class ValueState {
        object Unset : ValueState()
        data class Lazy<T : Any>(var thunk: (config: ItemContainer) -> T) : ValueState()
        data class Value(var value: Any) : ValueState()
    }

    private class Layer(val config: ConfigImpl) : Config by config {
        override val parent: Config? = null
        override val specs: List<ConfigSpec> = config.specsInLayer
        override val layer: Config = this

        override fun toMap(): Map<String, Any> {
            return mutableMapOf<String, Any>().apply {
                for (item in config.valueByItem.keys) {
                    if (config.getOrNull(item) != null) {
                        put(item.name, config[item].toCompatibleValue(mapper))
                    }
                }
            }
        }

        override fun <T : Any> get(item: Item<T>): T =
                if (contains(item)) config[item] else throw NoSuchItemException(item.name)

        override fun <T : Any> get(name: String): T =
                if (contains(name)) config[name] else throw NoSuchItemException(name)

        override fun <T : Any> getOrNull(item: Item<T>): T? =
                if (contains(item)) config.getOrNull(item) else null

        override fun <T : Any> getOrNull(name: String): T? =
                if (contains(name)) config.getOrNull(name) else null

        override fun <T : Any> invoke(name: String): T = super.invoke(name)

        override fun iterator(): Iterator<Item<*>> = config.valueByItem.keys.iterator()

        override fun contains(item: Item<*>): Boolean {
            return config.run { lock.read { valueByItem.containsKey(item) } }
        }

        override fun contains(name: String): Boolean {
            return config.run {
                lock.read {
                    val item = getItemOrNull(name)
                    if (item == null) false else valueByItem.containsKey(item)
                }
            }
        }

        override val items: List<Item<*>> get() = super.items

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Config) return false
            return toMap() == other.toMap()
        }

        override fun hashCode(): Int {
            return toMap().hashCode()
        }

        override fun toString(): String {
            return "Layer(items=${toMap()})"
        }
    }
}
