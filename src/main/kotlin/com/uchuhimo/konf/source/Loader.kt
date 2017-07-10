package com.uchuhimo.konf.source

import com.uchuhimo.konf.Config
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL

class Loader(val config: Config, val provider: SourceProvider) {
    fun reader(reader: Reader): Config =
            config.load(provider.fromReader(reader))

    fun inputStream(inputStream: InputStream): Config =
            config.load(provider.fromInputStream(inputStream))

    fun file(file: File): Config =
            config.load(provider.fromFile(file))

    fun string(content: String): Config =
            config.load(provider.fromString(content))

    fun bytes(content: ByteArray): Config =
            config.load(provider.fromBytes(content))

    fun bytes(content: ByteArray, offset: Int, length: Int): Config =
            config.load(provider.fromBytes(content, offset, length))

    fun url(url: URL): Config =
            config.load(provider.fromUrl(url))

    fun resource(resource: String): Config =
            config.load(provider.fromResource(resource))
}
