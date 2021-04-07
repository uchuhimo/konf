/*
 * Copyright 2017-2021 the original author or authors.
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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.uchuhimo.konf.source.MergedSource
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.EmptyMapSource
import com.uchuhimo.konf.source.deserializer.DurationDeserializer
import com.uchuhimo.konf.source.deserializer.EmptyStringToCollectionDeserializerModifier
import com.uchuhimo.konf.source.deserializer.OffsetDateTimeDeserializer
import com.uchuhimo.konf.source.deserializer.StringDeserializer
import com.uchuhimo.konf.source.deserializer.ZoneDateTimeDeserializer
import com.uchuhimo.konf.source.load
import com.uchuhimo.konf.source.loadItem
import com.uchuhimo.konf.source.toCompatibleValue
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.WeakHashMap
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
    override val mapper: ObjectMapper = createDefaultMapper(),
    private val specsInLayer: MutableList<Spec> = mutableListOf(),
    private val featuresInLayer: MutableMap<Feature, Boolean> = mutableMapOf(),
    private val nodeByItem: MutableMap<Item<*>, ItemNode> = mutableMapOf(),
    private val tree: TreeNode = ContainerNode.placeHolder(),
    private val hasChildren: Value<Boolean> = Value(false),
    private val beforeSetFunctions: MutableList<(item: Item<*>, value: Any?) -> Unit> = mutableListOf(),
    private val afterSetFunctions: MutableList<(item: Item<*>, value: Any?) -> Unit> = mutableListOf(),
    private val beforeLoadFunctions: MutableList<(source: Source) -> Unit> = mutableListOf(),
    private val afterLoadFunctions: MutableList<(source: Source) -> Unit> = mutableListOf(),
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) : Config {
    private val _source: Value<Source> = Value(EmptyMapSource())
    open val source: Source get() = _source.value
    private val nameByItem: WeakHashMap<Item<*>, String> = WeakHashMap()

    override fun <T> lock(action: () -> T): T = lock.write(action)

    override fun at(path: String): Config {
        if (path.isEmpty()) {
            return this
        } else {
            val originalConfig = this
            return object : BaseConfig(
                name = name,
                parent = parent?.at(path) as BaseConfig?,
                mapper = mapper,
                specsInLayer = specsInLayer,
                featuresInLayer = featuresInLayer,
                nodeByItem = nodeByItem,
                tree = tree.getOrNull(path) ?: ContainerNode.placeHolder().also {
                    lock.write { tree[path] = it }
                },
                hasChildren = hasChildren,
                beforeSetFunctions = beforeSetFunctions,
                afterSetFunctions = afterSetFunctions,
                beforeLoadFunctions = beforeLoadFunctions,
                afterLoadFunctions = afterLoadFunctions,
                lock = lock
            ) {
                override val source: Source
                    get() {
                        if (path !in originalConfig.source) {
                            originalConfig.source.tree[path] = ContainerNode.placeHolder()
                        }
                        return originalConfig.source[path]
                    }
            }
        }
    }

    override fun withPrefix(prefix: String): Config {
        if (prefix.isEmpty()) {
            return this
        } else {
            val originalConfig = this
            return object : BaseConfig(
                name = name,
                parent = parent?.withPrefix(prefix) as BaseConfig?,
                mapper = mapper,
                specsInLayer = specsInLayer,
                featuresInLayer = featuresInLayer,
                nodeByItem = nodeByItem,
                tree = if (prefix.isEmpty()) tree
                else ContainerNode.empty().apply {
                    set(prefix, tree)
                },
                hasChildren = hasChildren,
                beforeSetFunctions = beforeSetFunctions,
                afterSetFunctions = afterSetFunctions,
                beforeLoadFunctions = beforeLoadFunctions,
                afterLoadFunctions = afterLoadFunctions,
                lock = lock
            ) {
                override val source: Source get() = originalConfig.source.withPrefix(prefix)
            }
        }
    }

    override fun iterator(): Iterator<Item<*>> {
        return if (parent != null) {
            (nodeByItem.keys.iterator().asSequence() + parent!!.iterator().asSequence()).iterator()
        } else {
            nodeByItem.keys.iterator()
        }
    }

    override val itemWithNames: List<Pair<Item<*>, String>>
        get() = lock.read { tree.leafByPath }.map { (name, node) ->
            (node as ItemNode).item to name
        } + (parent?.itemWithNames ?: listOf())

    override fun toMap(): Map<String, Any> {
        return lock.read {
            itemWithNames.map { (item, name) ->
                name to try {
                    getOrNull(item, errorWhenNotFound = true).toCompatibleValue(mapper)
                } catch (_: UnsetValueException) {
                    ValueState.Unset
                }
            }.filter { (_, value) -> value != ValueState.Unset }.toMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(item: Item<T>): T = getOrNull(item, errorWhenNotFound = true) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String): T = getOrNull(name, errorWhenNotFound = true) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrNull(item: Item<T>): T? = getOrNull(item, errorWhenNotFound = false) as T?

    private fun setState(item: Item<*>, state: ValueState) {
        if (item in nodeByItem) {
            nodeByItem[item]!!.value = state
        } else {
            nodeByItem[item] = ItemNode(state, item)
        }
    }

    open fun getOrNull(
        item: Item<*>,
        errorWhenNotFound: Boolean,
        errorWhenGetDefault: Boolean = false,
        lazyContext: ItemContainer = this
    ): Any? {
        val valueState = lock.read { nodeByItem[item]?.value }
        if (valueState != null) {
            @Suppress("UNCHECKED_CAST")
            when (valueState) {
                is ValueState.Unset ->
                    if (errorWhenNotFound) {
                        throw UnsetValueException(item)
                    } else {
                        return null
                    }
                is ValueState.Null -> return null
                is ValueState.Value -> return valueState.value
                is ValueState.Default -> {
                    if (errorWhenGetDefault) {
                        throw GetDefaultValueException(item)
                    } else {
                        return valueState.value
                    }
                }
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
                            return null
                        } else {
                            throw InvalidLazySetException(
                                "fail to cast null to ${item.type.rawClass}" +
                                    " when getting item ${item.name} in config"
                            )
                        }
                    } else {
                        if (item.type.rawClass.isInstance(value)) {
                            return value
                        } else {
                            throw InvalidLazySetException(
                                "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                                    " when getting item ${item.name} in config"
                            )
                        }
                    }
                }
            }
        } else {
            if (parent != null) {
                return parent!!.getOrNull(item, errorWhenNotFound, errorWhenGetDefault, lazyContext)
            } else {
                if (errorWhenNotFound) {
                    throw NoSuchItemException(item)
                } else {
                    return null
                }
            }
        }
    }

    open fun getItemOrNull(name: String): Item<*>? {
        val trimmedName = name.trim()
        val item = getItemInLayerOrNull(trimmedName)
        return item ?: parent?.getItemOrNull(trimmedName)
    }

    private fun getItemInLayerOrNull(name: String): Item<*>? {
        return lock.read {
            (tree.getOrNull(name) as? ItemNode)?.item
        }
    }

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

    private fun containsInLayer(item: Item<*>) = lock.read { nodeByItem.containsKey(item) }

    override fun contains(item: Item<*>): Boolean {
        return if (containsInLayer(item)) {
            true
        } else {
            parent?.contains(item) ?: false
        }
    }

    private fun containsInLayer(name: String): Boolean {
        return containsInLayer(name.toPath())
    }

    override fun contains(name: String): Boolean {
        return if (containsInLayer(name)) {
            true
        } else {
            parent?.contains(name) ?: false
        }
    }

    private fun TreeNode.partialMatch(path: Path): Boolean {
        return if (this is LeafNode) {
            true
        } else if (path.isEmpty()) {
            !isEmpty()
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val result = children[key]
            if (result != null) {
                return result.partialMatch(rest)
            } else {
                return false
            }
        }
    }

    private fun containsInLayer(path: Path): Boolean {
        return lock.read {
            tree.partialMatch(path)
        }
    }

    override fun contains(path: Path): Boolean =
        containsInLayer(path) || (parent?.contains(path) ?: false)

    override fun nameOf(item: Item<*>): String {
        return nameByItem[item] ?: {
            val name = lock.read { tree.firstPath { it is ItemNode && it.item == item } }?.name
            if (name != null) {
                nameByItem[item] = name
                name
            } else {
                parent?.nameOf(item) ?: throw NoSuchItemException(item)
            }
        }()
    }

    open fun addBeforeSetFunction(beforeSetFunction: (item: Item<*>, value: Any?) -> Unit) {
        beforeSetFunctions += beforeSetFunction
        parent?.addBeforeSetFunction(beforeSetFunction)
    }

    open fun removeBeforeSetFunction(beforeSetFunction: (item: Item<*>, value: Any?) -> Unit) {
        beforeSetFunctions.remove(beforeSetFunction)
        parent?.removeBeforeSetFunction(beforeSetFunction)
    }

    override fun beforeSet(beforeSetFunction: (item: Item<*>, value: Any?) -> Unit): Handler {
        addBeforeSetFunction(beforeSetFunction)
        return object : Handler {
            override fun cancel() {
                removeBeforeSetFunction(beforeSetFunction)
            }
        }
    }

    private fun notifyBeforeSet(item: Item<*>, value: Any?) {
        for (beforeSetFunction in beforeSetFunctions) {
            beforeSetFunction(item, value)
        }
    }

    open fun addAfterSetFunction(afterSetFunction: (item: Item<*>, value: Any?) -> Unit) {
        afterSetFunctions += afterSetFunction
        parent?.addAfterSetFunction(afterSetFunction)
    }

    open fun removeAfterSetFunction(afterSetFunction: (item: Item<*>, value: Any?) -> Unit) {
        afterSetFunctions.remove(afterSetFunction)
        parent?.removeAfterSetFunction(afterSetFunction)
    }

    override fun afterSet(afterSetFunction: (item: Item<*>, value: Any?) -> Unit): Handler {
        addAfterSetFunction(afterSetFunction)
        return object : Handler {
            override fun cancel() {
                removeAfterSetFunction(afterSetFunction)
            }
        }
    }

    private fun notifyAfterSet(item: Item<*>, value: Any?) {
        for (afterSetFunction in afterSetFunctions) {
            afterSetFunction(item, value)
        }
    }

    override fun rawSet(item: Item<*>, value: Any?) {
        if (item in this) {
            if (value == null) {
                if (item.nullable) {
                    item.notifySet(null)
                    item.notifyBeforeSet(this, value)
                    notifyBeforeSet(item, value)
                    lock.write {
                        setState(item, ValueState.Null)
                    }
                    notifyAfterSet(item, value)
                    item.notifyAfterSet(this, value)
                } else {
                    throw ClassCastException(
                        "fail to cast null to ${item.type.rawClass}" +
                            " when setting item ${item.name} in config"
                    )
                }
            } else {
                if (item.type.rawClass.isInstance(value)) {
                    item.notifySet(value)
                    item.notifyBeforeSet(this, value)
                    notifyBeforeSet(item, value)
                    lock.write {
                        setState(item, ValueState.Value(value))
                    }
                    notifyAfterSet(item, value)
                    item.notifyAfterSet(this, value)
                } else {
                    throw ClassCastException(
                        "fail to cast $value with ${value::class} to ${item.type.rawClass}" +
                            " when setting item ${item.name} in config"
                    )
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
                setState(item, ValueState.Lazy(thunk))
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
            lock.write {
                setState(item, ValueState.Unset)
            }
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
        lock.write {
            nodeByItem.clear()
            tree.children.clear()
            if (tree is MapNode) {
                tree.isPlaceHolder = true
            }
        }
    }

    override fun clearAll() {
        clear()
        parent?.clearAll()
    }

    override fun containsRequired(): Boolean = try {
        validateRequired()
        true
    } catch (ex: UnsetValueException) {
        false
    }

    override fun validateRequired(): Config {
        for (item in this) {
            if (item is RequiredItem) {
                getOrNull(item, errorWhenNotFound = true)
            }
        }
        return this
    }

    override fun plus(config: Config): Config {
        return when (config) {
            is BaseConfig -> MergedConfig(this, config)
            else -> config.withFallback(this)
        }
    }

    override fun withFallback(config: Config): Config {
        return config + this
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

    override val sources: List<Source>
        get() {
            return lock.read { mutableListOf(source) }.apply {
                for (source in parent?.sources ?: listOf()) {
                    add(source)
                }
            }
        }

    override fun enable(feature: Feature): Config {
        return apply {
            lock.write {
                featuresInLayer[feature] = true
            }
        }
    }

    override fun disable(feature: Feature): Config {
        return apply {
            lock.write {
                featuresInLayer[feature] = false
            }
        }
    }

    override fun isEnabled(feature: Feature): Boolean {
        return lock.read {
            featuresInLayer[feature] ?: parent?.isEnabled(feature) ?: feature.enabledByDefault
        }
    }

    override fun addItem(item: Item<*>, prefix: String) {
        lock.write {
            if (hasChildren.value) {
                throw LayerFrozenException(this)
            }
            val path = prefix.toPath() + item.name.toPath()
            val name = path.name
            if (item !in this) {
                if (path in this) {
                    throw NameConflictException("item $name cannot be added")
                }
                val node = ItemNode(
                    when (item) {
                        is OptionalItem -> ValueState.Default(item.default)
                        is RequiredItem -> ValueState.Unset
                        is LazyItem -> ValueState.Lazy(item.thunk)
                    },
                    item
                )
                tree[name] = node
                nodeByItem[item] = node
                val sources = this.sources
                val mergedSource = if (sources.isNotEmpty()) {
                    sources.reduceRight { source, acc -> MergedSource(source, acc) }
                } else {
                    null
                }
                mergedSource?.let { loadItem(item, path, it) }
            } else {
                throw RepeatedItemException(name)
            }
        }
    }

    override fun addSpec(spec: Spec) {
        lock.write {
            if (hasChildren.value) {
                throw LayerFrozenException(this)
            }
            val sources = this.sources
            val mergedSource = if (sources.isNotEmpty()) {
                sources.reduceRight { source, acc -> MergedSource(source, acc) }
            } else {
                null
            }
            spec.items.forEach { item ->
                val name = spec.qualify(item)
                if (item !in this) {
                    val path = name.toPath()
                    if (path in this) {
                        throw NameConflictException("item $name cannot be added")
                    }
                    val node = ItemNode(
                        when (item) {
                            is OptionalItem -> ValueState.Default(item.default)
                            is RequiredItem -> ValueState.Unset
                            is LazyItem -> ValueState.Lazy(item.thunk)
                        },
                        item
                    )
                    tree[name] = node
                    nodeByItem[item] = node
                    mergedSource?.let { loadItem(item, path, it) }
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

    override fun withLayer(name: String): BaseConfig {
        lock.write { hasChildren.value = true }
        return BaseConfig(name, this, mapper)
    }

    open fun addBeforeLoadFunction(beforeLoadFunction: (source: Source) -> Unit) {
        beforeLoadFunctions += beforeLoadFunction
        parent?.addBeforeLoadFunction(beforeLoadFunction)
    }

    open fun removeBeforeLoadFunction(beforeLoadFunction: (source: Source) -> Unit) {
        beforeLoadFunctions.remove(beforeLoadFunction)
        parent?.removeBeforeLoadFunction(beforeLoadFunction)
    }

    override fun beforeLoad(beforeLoadFunction: (source: Source) -> Unit): Handler {
        addBeforeLoadFunction(beforeLoadFunction)
        return object : Handler {
            override fun cancel() {
                removeBeforeLoadFunction(beforeLoadFunction)
            }
        }
    }

    private fun notifyBeforeLoad(source: Source) {
        for (beforeLoadFunction in beforeLoadFunctions) {
            beforeLoadFunction(source)
        }
    }

    open fun addAfterLoadFunction(afterLoadFunction: (source: Source) -> Unit) {
        afterLoadFunctions += afterLoadFunction
        parent?.addAfterLoadFunction(afterLoadFunction)
    }

    open fun removeAfterLoadFunction(afterLoadFunction: (source: Source) -> Unit) {
        afterLoadFunctions.remove(afterLoadFunction)
        parent?.removeAfterLoadFunction(afterLoadFunction)
    }

    override fun afterLoad(afterLoadFunction: (source: Source) -> Unit): Handler {
        addAfterLoadFunction(afterLoadFunction)
        return object : Handler {
            override fun cancel() {
                removeAfterLoadFunction(afterLoadFunction)
            }
        }
    }

    private fun notifyAfterLoad(source: Source) {
        for (afterLoadFunction in afterLoadFunctions) {
            afterLoadFunction(source)
        }
    }

    override fun withSource(source: Source): Config {
        return withLayer("source: ${source.description}").also { config ->
            config.lock.write {
                config._source.value = load(config, source)
            }
        }
    }

    override fun withLoadTrigger(
        description: String,
        trigger: (
            config: Config,
            load: (source: Source) -> Unit
        ) -> Unit
    ): Config {
        return withLayer("trigger: $description").apply {
            trigger(this) { source ->
                notifyBeforeLoad(source)
                lock.write {
                    this._source.value = load(this, source)
                }
                notifyAfterLoad(source)
            }
        }
    }

    override fun toString(): String {
        return "Config(items=${toMap()})"
    }

    class ItemNode(override var value: ValueState, val item: Item<*>) : ValueNode

    data class Value<T>(var value: T)

    sealed class ValueState {
        object Unset : ValueState()
        object Null : ValueState()
        data class Lazy<T>(val thunk: (config: ItemContainer) -> T) : ValueState()
        data class Value(val value: Any) : ValueState()
        data class Default(val value: Any?) : ValueState()
    }
}

/**
 * Returns a new default object mapper for config.
 */
fun createDefaultMapper(): ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
    .apply {
        coercionConfigDefaults().setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty)
    }
    .registerModules(
        SimpleModule()
            .addDeserializer(String::class.java, StringDeserializer)
            .setDeserializerModifier(EmptyStringToCollectionDeserializerModifier),
        JavaTimeModule()
            .addDeserializer(Duration::class.java, DurationDeserializer)
            .addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer)
            .addDeserializer(ZonedDateTime::class.java, ZoneDateTimeDeserializer)
    )
