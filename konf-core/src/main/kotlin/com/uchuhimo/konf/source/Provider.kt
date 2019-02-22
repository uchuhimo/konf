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

import com.uchuhimo.konf.source.hocon.HoconProvider
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import com.uchuhimo.konf.source.xml.XmlProvider
import com.uchuhimo.konf.source.yaml.YamlProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.URIish
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.net.URL
import java.nio.file.Paths

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
    fun fromFile(file: File): Source {
        return file.inputStream().buffered().use { inputStream ->
            fromInputStream(inputStream).apply {
                addContext("file", file.toString())
            }
        }
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
     * Returns a new source from a specified git repository.
     *
     * @param repo git repository
     * @param file file in the git repository
     * @param dir local directory of the git repository
     * @param branch the initial branch
     * @param action additional action when cloning/pulling
     * @return a new source from a specified git repository
     */
    fun fromGit(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        action: TransportCommand<*, *>.() -> Unit = {}
    ): Source {
        return (dir?.let(::File) ?: createTempDir(prefix = "local_git_repo")).let { directory ->
            if (directory.list { _, name -> name == ".git" }.isEmpty()) {
                Git.cloneRepository().apply {
                    setURI(repo)
                    setDirectory(directory)
                    setBranch(branch)
                    this.action()
                }.call().close()
            } else {
                Git.open(directory).use { git ->
                    val uri = URIish(repo)
                    val remoteName = git.remoteList().call().firstOrNull { it.urIs.contains(uri) }?.name
                        ?: throw InvalidRemoteRepoException(repo, directory.path)
                    git.pull().apply {
                        remote = remoteName
                        remoteBranchName = branch
                        this.action()
                    }.call()
                }
            }
            fromFile(Paths.get(directory.path, file).toFile()).apply {
                addContext("repo", repo)
                addContext("file", file)
                addContext("dir", directory.path)
                addContext("branch", branch)
            }
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

    companion object {
        private val extensionToProvider = mutableMapOf(
            "conf" to HoconProvider,
            "json" to JsonProvider,
            "properties" to PropertiesProvider,
            "xml" to XmlProvider,
            "yml" to YamlProvider,
            "yaml" to YamlProvider
        )

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
