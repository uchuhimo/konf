package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.Path
import com.uchuhimo.konf.name
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toDescription
import com.uchuhimo.konf.toPath

open class FlatSource(
        val map: Map<String, String>,
        val prefix: String = "",
        type: String = "",
        context: Map<String, String> = mapOf()
) : Source {
    val _info = mutableMapOf(
            "type" to type.notEmptyOr("flat"))

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
        if (path.isEmpty()) {
            return this
        } else {
            if (contains(path)) {
                if (prefix.isEmpty()) {
                    return FlatSource(map, path.name, context = context)
                } else {
                    return FlatSource(map, "$prefix.${path.name}", context = context)
                }
            } else {
                return null
            }
        }
    }

    private fun getValue(): String =
            map[prefix] ?: throw NoSuchPathException(this, prefix.toPath())

    override fun isList(): Boolean = toList().isNotEmpty()

    override fun toList(): List<Source> {
        return generateSequence(0) { it + 1 }.map {
            getOrNull(it.toString().toPath())
        }.takeWhile {
            it != null
        }.filterNotNull().toList().map {
            it.apply { addInfo("inList", this@FlatSource.info.toDescription()) }
        }
    }

    override fun isMap(): Boolean = toMap().isNotEmpty()

    override fun toMap(): Map<String, Source> {
        return map.keys.filter {
            it.startsWith("$prefix.")
        }.map {
            it.removePrefix("$prefix.")
        }.filter {
            it.isNotEmpty()
        }.map {
            it.takeWhile { it != '.' }
        }.toSet().associate {
            it to FlatSource(map, "$prefix.$it", context = context).apply {
                addInfo("inMap", this@FlatSource.info.toDescription())
            }
        }
    }

    override fun isText(): Boolean = map.contains(prefix)

    override fun toText(): String = getValue()

    override fun toBoolean(): Boolean {
        val value = getValue()
        try {
            return value.toBoolean()
        } catch (cause: NumberFormatException) {
            throw ParseException("$value cannot be parsed to a boolean", cause)
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
