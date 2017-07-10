package com.uchuhimo.konf.source.hocon

import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.toDescription

class HoconValueSource(
        val value: ConfigValue,
        context: Map<String, String> = mapOf()
) : Source {
    val _info = mutableMapOf("type" to "HOCON-value")

    override val info: Map<String, String> get() = _info

    override fun addInfo(name: String, value: String) {
        _info.put(name, value)
    }

    val _context: MutableMap<String, String> = context.toMutableMap()

    override val context: Map<String, String> get() = _context

    override fun addContext(name: String, value: String) {
        _context.put(name, value)
    }

    private val type = value.valueType()

    private fun checkType(actual: ConfigValueType, expected: ConfigValueType) {
        if (actual != expected) {
            throw WrongTypeException(this, "HOCON(${actual.name})", "HOCON(${expected.name})")
        }
    }

    enum class NumType {
        Int, Long, Double
    }

    private fun checkNumType(expected: NumType) {
        val unwrappedValue = value.unwrapped()
        val type = when (unwrappedValue) {
            is Int -> NumType.Int
            is Long -> NumType.Long
            is Double -> NumType.Double
            else -> throw ParseException(
                    "value $unwrappedValue with type ${unwrappedValue::class.java.simpleName}" +
                            " is not a valid number(Int/Long/Double)")
        }
        if (type != expected) {
            throw WrongTypeException(this, "HOCON(${type.name})", "HOCON(${expected.name})")
        }
    }

    private val hoconSource: HoconSource by lazy {
        checkType(type, ConfigValueType.OBJECT)
        HoconSource((value as ConfigObject).toConfig(), context)
    }

    override fun contains(path: Path): Boolean = hoconSource.contains(path)

    override fun getOrNull(path: Path): Source? = hoconSource.getOrNull(path)

    override fun isList(): Boolean = type == ConfigValueType.LIST

    override fun toList(): List<Source> {
        checkType(type, ConfigValueType.LIST)
        return mutableListOf<Source>().apply {
            for (value in (value as ConfigList)) {
                add(HoconValueSource(value, context).apply {
                    addInfo("inList", this@HoconValueSource.info.toDescription())
                })
            }
        }
    }

    override fun isMap(): Boolean = type == ConfigValueType.OBJECT

    override fun toMap(): Map<String, Source> {
        checkType(type, ConfigValueType.OBJECT)
        return mutableMapOf<String, Source>().apply {
            for ((key, value) in (value as ConfigObject)) {
                put(key, HoconValueSource(value, context).apply {
                    addInfo("inMap", this@HoconValueSource.info.toDescription())
                })
            }
        }
    }

    override fun isText(): Boolean = type == ConfigValueType.STRING

    override fun toText(): String {
        checkType(type, ConfigValueType.STRING)
        return value.unwrapped() as String
    }

    override fun isBoolean(): Boolean = type == ConfigValueType.BOOLEAN

    override fun toBoolean(): Boolean {
        checkType(type, ConfigValueType.BOOLEAN)
        return value.unwrapped() as Boolean
    }

    override fun isDouble(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Double

    override fun toDouble(): Double {
        try {
            checkType(type, ConfigValueType.NUMBER)
            checkNumType(NumType.Double)
            return value.unwrapped() as Double
        } catch (e: WrongTypeException) {
            try {
                checkNumType(NumType.Long)
                return (value.unwrapped() as Long).toDouble()
            } catch (e: WrongTypeException) {
                checkNumType(NumType.Int)
                return (value.unwrapped() as Int).toDouble()
            }
        }
    }

    override fun isLong(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Long

    override fun toLong(): Long {
        try {
            checkType(type, ConfigValueType.NUMBER)
            checkNumType(NumType.Long)
            return value.unwrapped() as Long
        } catch (e: WrongTypeException) {
            checkNumType(NumType.Int)
            return (value.unwrapped() as Int).toLong()
        }
    }

    override fun isInt(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Int

    override fun toInt(): Int {
        checkType(type, ConfigValueType.NUMBER)
        checkNumType(NumType.Int)
        return value.unwrapped() as Int
    }
}
