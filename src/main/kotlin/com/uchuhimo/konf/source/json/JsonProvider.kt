package com.uchuhimo.konf.source.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import java.io.InputStream
import java.io.Reader

object JsonProvider : SourceProvider {
    override fun fromReader(reader: Reader): Source =
            JsonSource(ObjectMapper().readTree(reader))

    override fun fromInputStream(inputStream: InputStream): Source =
            JsonSource(ObjectMapper().readTree(inputStream))
}
