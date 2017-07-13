/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.net.URL

interface SourceProvider {
    fun fromReader(reader: Reader): Source

    fun fromInputStream(inputStream: InputStream): Source

    fun fromFile(file: File): Source = fromInputStream(file.inputStream().buffered()).apply {
        addContext("file", file.toString())
    }

    fun fromString(content: String): Source = fromReader(content.reader()).apply {
        addContext("content", "\"\n$content\n\"")
    }

    fun fromBytes(content: ByteArray): Source = fromInputStream(content.inputStream())

    fun fromBytes(content: ByteArray, offset: Int, length: Int): Source =
            fromInputStream(content.inputStream(offset, length))

    fun fromUrl(url: URL): Source {
        // from com.fasterxml.jackson.core.JsonFactory._optimizedStreamFromURL in version 2.8.9
        if (url.protocol == "file") {
            val host = url.host
            if (host == null || host.isEmpty()) {
                val path = url.path
                if (path.indexOf('%') < 0) {
                    return fromInputStream(FileInputStream(url.path)).apply {
                        addContext("url", url.toString())
                    }

                }
            }
        }
        return fromInputStream(url.openStream()).apply {
            addContext("url", url.toString())
        }
    }

    fun fromResource(resource: String): Source {
        val loader = Thread.currentThread().contextClassLoader
        val e = loader.getResources(resource)
        if (!e.hasMoreElements()) {
            throw SourceNotFoundException("resource not found on classpath: $resource")
        }
        val sources = mutableListOf<Source>()
        while (e.hasMoreElements()) {
            val url = e.nextElement()
            val source = fromUrl(url)
            sources.add(source)
        }
        return sources.reduce(Source::withFallback).apply {
            addContext("resource", resource)
        }
    }
}
