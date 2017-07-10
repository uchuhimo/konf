package com.uchuhimo.konf.source.toml

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.MapSource

class TomlMapSource(
        map: Map<String, Any>,
        context: Map<String, String> = mapOf()
) : MapSource(map, "TOML", context) {
    override fun Any.castToSource(context: Map<String, String>): Source = asTomlSource(context)
}
