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
import com.uchuhimo.konf.source.deserializer.DurationDeserializer
import com.uchuhimo.konf.source.deserializer.OffsetDateTimeDeserializer
import com.uchuhimo.konf.source.deserializer.StringDeserializer
import com.uchuhimo.konf.source.deserializer.ZoneDateTimeDeserializer
import com.uchuhimo.konf.source.toCompatibleValue
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class BaseConfig(
    override val name: String = "",
    final override val parent: BaseConfig? = null,
    override val mapper: ObjectMapper = createDefaultMapper()
) : Config {
    protected val specsInLayer = mutableListOf<Spec>()
    protected val valueByItem = mutableMapOf<Item<*>, ValueState>()
    protected val nameByItem = mutableBiMapOf<Item<*>, String>()

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
        get() = nameByItem.entries.map { it.toPair() } + (parent?.itemWithNames ?: listOf())

    override fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val config = this@BaseConfig
            for ((item, name) in config.itemWithNames) {
                if (config.getOrNull(item) != null) {
                    put(name, config[item].toCompatibleValue(mapper))
                }
            }
        }
    }

    override fun <T : Any> get(item: Item<T>): T = getOrNull(item, errorWhenUnset = true)
        ?: throw NoSuchItemException(item)

    override fun <T : Any> get(name: String): T = getOrNull(name, errorWhenUnset = true)
        ?: throw NoSuchItemException(name)

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
                        throw UnsetValueException(item)
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
                                " when getting item ${item.name} in config")
                    }
                }
            }
        } else {
            return parent?.getOrNull(item, errorWhenUnset, lazyContext)
        }
    }

    open fun getItemOrNull(name: String): Item<*>? {
        val item = getItemInLayerOrNull(name)
        return item ?: parent?.getItemOrNull(name)
    }

    protected fun getItemInLayerOrNull(name: String) = lock.read { nameByItem.inverse[name] }

    override fun <T : Any> getOrNull(name: String): T? = getOrNull(name, errorWhenUnset = false)

    private fun <T : Any> getOrNull(name: String, errorWhenUnset: Boolean): T? {
        val item = getItemOrNull(name) ?: return null
        @Suppress("UNCHECKED_CAST")
        return getOrNull(item as Item<T>, errorWhenUnset)
    }

    override fun contains(item: Item<*>): Boolean {
        return if (containsInLayer(item)) {
            true
        } else {
            parent?.contains(item) ?: false
        }
    }

    protected fun containsInLayer(item: Item<*>) = lock.read { valueByItem.containsKey(item) }

    override fun contains(name: String): Boolean {
        return if (containsInLayer(name)) {
            true
        } else {
            parent?.contains(name) ?: false
        }
    }

    protected fun containsInLayer(name: String) = lock.read { nameByItem.containsValue(name) }

    override fun nameOf(item: Item<*>): String {
        val name = lock.read { nameByItem[item] }
        return name ?: parent?.nameOf(item) ?: throw NoSuchItemException(item)
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
                throw NoSuchItemException(item)
            }
        } else {
            throw ClassCastException(
                "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                    " when setting item ${item.name} in config")
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
            throw NoSuchItemException(item)
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
        valueByItem.clear()
    }

    override fun <T : Any> property(item: Item<T>): ReadWriteProperty<Any?, T> {
        if (!contains(item)) {
            throw NoSuchItemException(item)
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

    override val specs: List<Spec>
        get() = mutableListOf<Spec>().apply {
            addAll(object : Iterator<Spec> {
                private var currentConfig = this@BaseConfig
                private var current = currentConfig.specsInLayer.iterator()

                override tailrec fun hasNext(): Boolean {
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

                override fun next(): Spec = current.next()
            }.asSequence())
        }

    override val layer: Config = Layer(this)

    protected fun checkNameConflictInLayer(name: Path) {
        nameByItem.values.forEach {
            val itemName = it.toPath()
            if ((name.size >= itemName.size && name.subList(0, itemName.size) == itemName) ||
                (name.size < itemName.size && itemName.subList(0, name.size) == name)) {
                throw NameConflictException("item ${name.name} cannot be added" +
                    " since item ${itemName.name} has been added to config")
            }
        }
    }

    open fun checkNameConflict(name: Path) {
        checkNameConflictInLayer(name)
        parent?.checkNameConflict(name)
    }

    override fun addSpec(spec: Spec) {
        lock.write {
            if (hasChildren) {
                throw SpecFrozenException(this)
            }
            spec.items.forEach { item ->
                val name = spec.qualify(item.name)
                if (item !in this) {
                    checkNameConflict(name.toPath())
                    nameByItem[item] = name
                    valueByItem[item] = when (item) {
                        is OptionalItem -> ValueState.Value(item.default)
                        is RequiredItem -> ValueState.Unset
                        is LazyItem -> ValueState.Lazy(item.thunk)
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
        data class Lazy<T : Any>(var thunk: (config: ItemContainer) -> T) : ValueState()
        data class Value(var value: Any) : ValueState()
    }

    private class Layer(val config: BaseConfig) : Config by config {
        override val parent: Config? = null
        override val specs: List<Spec> = config.specsInLayer
        override val layer: Config = this

        override fun toMap(): Map<String, Any> {
            return mutableMapOf<String, Any>().apply {
                for (item in config.valueByItem.keys) {
                    if (config.getOrNull(item) != null) {
                        put(nameOf(item), config[item].toCompatibleValue(mapper))
                    }
                }
            }
        }

        override fun <T : Any> get(item: Item<T>): T =
            if (contains(item)) config[item] else throw NoSuchItemException(item)

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
