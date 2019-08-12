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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Constants
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Loader to load source from various input formats.
 *
 * @param config parent config
 */
class Loader(
    /**
     * Parent config for all child configs loading source in this loader.
     */
    val config: Config,
    /**
     * Source provider to provide source from various input format.
     */
    val provider: Provider
) {
    private val optional = config.isEnabled(Feature.OPTIONAL_SOURCE_BY_DEFAULT)

    /**
     * Returns a child config containing values from specified reader.
     *
     * @param reader specified reader for reading character streams
     * @return a child config containing values from specified reader
     */
    fun reader(reader: Reader): Config =
        config.withSource(provider.fromReader(reader))

    /**
     * Returns a child config containing values from specified input stream.
     *
     * @param inputStream specified input stream of bytes
     * @return a child config containing values from specified input stream
     */
    fun inputStream(inputStream: InputStream): Config =
        config.withSource(provider.fromInputStream(inputStream))

    /**
     * Returns a child config containing values from specified file.
     *
     * @param file specified file
     * @param optional whether the source is optional
     * @return a child config containing values from specified file
     */
    fun file(file: File, optional: Boolean = this.optional): Config =
        config.withSource(provider.fromFile(file, optional))

    /**
     * Returns a child config containing values from specified file path.
     *
     * @param file specified file path
     * @param optional whether the source is optional
     * @return a child config containing values from specified file path
     */
    fun file(file: String, optional: Boolean = this.optional): Config =
        config.withSource(provider.fromFile(file, optional))

    /**
     * Returns a child config containing values from specified file,
     * and reloads values when file content has been changed.
     *
     * @param file specified file
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @return a child config containing values from watched file
     */
    fun watchFile(
        file: File,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional
    ): Config {
        return provider.fromFile(file, optional).let { source ->
            config.withLoadTrigger("watch ${source.description}") { newConfig, load ->
                load(source)
                val watcher = FileSystems.getDefault().newWatchService()
                val path = file.toPath().parent
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
                GlobalScope.launch(context) {
                    while (true) {
                        delay(unit.toMillis(delayTime))
                        val key = watcher.poll()
                        if (key != null) {
                            for (event in key.pollEvents()) {
                                val kind = event.kind()
                                @Suppress("UNCHECKED_CAST")
                                event as WatchEvent<Path>
                                val filename = event.context()
                                if (kind == StandardWatchEventKinds.OVERFLOW) {
                                    continue
                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY &&
                                    filename.toString() == file.name) {
                                    newConfig.lock {
                                        newConfig.clear()
                                        load(provider.fromFile(file, optional))
                                    }
                                }
                                val valid = key.reset()
                                if (!valid) {
                                    watcher.close()
                                    throw InvalidWatchKeyException(source)
                                }
                            }
                        }
                    }
                }
            }.withLayer()
        }
    }

    /**
     * Returns a child config containing values from specified file path,
     * and reloads values when file content has been changed.
     *
     * @param file specified file path
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @return a child config containing values from watched file
     */
    fun watchFile(
        file: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional
    ): Config =
        watchFile(File(file), delayTime, unit, context, optional)

    /**
     * Returns a child config containing values from specified string.
     *
     * @param content specified string
     * @return a child config containing values from specified string
     */
    fun string(content: String): Config =
        config.withSource(provider.fromString(content))

    /**
     * Returns a child config containing values from specified byte array.
     *
     * @param content specified byte array
     * @return a child config containing values from specified byte array
     */
    fun bytes(content: ByteArray): Config =
        config.withSource(provider.fromBytes(content))

    /**
     * Returns a child config containing values from specified portion of byte array.
     *
     * @param content specified byte array
     * @param offset the start offset of the portion of the array to read
     * @param length the length of the portion of the array to read
     * @return a child config containing values from specified portion of byte array
     */
    fun bytes(content: ByteArray, offset: Int, length: Int): Config =
        config.withSource(provider.fromBytes(content, offset, length))

    /**
     * Returns a child config containing values from specified url.
     *
     * @param url specified url
     * @param optional whether the source is optional
     * @return a child config containing values from specified url
     */
    fun url(url: URL, optional: Boolean = this.optional): Config =
        config.withSource(provider.fromUrl(url, optional))

    /**
     * Returns a child config containing values from specified url string.
     *
     * @param url specified url string
     * @param optional whether the source is optional
     * @return a child config containing values from specified url string
     */
    fun url(url: String, optional: Boolean = this.optional): Config =
        config.withSource(provider.fromUrl(url, optional))

    /**
     * Returns a child config containing values from specified url,
     * and reloads values periodically.
     *
     * @param url specified url
     * @param period reload period. The default value is 5.
     * @param unit time unit of reload period. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @return a child config containing values from specified url
     */
    fun watchUrl(
        url: URL,
        period: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional
    ): Config {
        return provider.fromUrl(url, optional).let { source ->
            config.withLoadTrigger("watch ${source.description}") { newConfig, load ->
                load(source)
                GlobalScope.launch(context) {
                    while (true) {
                        delay(unit.toMillis(period))
                        newConfig.lock {
                            newConfig.clear()
                            load(provider.fromUrl(url, optional))
                        }
                    }
                }
            }.withLayer()
        }
    }

    /**
     * Returns a child config containing values from specified url string,
     * and reloads values periodically.
     *
     * @param url specified url string
     * @param period reload period. The default value is 5.
     * @param unit time unit of reload period. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @return a child config containing values from specified url string
     */
    fun watchUrl(
        url: String,
        period: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional
    ): Config =
        watchUrl(URL(url), period, unit, context, optional)

    /**
     * Returns a child config containing values from specified resource.
     *
     * @param resource path of specified resource
     * @param optional whether the source is optional
     * @return a child config containing values from specified resource
     */
    fun resource(resource: String, optional: Boolean = this.optional): Config =
        config.withSource(provider.fromResource(resource, optional))

    /**
     * Returns a child config containing values from a specified git repository.
     *
     * @param repo git repository
     * @param file file in the git repository
     * @param dir local directory of the git repository
     * @param branch the initial branch
     * @param optional whether the source is optional
     * @param action additional action when cloning/pulling
     * @return a child config containing values from a specified git repository
     */
    fun git(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        optional: Boolean = this.optional,
        action: TransportCommand<*, *>.() -> Unit = {}
    ): Config =
        config.withSource(provider.fromGit(repo, file, dir, branch, optional, action))

    /**
     * Returns a child config containing values from a specified git repository,
     * and reloads values periodically.
     *
     * @param repo git repository
     * @param file file in the git repository
     * @param dir local directory of the git repository
     * @param branch the initial branch
     * @param period reload period. The default value is 1.
     * @param unit time unit of reload period. The default value is [TimeUnit.MINUTES].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @param action additional action when cloning/pulling
     * @return a child config containing values from a specified git repository
     */
    fun watchGit(
        repo: String,
        file: String,
        dir: String? = null,
        branch: String = Constants.HEAD,
        period: Long = 1,
        unit: TimeUnit = TimeUnit.MINUTES,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional,
        action: TransportCommand<*, *>.() -> Unit = {}
    ): Config {
        return (dir ?: createTempDir(prefix = "local_git_repo").path).let { directory ->
            provider.fromGit(repo, file, directory, branch, optional, action).let { source ->
                config.withLoadTrigger("watch ${source.description}") { newConfig, load ->
                    load(source)
                    GlobalScope.launch(context) {
                        while (true) {
                            delay(unit.toMillis(period))
                            newConfig.lock {
                                newConfig.clear()
                                load(provider.fromGit(repo, file, directory, branch, optional, action))
                            }
                        }
                    }
                }.withLayer()
            }
        }
    }
}
