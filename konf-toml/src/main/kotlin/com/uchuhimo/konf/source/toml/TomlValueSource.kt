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

package com.uchuhimo.konf.source.toml

import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.ValueSource

/**
 * Source from a TOML value.
 */
class TomlValueSource(
    value: Any,
    context: Map<String, String> = mapOf()
) : ValueSource(value, "TOML-value", context) {
    override fun Any.castToSource(context: Map<String, String>): Source = asTomlSource(context)

    override fun toLong(): Long = cast()

    override fun toInt(): Int = toLong().also { value ->
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
            throw ParseException("$value is out of range of Int")
        }
    }.toInt()
}

fun Any.asTomlSource(context: Map<String, String> = mapOf()): Source =
    when {
        this is Source -> this
        this is Map<*, *> ->
            // assume that only `Map<String, Any>` is provided,
            // key type mismatch will be detected when loaded into Config
            @Suppress("UNCHECKED_CAST")
            TomlMapSource(this as Map<String, Any>, context)
        else -> TomlValueSource(this, context)
    }
