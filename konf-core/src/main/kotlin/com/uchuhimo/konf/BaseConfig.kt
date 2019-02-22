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
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.uchuhimo.collections.mutableBiMapOf
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.deserializer.DurationDeserializer
import com.uchuhimo.konf.source.deserializer.OffsetDateTimeDeserializer
import com.uchuhimo.konf.source.deserializer.StringDeserializer
import com.uchuhimo.konf.source.deserializer.ZoneDateTimeDeserializer
import com.uchuhimo.konf.source.load
import com.uchuhimo.konf.source.loadItem
import com.uchuhimo.konf.source.toCompatibleValue
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The default implementation for [Config].
 */
open class BaseConfig(
    override val name: String = "",
    override val parent: BaseConfig? = null,
    override val mapper: ObjectMapper = createDefaultMapper()
) : Config {
    protected val specsInLayer = mutableListOf<Spec>()
    protected val sourcesInLayer = ArrayDeque<Source>()
    protected val valueByItem = mutableMapOf<Item<*>, ValueState>()
    protected val nameByItem = mutableBiMapOf<Item<*>, String>()
    protected val featuresInLayer = mutableMapOf<Feature, Boolean>()

    private var hasChildren = false

    protected val lock = ReentrantReadWriteLock()

    override fun <T> lock(action: () -> T): T = lock.write(action)

    override fun at(path: String): Config = DrillDownConfig(path, this)

    override fun withPrefix(prefix: String): Config = RollUpConfig(prefix, this)

    override fun iterator(): Iterator<Item<*>> = object : Iterator<Item<*>> {
        private var currentConfig = this@BaseConfig
        private var current = currentConfig.nameByItem.keys.iterator()

        override tailrec fun hasNext(): Boolean {
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

    override val itemWithNames: List<Pair<Item<*>, String>>
        get() = lock.read { nameByItem.entries }.map { it.toPair() } +
            (parent?.itemWithNames ?: listOf())

    override fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val config = this@BaseConfig
            for ((item, name) in config.itemWithNames) {
                try {
                    put(name, config.getOrNull(item, errorWhenNotFound = true).toCompatibleValue(mapper))
                } catch (_: UnsetValueException) {
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(item: Item<T>): T = getOrNull(item, errorWhenNotFound = true) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String): T = getOrNull(name, errorWhenNotFound = true) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrNull(item: Item<T>): T? = getOrNull(item, errorWhenNotFound = false) as T?

    private fun getOrNull(
        item: Item<*>,
        errorWhenNotFound: Boolean,
        lazyContext: ItemContainer = this
    ): Any? {
        val valueState = lock.read { valueByItem[item] }
        if (valueState != null) {
            @Suppress("UNCHECKED_CAST")
            return when (valueState) {
                is ValueState.Unset ->
                    if (errorWhenNotFound) {
                        throw UnsetValueException(item)
                    } else {
                        null
                    }
                is ValueState.Null -> null
                is ValueState.Value -> valueState.value
                is ValueState.Lazy<*> -> {
                    val value = try {
                        valueState.thunk(lazyContext)
                    } catch (exception: ConfigException) {
                        when (exception) {
                            is UnsetValueException, is NoSuchItemException -> {
                                if (errorWhenNotFound) {
                                    throw exception
                                } else {
                                    return null
                                }
                            }
                            else -> throw exception
                        }
                    }
                    if (value == null) {
                        if (item.nullable) {
                            null
                        } else {
                            throw InvalidLazySetException(
                                "fail to cast null to ${item.type.rawClass}" +
                                    " when getting item ${item.name} in config")
                        }
                    } else {
                        if (item.type.rawClass.isInstance(value)) {
                            value
                        } else {
                            throw InvalidLazySetException(
                                "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                                    " when getting item ${item.name} in config")
                        }
                    }
                }
            }
        } else {
            return if (parent != null) {
                parent!!.getOrNull(item, errorWhenNotFound, lazyContext)
            } else {
                if (errorWhenNotFound) {
                    throw NoSuchItemException(item)
                } else {
                    null
                }
            }
        }
    }

    open fun getItemOrNull(name: String): Item<*>? {
        val item = getItemInLayerOrNull(name)
        return item ?: parent?.getItemOrNull(name)
    }

    protected fun getItemInLayerOrNull(name: String) = lock.read { nameByItem.inverse[name] }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrNull(name: String): T? = getOrNull(name, errorWhenNotFound = false) as T?

    private fun getOrNull(name: String, errorWhenNotFound: Boolean): Any? {
        val item = getItemOrNull(name)
        return if (item != null) {
            getOrNull(item, errorWhenNotFound)
        } else {
            if (errorWhenNotFound) {
                throw NoSuchItemException(name)
            } else {
                null
            }
        }
    }

    protected fun containsInLayer(item: Item<*>) = lock.read { valueByItem.containsKey(item) }

    override fun contains(item: Item<*>): Boolean {
        return if (containsInLayer(item)) {
            true
        } else {
            parent?.contains(item) ?: false
        }
    }

    protected fun containsInLayer(name: String) = lock.read { nameByItem.containsValue(name) }

    override fun contains(name: String): Boolean {
        return if (containsInLayer(name)) {
            true
        } else {
            parent?.contains(name) ?: false
        }
    }

    protected fun containsInLayer(path: Path): Boolean {
        nameByItem.values.forEach {
            val itemName = it.toPath()
            if ((path.size >= itemName.size && path.subList(0, itemName.size) == itemName) ||
                (path.size < itemName.size && itemName.subList(0, path.size) == path)) {
                return true
            }
        }
        return false
    }

    override fun contains(path: Path): Boolean =
        containsInLayer(path) || (parent?.contains(path) ?: false)

    override fun nameOf(item: Item<*>): String {
        val name = lock.read { nameByItem[item] }
        return name ?: parent?.nameOf(item) ?: throw NoSuchItemException(item)
    }

    override fun rawSet(item: Item<*>, value: Any?) {
        if (item in this) {
            if (value == null) {
                if (item.nullable) {
                    lock.write {
                        valueByItem[item] = ValueState.Null
                    }
                } else {
                    throw ClassCastException(
                        "fail to cast null to ${item.type.rawClass}" +
                            " when setting item ${item.name} in config")
                }
            } else {
                if (item.type.rawClass.isInstance(value)) {
                    lock.write {
                        val valueState = valueByItem[item]
                        if (valueState is ValueState.Value) {
                            valueState.value = value
                        } else {
                            valueByItem[item] = ValueState.Value(value)
                        }
                    }
                } else {
                    throw ClassCastException(
                        "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                            " when setting item ${item.name} in config")
                }
            }
        } else {
            throw NoSuchItemException(item)
        }
    }

    override fun <T> set(item: Item<T>, value: T) {
        rawSet(item, value)
    }

    override fun <T> set(name: String, value: T) {
        val item = getItemOrNull(name)
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            set(item as Item<T>, value)
        } else {
            throw NoSuchItemException(name)
        }
    }

    override fun <T> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T) {
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
            throw NoSuchItemException(item)
        }
    }

    override fun <T> lazySet(name: String, thunk: (config: ItemContainer) -> T) {
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
            throw NoSuchItemException(item)
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

    override fun clear() {
        lock.write { valueByItem.clear() }
    }

    override fun <T> property(item: Item<T>): ReadWriteProperty<Any?, T> {
        if (!contains(item)) {
            throw NoSuchItemException(item)
        }
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(item)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                set(item, value)
        }
    }

    override fun <T> property(name: String): ReadWriteProperty<Any?, T> {
        if (!contains(name)) {
            throw NoSuchItemException(name)
        }
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(name)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                set(name, value)
        }
    }

    override val specs: List<Spec> get() = lock.read { specsInLayer + (parent?.specs ?: listOf()) }

    override val sources: Deque<Source>
        get() {
            return lock.read { sourcesInLayer.clone() }.apply {
                for (source in parent?.sources ?: ArrayDeque()) {
                    addLast(source)
                }
            }
        }

    override fun enable(feature: Feature): Config {
        return apply {
            lock {
                featuresInLayer[feature] = true
            }
        }
    }

    override fun disable(feature: Feature): Config {
        return apply {
            lock {
                featuresInLayer[feature] = false
            }
        }
    }

    override fun isEnabled(feature: Feature): Boolean {
        return lock {
            featuresInLayer[feature] ?: parent?.isEnabled(feature) ?: feature.enabledByDefault
        }
    }

    @Suppress("LeakingThis")
    override val layer: Config = Layer(this)

    override fun addItem(item: Item<*>, prefix: String) {
        lock.write {
            if (hasChildren) {
                throw LayerFrozenException(this)
            }
            val path = prefix.toPath() + item.name.toPath()
            val name = path.name
            if (item !in this) {
                if (path in this) {
                    throw NameConflictException("item $name cannot be added")
                }
                nameByItem[item] = name
                valueByItem[item] = when (item) {
                    is OptionalItem -> {
                        if (item.default == null) {
                            ValueState.Null
                        } else {
                            ValueState.Value(item.default)
                        }
                    }
                    is RequiredItem -> ValueState.Unset
                    is LazyItem -> ValueState.Lazy(item.thunk)
                }
                sources.firstOrNull { loadItem(item, path, it) }
            } else {
                throw RepeatedItemException(name)
            }
        }
    }

    override fun addSpec(spec: Spec) {
        lock.write {
            if (hasChildren) {
                throw LayerFrozenException(this)
            }
            val sources = this.sources
            spec.items.forEach { item ->
                val name = spec.qualify(item)
                if (item !in this) {
                    val path = name.toPath()
                    if (path in this) {
                        throw NameConflictException("item $name cannot be added")
                    }
                    nameByItem[item] = name
                    valueByItem[item] = when (item) {
                        is OptionalItem -> {
                            if (item.default == null) {
                                ValueState.Null
                            } else {
                                ValueState.Value(item.default)
                            }
                        }
                        is RequiredItem -> ValueState.Unset
                        is LazyItem -> ValueState.Lazy(item.thunk)
                    }
                    sources.firstOrNull { loadItem(item, path, it) }
                } else {
                    throw RepeatedItemException(name)
                }
            }
            spec.innerSpecs.forEach { innerSpec ->
                addSpec(innerSpec.withPrefix(spec.prefix))
            }
            specsInLayer += spec
        }
    }

    override fun addSource(source: Source) {
        lock.write {
            load(this, source)
            sourcesInLayer.push(source)
        }
    }

    override fun withLayer(name: String): Config {
        lock.write { hasChildren = true }
        return BaseConfig(name, this, mapper)
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

    protected sealed class ValueState {
        object Unset : ValueState()
        object Null : ValueState()
        data class Lazy<T>(var thunk: (config: ItemContainer) -> T) : ValueState()
        data class Value(var value: Any) : ValueState()
    }

    private class Layer(val config: BaseConfig) : Config by config {
        override val parent: Config? = null
        override val specs: List<Spec> = config.specsInLayer
        override val layer: Config = this

        override fun toMap(): Map<String, Any> {
            return mutableMapOf<String, Any>().apply {
                for (item in config.lock.read { config.valueByItem.keys }) {
                    try {
                        put(nameOf(item), config.getOrNull(item, errorWhenNotFound = true).toCompatibleValue(mapper))
                    } catch (_: UnsetValueException) {
                    }
                }
            }
        }

        override fun <T> get(item: Item<T>): T =
            if (contains(item)) config[item] else throw NoSuchItemException(item)

        override fun <T> get(name: String): T =
            if (contains(name)) config[name] else throw NoSuchItemException(name)

        override fun <T> getOrNull(item: Item<T>): T? =
            if (contains(item)) config.getOrNull(item) else null

        override fun <T> getOrNull(name: String): T? =
            if (contains(name)) config.getOrNull(name) else null

        override fun <T> invoke(name: String): T = super.invoke(name)

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

/**
 * Returns a new default object mapper for config.
 */
fun createDefaultMapper(): ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
    .registerModules(
        SimpleModule()
            .addDeserializer(String::class.java, StringDeserializer),
        JavaTimeModule()
            .addDeserializer(Duration::class.java, DurationDeserializer)
            .addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer)
            .addDeserializer(ZonedDateTime::class.java, ZoneDateTimeDeserializer))
