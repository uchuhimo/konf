package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.Path
import com.uchuhimo.konf.name
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toDescription

open class KVSource(
        val map: Map<String, Any>,
        type: String = "",
        context: Map<String, String> = mapOf()
) : ValueSource(map, type.notEmptyOr("KV"), context) {
    override fun contains(path: Path): Boolean = map.contains(path.name)

    override fun getOrNull(path: Path): Source? = map[path.name]?.castToSource(context)

    override fun toMap(): Map<String, Source> = map.mapValues { (_, value) ->
        value.castToSource(context).apply { addInfo("inMap", this@KVSource.info.toDescription()) }
    }
}

fun Map<String, Any>.asKVSource() = KVSource(this)
