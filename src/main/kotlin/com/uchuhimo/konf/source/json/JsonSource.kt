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

package com.uchuhimo.konf.source.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.toDescription
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Source from a JSON node.
 */
class JsonSource(
        val node: JsonNode,
        context: Map<String, String> = mapOf()
) : Source, SourceInfo by SourceInfo.with(context) {
    init {
        addInfo("type", "JSON")
    }

    override fun contains(path: Path): Boolean {
        return if (path.isEmpty()) {
            true
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val childNode = node[key]
            if (childNode != null) {
                JsonSource(childNode, context).contains(rest)
            } else {
                false
            }
        }
    }

    override fun getOrNull(path: Path): Source? {
        return if (path.isEmpty()) {
            this
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val childNode = node[key]
            if (childNode != null) {
                JsonSource(childNode, context).getOrNull(rest)
            } else {
                null
            }
        }
    }

    override fun toList(): List<Source> {
        if (node.isArray) {
            return mutableListOf<JsonNode>().apply {
                addAll(node.elements().asSequence())
            }.map {
                        JsonSource(it, context).apply {
                            addInfo("inList", this@JsonSource.info.toDescription())
                        }
                    }
        } else {
            throw WrongTypeException(this, node.nodeType.name, JsonNodeType.ARRAY.name)
        }
    }

    override fun toMap(): Map<String, Source> {
        if (node.isObject) {
            return mutableMapOf<String, JsonNode>().apply {
                for ((key, value) in node.fields()) {
                    put(key, value)
                }
            }.mapValues { (_, value) ->
                        JsonSource(value, context).apply {
                            addInfo("inMap", this@JsonSource.info.toDescription())
                        }
                    }
        } else {
            throw WrongTypeException(this, node.nodeType.name, JsonNodeType.OBJECT.name)
        }
    }

    override fun toText(): String {
        if (node.isTextual) {
            return node.textValue()
        } else {
            throw WrongTypeException(this, node.nodeType.name, JsonNodeType.STRING.name)
        }
    }

    override fun toBoolean(): Boolean {
        if (node.isBoolean) {
            return node.booleanValue()
        } else {
            throw WrongTypeException(this, node.nodeType.name, JsonNodeType.BOOLEAN.name)
        }
    }

    override fun toDouble(): Double {
        if (node.isDouble) {
            return node.doubleValue()
        } else {
            throw WrongTypeException(this, node.nodeType.name, "DOUBLE")
        }
    }

    override fun toFloat(): Float {
        return if (node.isFloat) {
            node.floatValue()
        } else {
            super.toFloat()
        }
    }

    override fun toInt(): Int {
        if (node.isInt) {
            return node.intValue()
        } else {
            throw WrongTypeException(this, node.nodeType.name, "INT")
        }
    }

    override fun toLong(): Long {
        return if (node.isLong) {
            node.longValue()
        } else {
            super.toLong()
        }
    }

    override fun toShort(): Short {
        return if (node.isShort) {
            node.shortValue()
        } else {
            super.toShort()
        }
    }

    override fun toBigInteger(): BigInteger {
        return if (node.isBigInteger) {
            node.bigIntegerValue()
        } else {
            super.toBigInteger()
        }
    }

    override fun toBigDecimal(): BigDecimal {
        return if (node.isBigDecimal) {
            node.decimalValue()
        } else {
            super.toBigDecimal()
        }
    }
}
