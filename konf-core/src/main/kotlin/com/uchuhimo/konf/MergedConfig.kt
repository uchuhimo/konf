/*
 * Copyright 2017-2020 the original author or authors.
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

/**
 * Config that merge [fallback] and [facade].
 *
 * All operations will be applied to [facade] first, and then fall back to [facade] when necessary.
 */
open class MergedConfig(val fallback: BaseConfig, val facade: BaseConfig) :
    BaseConfig("merged(facade=${facade.name.notEmptyOr("\"\"")}, fallback=${fallback.name.notEmptyOr("\"\"")})") {

    override fun rawSet(item: Item<*>, value: Any?) {
        if (item in facade) {
            facade.rawSet(item, value)
        } else {
            fallback.rawSet(item, value)
        }
    }

    override fun getItemOrNull(name: String): Item<*>? {
        return facade.getItemOrNull(name) ?: fallback.getItemOrNull(name)
    }

    override fun <T> lazySet(item: Item<T>, thunk: (config: ItemContainer) -> T) {
        if (item in facade) {
            facade.lazySet(item, thunk)
        } else {
            fallback.lazySet(item, thunk)
        }
    }

    override fun unset(item: Item<*>) {
        if (item in facade) {
            facade.unset(item)
        } else {
            fallback.unset(item)
        }
    }

    override fun clear() {
        facade.clear()
        fallback.clear()
    }

    override val specs: List<Spec>
        get() = facade.specs + fallback.specs

    override val sources: List<Source>
        get() = facade.sources.toMutableList().apply {
            for (source in fallback.sources) {
                add(source)
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

    override fun <T> lock(action: () -> T): T = facade.lock { fallback.lock(action) }

    override fun getOrNull(
        item: Item<*>,
        errorWhenNotFound: Boolean,
        errorWhenGetDefault: Boolean,
        lazyContext: ItemContainer
    ): Any? {
        if (item in facade && item in fallback) {
            try {
                return facade.getOrNull(item, errorWhenNotFound, true, lazyContext)
            } catch (ex: Exception) {
                when (ex) {
                    is UnsetValueException -> {
                        return fallback.getOrNull(item, errorWhenNotFound, errorWhenGetDefault, lazyContext)
                    }
                    is GetDefaultValueException -> {
                        try {
                            return fallback.getOrNull(item, errorWhenNotFound, errorWhenGetDefault, lazyContext)
                        } catch (ex: Exception) {
                            when (ex) {
                                is UnsetValueException -> {
                                    if (errorWhenGetDefault) {
                                        throw GetDefaultValueException(item)
                                    } else {
                                        return (item as OptionalItem).default
                                    }
                                }
                                else -> throw ex
                            }
                        }
                    }
                    else -> throw ex
                }
            }
        } else if (item in facade) {
            return facade.getOrNull(item, errorWhenNotFound, errorWhenGetDefault, lazyContext)
        } else {
            return fallback.getOrNull(item, errorWhenNotFound, errorWhenGetDefault, lazyContext)
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
}
