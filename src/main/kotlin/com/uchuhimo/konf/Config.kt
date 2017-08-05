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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Config : ItemContainer {
    fun rawSet(item: Item<*>, value: Any)
    operator fun <T : Any> set(item: Item<T>, value: T)
    operator fun <T : Any> set(name: String, value: T)
    fun <T : Any> lazySet(item: Item<T>, lazyThunk: (config: ItemContainer) -> T)
    fun <T : Any> lazySet(name: String, lazyThunk: (config: ItemContainer) -> T)
    fun unset(item: Item<*>)
    fun unset(name: String)

    fun <T : Any> property(item: Item<T>): ReadWriteProperty<Any?, T>
    fun <T : Any> property(name: String): ReadWriteProperty<Any?, T>

    val name: String
    val parent: Config?

    val specs: List<ConfigSpec>
    fun addSpec(spec: ConfigSpec)
    fun withLayer(name: String = ""): Config

    fun withSource(source: Source): Config = loadFromSource(source)

    @JavaApi
    fun withSourceFrom(): DefaultLoaders = withSourceFrom

    val withSourceFrom: DefaultLoaders get() = DefaultLoaders(this)

    val toTree: ConfigTree

    val mapper: ObjectMapper

    companion object {
        operator fun invoke(): Config = ConfigImpl()

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
            if (current.hasNext()) {
                return true
            } else {
                val parent = currentConfig.parent
                if (parent != null) {
                    currentConfig = parent
                    current = currentConfig.nameByItem.keys.iterator()
                    return hasNext()
                } else {
                    return false
                }
            }
        }

        override fun next(): Item<*> = current.next()
    }

    override val toTree: ConfigTree get() = tree.deepCopy()

    override fun <T : Any> get(item: Item<T>): T = getOrNull(item, errorWhenUnset = true) ?:
            throw NoSuchItemException(item.name)

    override fun <T : Any> get(name: String): T = getOrNull<T>(name, errorWhenUnset = true) ?:
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
                    val value = valueState.thunk(lazyContext)!!
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
            if (parent != null) {
                return parent.getOrNull(item, errorWhenUnset, lazyContext)
            } else {
                return null
            }
        }
    }

    private fun getItemOrNull(name: String): Item<*>? {
        val item = lock.read { nameByItem.inverse[name] }
        if (item != null) {
            return item
        } else {
            if (parent != null) {
                return parent.getItemOrNull(name)
            } else {
                return null
            }
        }
    }

    override fun <T : Any> getOrNull(name: String): T? = getOrNull(name, errorWhenUnset = false)

    private fun <T : Any> getOrNull(name: String, errorWhenUnset: Boolean): T? {
        val item = getItemOrNull(name) ?: return null
        @Suppress("UNCHECKED_CAST")
        return getOrNull(item as Item<T>, errorWhenUnset)
    }

    override fun contains(item: Item<*>): Boolean {
        if (lock.read { valueByItem.containsKey(item) }) {
            return true
        } else {
            if (parent != null) {
                return parent.contains(item)
            } else {
                return false
            }
        }
    }

    override fun contains(name: String): Boolean {
        if (lock.read { nameByItem.containsValue(name) }) {
            return true
        } else {
            if (parent != null) {
                return parent.contains(name)
            } else {
                return false
            }
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

    override fun <T : Any> lazySet(item: Item<T>, lazyThunk: (config: ItemContainer) -> T) {
        if (item in this) {
            lock.write {
                val valueState = valueByItem[item]
                if (valueState is ValueState.Lazy<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (valueState as ValueState.Lazy<T>).thunk = lazyThunk
                } else {
                    valueByItem[item] = ValueState.Lazy(lazyThunk)
                }
            }
        } else {
            throw NoSuchItemException(item.name)
        }
    }

    override fun <T : Any> lazySet(name: String, lazyThunk: (config: ItemContainer) -> T) {
        val item = getItemOrNull(name)
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            lazySet(item as Item<T>, lazyThunk)
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

    override val specs: List<ConfigSpec> get() = mutableListOf<ConfigSpec>().apply {
        addAll(object : Iterator<ConfigSpec> {
            private var currentConfig = this@ConfigImpl
            private var current = currentConfig.specsInLayer.iterator()

            tailrec override fun hasNext(): Boolean {
                if (current.hasNext()) {
                    return true
                } else {
                    val parent = currentConfig.parent
                    if (parent != null) {
                        currentConfig = parent
                        current = currentConfig.specsInLayer.iterator()
                        return hasNext()
                    } else {
                        return false
                    }
                }
            }

            override fun next(): ConfigSpec = current.next()
        }.asSequence())
    }

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

    private sealed class ValueState {
        object Unset : ValueState()
        data class Lazy<T>(var thunk: (config: ItemContainer) -> T) : ValueState()
        data class Value(var value: Any) : ValueState()
    }
}
