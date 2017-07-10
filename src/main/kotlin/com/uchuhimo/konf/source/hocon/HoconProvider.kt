package com.uchuhimo.konf.source.hocon

import com.typesafe.config.ConfigFactory
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import java.io.InputStream
import java.io.Reader

object HoconProvider : SourceProvider {
    override fun fromReader(reader: Reader): Source =
            HoconSource(ConfigFactory.parseReader(reader))

    override fun fromInputStream(inputStream: InputStream): Source =
            fromReader(inputStream.reader())
}
