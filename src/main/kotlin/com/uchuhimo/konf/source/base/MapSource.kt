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

package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toDescription

/**
 * Source from a hierarchical map.
 */
open class MapSource(
        val map: Map<String, Any>,
        type: String = "",
        context: Map<String, String> = mapOf()
) : ValueSource(map, type.notEmptyOr("map"), context) {
    override fun contains(path: Path): Boolean {
        return if (path.isEmpty()) {
            true
        } else {
            val key = path.first()
            val rest = path.drop(1)
            map[key]?.castToSource(context)?.contains(rest) ?: false
        }
    }

    override fun getOrNull(path: Path): Source? {
        return if (path.isEmpty()) {
            this
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val result = map[key]
            result?.castToSource(context)?.getOrNull(rest)
        }
    }

    override fun isMap(): Boolean = true

    override fun toMap(): Map<String, Source> = map.mapValues { (_, value) ->
        value.castToSource(context).apply { addInfo("inMap", this@MapSource.info.toDescription()) }
    }
}

/**
 * Returns a hierarchical map for this config.
 *
 * The returned map contains all items in this config.
 * This map can be loaded into config as [com.uchuhimo.konf.source.base.MapSource] using
 * `config.withSourceFrom.map.hierarchical(map)`.
 */
fun Config.toHierarchicalMap(): Map<String, Any> {
    fun MutableMap<String, Any>.putHierarchically(key: String, value: Any) {
        if ('.' in key) {
            val prefix = key.substringBefore('.')
            val suffix = key.substringAfter('.')
            @Suppress("UNCHECKED_CAST")
            val child = getOrPut(prefix) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
            child.putHierarchically(suffix, value)
        } else {
            put(key, value)
        }
    }
    return mutableMapOf<String, Any>().apply {
        for ((key, value) in this@toHierarchicalMap.toMap()) {
            putHierarchically(key, value)
        }
    }
}
