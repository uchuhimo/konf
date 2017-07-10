package com.uchuhimo.konf.source.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.toDescription
import java.math.BigDecimal
import java.math.BigInteger

class JsonSource(
        val node: JsonNode,
        context: Map<String, String> = mapOf()
) : Source {
    val _info = mutableMapOf("type" to "JSON")

    override val info: Map<String, String> get() = _info

    override fun addInfo(name: String, value: String) {
        _info.put(name, value)
    }

    val _context: MutableMap<String, String> = context.toMutableMap()

    override val context: Map<String, String> get() = _context

    override fun addContext(name: String, value: String) {
        _context.put(name, value)
    }

    override fun contains(path: Path): Boolean {
        if (path.isEmpty()) {
            return true
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val childNode = node[key]
            if (childNode != null) {
                return JsonSource(childNode, context).contains(rest)
            } else {
                return false
            }
        }
    }

    override fun getOrNull(path: Path): Source? {
        if (path.isEmpty()) {
            return this
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val childNode = node[key]
            if (childNode != null) {
                return JsonSource(childNode, context).getOrNull(rest)
            } else {
                return null
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
            }.mapValues {
                (_, value) ->
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
        if (node.isFloat) {
            return node.floatValue()
        } else {
            return super.toFloat()
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
        if (node.isLong) {
            return node.longValue()
        } else {
            return super.toLong()
        }
    }

    override fun toShort(): Short {
        if (node.isShort) {
            return node.shortValue()
        } else {
            return super.toShort()
        }
    }

    override fun toBigInteger(): BigInteger {
        if (node.isBigInteger) {
            return node.bigIntegerValue()
        } else {
            return super.toBigInteger()
        }
    }

    override fun toBigDecimal(): BigDecimal {
        if (node.isBigDecimal) {
            return node.decimalValue()
        } else {
            return super.toBigDecimal()
        }
    }
}
