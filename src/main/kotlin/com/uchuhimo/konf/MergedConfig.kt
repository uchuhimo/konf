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

import com.uchuhimo.konf.source.Source
import java.util.ArrayDeque
import java.util.Deque
import kotlin.properties.ReadWriteProperty

/**
 * Config that merge [fallback] and [facade].
 *
 * All operations will be applied to [facade] first, and then fall back to [facade] when necessary.
 */
class MergedConfig(val fallback: Config, val facade: Config) :
    BaseConfig("merged(facade=${facade.name}, fallback=${fallback.name})") {

    override fun rawSet(item: Item<*>, value: Any?) {
        if (item in facade) {
            facade.rawSet(item, value)
        } else {
            fallback.rawSet(item, value)
        }
    }

    override fun <T> set(item: Item<T>, value: T) {
        if (item in facade) {
            facade[item] = value
        } else {
            fallback[item] = value
        }
    }

    override fun <T> set(name: String, value: T) {
        if (name in facade) {
            facade[name] = value
        } else {
            fallback[name] = value
        }
    }

    override fun <T> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T) {
        if (item in facade) {
            facade.lazySet(item, thunk)
        } else {
            fallback.lazySet(item, thunk)
        }
    }

    override fun <T> lazySet(name: String, thunk: (config: ItemContainer) -> T) {
        if (name in facade) {
            facade.lazySet(name, thunk)
        } else {
            fallback.lazySet(name, thunk)
        }
    }

    override fun unset(item: Item<*>) {
        if (item in facade) {
            facade.unset(item)
        } else {
            fallback.unset(item)
        }
    }

    override fun unset(name: String) {
        if (name in facade) {
            facade.unset(name)
        } else {
            fallback.unset(name)
        }
    }

    override fun clear() {
        facade.clear()
        fallback.clear()
    }

    override val specs: List<Spec>
        get() = facade.specs + fallback.specs

    override val sources: Deque<Source>
        get() = ArrayDeque(facade.sources).apply {
            for (source in fallback.sources) {
                addLast(source)
            }
        }

    override fun addItem(item: Item<*>, prefix: String) {
        val path = prefix.toPath() + item.name.toPath()
        val name = path.name
        if (item !in fallback) {
            if (path in fallback) {
                throw NameConflictException("item $name cannot be added")
            }
        } else {
            throw RepeatedItemException(name)
        }
        facade.addItem(item, prefix)
    }

    override fun addSpec(spec: Spec) {
        spec.items.forEach { item ->
            val name = spec.qualify(item)
            if (item !in fallback) {
                val path = name.toPath()
                if (path in fallback) {
                    throw NameConflictException("item $name cannot be added")
                }
            } else {
                throw RepeatedItemException(name)
            }
        }
        facade.addSpec(spec)
    }

    override fun addSource(source: Source) {
        facade.addSource(source)
        fallback.addSource(source)
    }

    override fun <T> lock(action: () -> T): T = facade.lock { fallback.lock(action) }

    override fun toMap(): Map<String, Any> = fallback.toMap() + facade.toMap()

    override fun <T> get(item: Item<T>): T {
        return if (item in facade) {
            facade[item]
        } else {
            fallback[item]
        }
    }

    override fun <T> get(name: String): T {
        return if (name in facade) {
            facade[name]
        } else {
            fallback[name]
        }
    }

    override fun <T> getOrNull(item: Item<T>): T? {
        return if (item in facade) {
            facade.getOrNull(item)
        } else {
            fallback.getOrNull(item)
        }
    }

    override fun <T> getOrNull(name: String): T? {
        return if (name in facade) {
            facade.getOrNull(name)
        } else {
            fallback.getOrNull(name)
        }
    }

    override fun getOrNull(
        item: Item<*>,
        errorWhenNotFound: Boolean,
        lazyContext: ItemContainer
    ): Any? {
        return if (item in facade) {
            (facade as BaseConfig).getOrNull(item, errorWhenNotFound, lazyContext)
        } else {
            (fallback as BaseConfig).getOrNull(item, errorWhenNotFound, lazyContext)
        }
    }

    override fun iterator(): Iterator<Item<*>> =
        (facade.iterator().asSequence() + fallback.iterator().asSequence()).iterator()

    override fun contains(item: Item<*>): Boolean = item in facade || item in fallback

    override fun contains(name: String): Boolean = name in facade || name in fallback

    override fun nameOf(item: Item<*>): String {
        return if (item in facade) {
            facade.nameOf(item)
        } else {
            fallback.nameOf(item)
        }
    }

    override val itemWithNames: List<Pair<Item<*>, String>>
        get() = facade.itemWithNames + fallback.itemWithNames

    override fun <T> property(item: Item<T>): ReadWriteProperty<Any?, T> {
        return if (item in facade) {
            facade.property(item)
        } else {
            fallback.property(item)
        }
    }

    override fun <T> property(name: String): ReadWriteProperty<Any?, T> {
        return if (name in facade) {
            facade.property(name)
        } else {
            fallback.property(name)
        }
    }
}
