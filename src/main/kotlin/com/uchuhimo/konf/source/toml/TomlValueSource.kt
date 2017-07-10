package com.uchuhimo.konf.source.toml

import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.ValueSource

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
        if (this is Source) {
            this
        } else if (this is Map<*, *>) {
            try {
                TomlMapSource(this as Map<String, Any>, context)
            } catch (e: ClassCastException) {
                TomlValueSource(this, context)
            }
        } else {
            TomlValueSource(this, context)
        }
