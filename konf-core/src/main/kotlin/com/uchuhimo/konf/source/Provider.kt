/*
 * Copyright 2017-2020 the original author or authors.
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
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

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
    fun reader(reader: Reader): Source

    /**
     * Returns a new source from specified input stream.
     *
     * @param inputStream specified input stream of bytes
     * @return a new source from specified input stream
     */
    fun inputStream(inputStream: InputStream): Source

    /**
     * Returns a new source from specified file.
     *
     * @param file specified file
     * @param optional whether this source is optional
     * @return a new source from specified file
     */
    fun file(file: File, optional: Boolean = false): Source {
        val extendContext: Source.() -> Unit = {
            info["file"] = file.toString()
        }
        if (!file.exists() && optional) {
            return EmptyMapSource().apply(extendContext)
        }
        return file.inputStream().buffered().use { inputStream ->
            inputStream(inputStream).apply(extendContext)
        }
    }

    /**
     * Returns a new source from specified file path.
     *
     * @param file specified file path
     * @param optional whether this source is optional
     * @return a new source from specified file path
     */
    fun file(file: String, optional: Boolean = false): Source = file(File(file), optional)

    /**
     * Returns a new source from specified string.
     *
     * @param content specified string
     * @return a new source from specified string
     */
    fun string(content: String): Source = reader(content.reader()).apply {
        info["content"] = "\"\n$content\n\""
    }

    /**
     * Returns a new source from specified byte array.
     *
     * @param content specified byte array
     * @return a new source from specified byte array
     */
    fun bytes(content: ByteArray): Source {
        return content.inputStream().use {
            inputStream(it)
        }
    }

    /**
     * Returns a new source from specified portion of byte array.
     *
     * @param content specified byte array
     * @param offset the start offset of the portion of the array to read
     * @param length the length of the portion of the array to read
     * @return a new source from specified portion of byte array
     */
    fun bytes(content: ByteArray, offset: Int, length: Int): Source {
        return content.inputStream(offset, length).use {
            inputStream(it)
        }
    }

    /**
     * Returns a new source from specified url.
     *
     * @param url specified url
     * @param optional whether this source is optional
     * @return a new source from specified url
     */
    fun url(url: URL, optional: Boolean = false): Source {
        // from com.fasterxml.jackson.core.JsonFactory._optimizedStreamFromURL in version 2.8.9
        val extendContext: Source.() -> Unit = {
            info["url"] = url.toString()
        }
        if (url.protocol == "file") {
            val host = url.host
            if (host == null || host.isEmpty()) {
                val path = url.path
                if (path.indexOf('%') < 0) {
                    val file = File(path)
                    if (!file.exists() && optional) {
                        return EmptyMapSource().apply(extendContext)
                    }
                    return file.inputStream().use {
                        inputStream(it).apply(extendContext)
                    }
                }
            }
        }
        return try {
            val stream = url.openStream()
            stream.use {
                inputStream(it).apply(extendContext)
            }
        } catch (ex: IOException) {
            if (optional) {
                EmptyMapSource().apply(extendContext)
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
    fun url(url: String, optional: Boolean = false): Source = url(URL(url), optional)

    /**
     * Returns a new source from specified resource.
     *
     * @param resource path of specified resource
     * @param optional whether this source is optional
     * @return a new source from specified resource
     */
    fun resource(resource: String, optional: Boolean = false): Source {
        val extendContext: Source.() -> Unit = {
            info["resource"] = resource
        }
        val loader = Thread.currentThread().contextClassLoader
        val e = try {
            loader.getResources(resource)
        } catch (ex: IOException) {
            if (optional) {
                return EmptyMapSource().apply(extendContext)
            } else {
                throw ex
            }
        }
        if (!e.hasMoreElements()) {
            if (optional) {
                return EmptyMapSource().apply(extendContext)
            } else {
                throw SourceNotFoundException("resource not found on classpath: $resource")
            }
        }
        val sources = mutableListOf<Source>()
        while (e.hasMoreElements()) {
            val url = e.nextElement()
            val source = url(url, optional)
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
            override fun reader(reader: Reader): Source =
                this@Provider.reader(reader).let(transform)

            override fun inputStream(inputStream: InputStream): Source =
                this@Provider.inputStream(inputStream).let(transform)

            override fun file(file: File, optional: Boolean): Source =
                this@Provider.file(file, optional).let(transform)

            override fun file(file: String, optional: Boolean): Source =
                this@Provider.file(file, optional).let(transform)

            override fun string(content: String): Source =
                this@Provider.string(content).let(transform)

            override fun bytes(content: ByteArray): Source =
                this@Provider.bytes(content).let(transform)

            override fun bytes(content: ByteArray, offset: Int, length: Int): Source =
                this@Provider.bytes(content, offset, length).let(transform)

            override fun url(url: URL, optional: Boolean): Source =
                this@Provider.url(url, optional).let(transform)

            override fun url(url: String, optional: Boolean): Source =
                this@Provider.url(url, optional).let(transform)

            override fun resource(resource: String, optional: Boolean): Source =
                this@Provider.resource(resource, optional).let(transform)
        }
    }

    companion object {
        private val extensionToProvider = ConcurrentHashMap(
            mutableMapOf(
                "json" to JsonProvider,
                "properties" to PropertiesProvider
            )
        )

        init {
            val reflections = Reflections(
                "com.uchuhimo.konf",
                SubTypesScanner(),
                TypeAnnotationsScanner()
            )
            val providers = reflections.getSubTypesOf(Provider::class.java)
                .intersect(reflections.getTypesAnnotatedWith(RegisterExtension::class.java))
            for (provider in providers) {
                for (
                    annotation in ReflectionUtils.getAnnotations(provider).filter {
                        it.annotationClass == RegisterExtension::class
                    }
                ) {
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
