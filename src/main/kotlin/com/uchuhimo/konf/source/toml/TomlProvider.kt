package com.uchuhimo.konf.source.toml

import com.moandjiezana.toml.Toml
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import java.io.InputStream
import java.io.Reader

object TomlProvider : SourceProvider {
    override fun fromReader(reader: Reader): Source =
            Toml().read(reader).toMap().asTomlSource()

    override fun fromInputStream(inputStream: InputStream): Source =
            Toml().read(inputStream).toMap().asTomlSource()
}
