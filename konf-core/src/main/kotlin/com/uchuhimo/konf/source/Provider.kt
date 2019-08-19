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

import com.uchuhimo.konf.source.base.EmptyMapSource
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import org.reflections.ReflectionUtils
import org.reflections.Reflections
import java.io.File
import java.io.FileInputStream
import java.io.IOException
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
     * @param optional whether this source is optional
     * @return a new source from specified file
     */
    fun fromFile(file: File, optional: Boolean = false): Source {
        val extendContext: Source.() -> Unit = {
            addContext("file", file.toString())
        }
        if (!file.exists() && optional) {
            return EmptyMapSource.apply(extendContext)
        }
        return file.inputStream().buffered().use { inputStream ->
            fromInputStream(inputStream).apply(extendContext)
        }
    }

    /**
     * Returns a new source from specified file path.
     *
     * @param file specified file path
     * @param optional whether this source is optional
     * @return a new source from specified file path
     */
    fun fromFile(file: String, optional: Boolean = false): Source = fromFile(File(file), optional)

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
     * @param optional whether this source is optional
     * @return a new source from specified url
     */
    fun fromUrl(url: URL, optional: Boolean = false): Source {
        // from com.fasterxml.jackson.core.JsonFactory._optimizedStreamFromURL in version 2.8.9
        val extendContext: Source.() -> Unit = {
            addContext("url", url.toString())
        }
        if (url.protocol == "file") {
            val host = url.host
            if (host == null || host.isEmpty()) {
                val path = url.path
                if (path.indexOf('%') < 0) {
                    val file = File(path)
                    if (!file.exists() && optional) {
                        return EmptyMapSource.apply(extendContext)
                    }
                    return fromInputStream(FileInputStream(file)).apply(extendContext)
                }
            }
        }
        return try {
            val stream = url.openStream()
            fromInputStream(stream).apply(extendContext)
        } catch (ex: IOException) {
            if (optional) {
                EmptyMapSource.apply(extendContext)
            } else {
                throw ex
            }
        }
    }

    /**
     * Returns a new source from specified url string.
     *
     * @param url specified url string
     * @param optional whether this source is optional
     * @return a new source from specified url string
     */
    fun fromUrl(url: String, optional: Boolean = false): Source = fromUrl(URL(url), optional)

    /**
     * Returns a new source from specified resource.
     *
     * @param resource path of specified resource
     * @param optional whether this source is optional
     * @return a new source from specified resource
     */
    fun fromResource(resource: String, optional: Boolean = false): Source {
        val extendContext: Source.() -> Unit = {
            addContext("resource", resource)
        }
        val loader = Thread.currentThread().contextClassLoader
        val e = try {
            loader.getResources(resource)
        } catch (ex: IOException) {
            if (optional) {
                return EmptyMapSource.apply(extendContext)
            } else {
                throw ex
            }
        }
        if (!e.hasMoreElements()) {
            if (optional) {
                return EmptyMapSource.apply(extendContext)
            } else {
                throw SourceNotFoundException("resource not found on classpath: $resource")
            }
        }
        val sources = mutableListOf<Source>()
        while (e.hasMoreElements()) {
            val url = e.nextElement()
            val source = fromUrl(url, optional)
            sources.add(source)
        }

        return sources.reduce(Source::withFallback).apply(extendContext)
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

            override fun fromFile(file: File, optional: Boolean): Source =
                this@Provider.fromFile(file, optional).let(transform)

            override fun fromFile(file: String, optional: Boolean): Source =
                this@Provider.fromFile(file, optional).let(transform)

            override fun fromString(content: String): Source =
                this@Provider.fromString(content).let(transform)

            override fun fromBytes(content: ByteArray): Source =
                this@Provider.fromBytes(content).let(transform)

            override fun fromBytes(content: ByteArray, offset: Int, length: Int): Source =
                this@Provider.fromBytes(content, offset, length).let(transform)

            override fun fromUrl(url: URL, optional: Boolean): Source =
                this@Provider.fromUrl(url, optional).let(transform)

            override fun fromUrl(url: String, optional: Boolean): Source =
                this@Provider.fromUrl(url, optional).let(transform)

            override fun fromResource(resource: String, optional: Boolean): Source =
                this@Provider.fromResource(resource, optional).let(transform)
        }
    }

    companion object {
        private val extensionToProvider = mutableMapOf(
            "json" to JsonProvider,
            "properties" to PropertiesProvider
        )

        init {
            val reflections = Reflections("")
            val providers = reflections.getSubTypesOf(Provider::class.java)
                .intersect(reflections.getTypesAnnotatedWith(RegisterExtension::class.java))
            for (provider in providers) {
                for (annotation in ReflectionUtils.getAnnotations(provider).filter {
                    it.annotationClass == RegisterExtension::class
                }) {
                    for (extension in (annotation as RegisterExtension).value) {
                        registerExtension(extension, provider.kotlin.objectInstance!! as Provider)
                    }
                }
            }
        }

        /**
         * Register extension with the corresponding provider.
         *
         * @param extension the file extension
         * @param provider the corresponding provider
         */
        fun registerExtension(extension: String, provider: Provider) {
            extensionToProvider[extension] = provider
        }

        /**
         * Unregister the given extension.
         *
         * @param extension the file extension
         */
        fun unregisterExtension(extension: String): Provider? =
            extensionToProvider.remove(extension)

        /**
         * Returns corresponding provider based on extension.
         *
         * Returns null if the specific extension is unregistered.
         *
         * @param extension the file extension
         * @return the corresponding provider based on extension
         */
        fun of(extension: String): Provider? =
            extensionToProvider[extension]
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RegisterExtension(val value: Array<String>)
