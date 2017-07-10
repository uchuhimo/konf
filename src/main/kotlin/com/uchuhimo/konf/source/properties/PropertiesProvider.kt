package com.uchuhimo.konf.source.properties

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import com.uchuhimo.konf.source.base.FlatSource
import java.io.InputStream
import java.io.Reader
import java.util.Properties

object PropertiesProvider : SourceProvider {
    private fun Properties.toMap(): Map<String, String> {
        return mapKeys {
            it.key as String
        }.mapValues {
            it.value as String
        }
    }

    override fun fromReader(reader: Reader): Source =
            FlatSource(Properties().apply { load(reader) }.toMap(), type = "properties")

    override fun fromInputStream(inputStream: InputStream): Source =
            FlatSource(Properties().apply { load(inputStream) }.toMap(), type = "properties")

    fun fromSystem(): Source = FlatSource(System.getProperties().toMap(), type = "system-properties")
}
