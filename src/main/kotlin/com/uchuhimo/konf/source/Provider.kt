/*
 * Copyright 2017-2018 the original author or authors.
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

/**
 * Provides source from various input format.
 */
interface Provider {
    /**
     * Returns a new source from specified reader.
     *
     * @param reader specified reader for reading character streams
     * @return a new source from specified reader
     */
    fun fromReader(reader: Reader): Source

    /**
     * Returns a new source from specified input stream.
     *
     * @param inputStream specified input stream of bytes
     * @return a new source from specified input stream
     */
    fun fromInputStream(inputStream: InputStream): Source

    /**
     * Returns a new source from specified file.
     *
     * @param file specified file
     * @return a new source from specified file
     */
    fun fromFile(file: File): Source = fromInputStream(file.inputStream().buffered()).apply {
        addContext("file", file.toString())
    }

    /**
     * Returns a new source from specified file path.
     *
     * @param file specified file path
     * @return a new source from specified file path
     */
    fun fromFile(file: String): Source = fromFile(File(file))

    /**
     * Returns a new source from specified string.
     *
     * @param content specified string
     * @return a new source from specified string
     */
    fun fromString(content: String): Source = fromReader(content.reader()).apply {
        addContext("content", "\"\n$content\n\"")
    }

    /**
     * Returns a new source from specified byte array.
     *
     * @param content specified byte array
     * @return a new source from specified byte array
     */
    fun fromBytes(content: ByteArray): Source = fromInputStream(content.inputStream())

    /**
     * Returns a new source from specified portion of byte array.
     *
     * @param content specified byte array
     * @param offset the start offset of the portion of the array to read
     * @param length the length of the portion of the array to read
     * @return a new source from specified portion of byte array
     */
    fun fromBytes(content: ByteArray, offset: Int, length: Int): Source =
        fromInputStream(content.inputStream(offset, length))

    /**
     * Returns a new source from specified url.
     *
     * @param url specified url
     * @return a new source from specified url
     */
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

    /**
     * Returns a new source from specified url string.
     *
     * @param url specified url string
     * @return a new source from specified url string
     */
    fun fromUrl(url: String): Source = fromUrl(URL(url))

    /**
     * Returns a new source from specified resource.
     *
     * @param resource path of specified resource
     * @return a new source from specified resource
     */
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

    /**
     * Returns a provider providing sources that applying the given [transform] function.
     *
     * @param transform the given transformation function
     * @return a provider providing sources that applying the given [transform] function
     */
    fun map(transform: (Source) -> Source): Provider {
        return object : Provider {
            override fun fromReader(reader: Reader): Source =
                this@Provider.fromReader(reader).let(transform)

            override fun fromInputStream(inputStream: InputStream): Source =
                this@Provider.fromInputStream(inputStream).let(transform)

            override fun fromFile(file: File): Source =
                this@Provider.fromFile(file).let(transform)

            override fun fromFile(file: String): Source =
                this@Provider.fromFile(file).let(transform)

            override fun fromString(content: String): Source =
                this@Provider.fromString(content).let(transform)

            override fun fromBytes(content: ByteArray): Source =
                this@Provider.fromBytes(content).let(transform)

            override fun fromBytes(content: ByteArray, offset: Int, length: Int): Source =
                this@Provider.fromBytes(content, offset, length).let(transform)

            override fun fromUrl(url: URL): Source =
                this@Provider.fromUrl(url).let(transform)

            override fun fromUrl(url: String): Source =
                this@Provider.fromUrl(url).let(transform)

            override fun fromResource(resource: String): Source =
                this@Provider.fromResource(resource).let(transform)
        }
    }
}
