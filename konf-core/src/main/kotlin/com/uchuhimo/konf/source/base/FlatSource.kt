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
import com.uchuhimo.konf.name
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.toDescription
import com.uchuhimo.konf.toPath

/**
 * Source from a map in flat format.
 */
open class FlatSource(
    val map: Map<String, String>,
    val prefix: String = "",
    type: String = "",
    context: Map<String, String> = mapOf()
) : StringValueSource, SourceInfo by SourceInfo.with(context) {
    init {
        @Suppress("LeakingThis")
        addInfo("type", type.notEmptyOr("flat"))
    }

    override fun contains(path: Path): Boolean {
        if (path.isEmpty()) {
            return true
        } else {
            val fullPath = if (prefix.isEmpty()) path.name else "$prefix.${path.name}"
            return map.any { (key, _) ->
                if (key.startsWith(fullPath)) {
                    if (key == fullPath) {
                        true
                    } else {
                        val suffix = key.removePrefix(fullPath)
                        suffix.startsWith(".") && suffix.length > 1
                    }
                } else {
                    false
                }
            }
        }
    }

    override fun getOrNull(path: Path): Source? {
        return if (path.isEmpty()) {
            this
        } else {
            if (contains(path)) {
                if (prefix.isEmpty()) {
                    FlatSource(map, path.name, context = context)
                } else {
                    FlatSource(map, "$prefix.${path.name}", context = context)
                }
            } else {
                null
            }
        }
    }

    override fun getValue(): String =
        map[prefix] ?: throw NoSuchPathException(this, prefix.toPath())

    override fun toRegularList(): List<Source> {
        try {
            return super.toRegularList()
        } catch (e: Exception) {
            val list = generateSequence(0) { it + 1 }.map {
                getOrNull(it.toString().toPath())
            }.takeWhile {
                it != null
            }.filterNotNull().toList().map {
                it.apply { addInfo("inList", this@FlatSource.info.toDescription()) }
            }
            if (list.isNotEmpty()) {
                return list
            } else {
                throw ParseException("$prefix in $map cannot be parsed to a list")
            }
        }
    }

    override fun isMap(): Boolean = toMap().isNotEmpty()

    override fun toMap(): Map<String, Source> {
        val keys = if (prefix.isEmpty()) {
            map.keys
        } else {
            map.keys.asSequence().filter {
                it.startsWith("$prefix.")
            }.map {
                it.removePrefix("$prefix.")
            }.filter {
                it.isNotEmpty()
            }.toList()
        }
        return keys.map {
            it.takeWhile { it != '.' }
        }.toSet().associate {
            val newPrefix = if (prefix.isEmpty()) it else "$prefix.$it"
            it to FlatSource(map, newPrefix, context = context).apply {
                addInfo("inMap", this@FlatSource.info.toDescription())
            }
        }
    }

    override fun isText(): Boolean = map.contains(prefix)
}

/**
 * Returns a map in flat format for this config.
 *
 * The returned map contains all items in this config.
 * This map can be loaded into config as [com.uchuhimo.konf.source.base.FlatSource] using
 * `config.from.map.flat(map)`.
 */
fun Config.toFlatMap(): Map<String, String> {
    fun MutableMap<String, String>.putFlat(key: String, value: Any) {
        when (value) {
            is List<*> -> {
                if (value.isNotEmpty()) {
                    val first = value[0]
                    when (first) {
                        is List<*>, is Map<*, *> ->
                            value.forEachIndexed { index, child ->
                                putFlat("$key.$index", child!!)
                            }
                        else -> {
                            if (value.map { it.toString() }.any { it.contains(',') }) {
                                value.forEachIndexed { index, child ->
                                    putFlat("$key.$index", child!!)
                                }
                            } else {
                                put(key, value.joinToString(","))
                            }
                        }
                    }
                } else {
                    put(key, "")
                }
            }
            is Map<*, *> ->
                value.forEach { (suffix, child) ->
                    putFlat("$key.$suffix", child!!)
                }
            else -> put(key, value.toString())
        }
    }
    return mutableMapOf<String, String>().apply {
        for ((key, value) in this@toFlatMap.toMap()) {
            putFlat(key, value)
        }
    }
}
