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

class MergedMap<K, V>(val fallback: MutableMap<K, V>, val facade: MutableMap<K, V>) : MutableMap<K, V> {
    override val size: Int
        get() = keys.size

    override fun containsKey(key: K): Boolean {
        return facade.containsKey(key) || fallback.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return facade.containsValue(value) || fallback.containsValue(value)
    }

    override fun get(key: K): V? {
        return facade[key] ?: fallback[key]
    }

    override fun isEmpty(): Boolean {
        return facade.isEmpty() && fallback.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = keys.map { it to getValue(it) }.toMap(LinkedHashMap()).entries
    override val keys: MutableSet<K>
        get() = facade.keys.union(fallback.keys).toMutableSet()
    override val values: MutableCollection<V>
        get() = keys.map { getValue(it) }.toMutableList()

    override fun clear() {
        facade.clear()
        fallback.clear()
    }

    override fun put(key: K, value: V): V? {
        return facade.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        facade.putAll(from)
    }

    override fun remove(key: K): V? {
        if (key in facade) {
            if (key in fallback) {
                fallback.remove(key)
            }
            return facade.remove(key)
        } else {
            return fallback.remove(key)
        }
    }
}
