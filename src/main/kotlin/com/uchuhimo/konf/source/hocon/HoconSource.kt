package com.uchuhimo.konf.source.hocon

import com.typesafe.config.Config
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.name
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.unsupported

class HoconSource(
        val config: Config,
        context: Map<String, String> = mapOf()) : Source {
    val _info = mutableMapOf("type" to "HOCON")

    override val info: Map<String, String> get() = _info

    override fun addInfo(name: String, value: String) {
        _info.put(name, value)
    }

    val _context: MutableMap<String, String> = context.toMutableMap()

    override val context: Map<String, String> get() = _context

    override fun addContext(name: String, value: String) {
        _context.put(name, value)
    }

    override fun contains(path: Path): Boolean = config.hasPath(path.name)

    override fun getOrNull(path: Path): Source? {
        val name = path.name
        if (config.hasPath(name)) {
            return HoconValueSource(config.getValue(name), context)
        } else {
            return null
        }
    }

    override fun toInt(): Int = unsupported()

    override fun toList(): List<Source> = unsupported()

    override fun toMap(): Map<String, Source> = unsupported()

    override fun toText(): String = unsupported()

    override fun toBoolean(): Boolean = unsupported()

    override fun toLong(): Long = unsupported()

    override fun toDouble(): Double = unsupported()
}
