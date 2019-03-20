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

import com.uchuhimo.konf.Path
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo

/**
 * Source from something that can be viewed as string.
 */
interface StringValueSource : Source, SourceInfo {

    fun getValue(): String

    override fun contains(path: Path): Boolean = path.isEmpty()

    override fun getOrNull(path: Path): Source? {
        if (path.isEmpty()) {
            return this
        } else {
            throw NoSuchPathException(this, path)
        }
    }

    override fun isList(): Boolean {
        return try {
            toList()
            true
        } catch (_: ParseException) {
            false
        }
    }

    override fun toList(): List<Source> {
        val value = getValue()
        if (value.isNotEmpty()) {
            return value.split(',').map { SingleStringValueSource(it, context = context) }
        } else {
            throw ParseException("$value cannot be parsed to a list")
        }
    }

    override fun isText(): Boolean = true

    override fun toText(): String = getValue()

    override fun toBoolean(): Boolean {
        val value = getValue()
        return when {
            value.toLowerCase() == "true" -> true
            value.toLowerCase() == "false" -> false
            else -> throw ParseException("$value cannot be parsed to a boolean")
        }
    }

    override fun toDouble(): Double {
        val value = getValue()
        try {
            return value.toDouble()
        } catch (cause: NumberFormatException) {
            throw ParseException("$value cannot be parsed to a double", cause)
        }
    }

    override fun toInt(): Int {
        val value = getValue()
        try {
            return value.toInt()
        } catch (cause: NumberFormatException) {
            throw ParseException("$value cannot be parsed to an int", cause)
        }
    }

    override fun toLong(): Long {
        val value = getValue()
        try {
            return value.toLong()
        } catch (cause: NumberFormatException) {
            throw ParseException("$value cannot be parsed to a long", cause)
        }
    }
}

/**
 * Source from a single string.
 */
open class SingleStringValueSource(
    private val value: String,
    type: String = "",
    context: Map<String, String> = mapOf()
) : StringValueSource, SourceInfo by SourceInfo.with(context) {
    init {
        @Suppress("LeakingThis")
        addInfo("type", type.notEmptyOr("single string"))
    }

    override fun getValue(): String = value
}
