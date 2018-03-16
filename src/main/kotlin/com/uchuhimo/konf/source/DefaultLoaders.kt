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
import com.uchuhimo.konf.source.base.FlatSource
import com.uchuhimo.konf.source.base.KVSource
import com.uchuhimo.konf.source.base.MapSource
import com.uchuhimo.konf.source.env.EnvProvider
import com.uchuhimo.konf.source.hocon.HoconProvider
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import com.uchuhimo.konf.source.toml.TomlProvider
import com.uchuhimo.konf.source.xml.XmlProvider
import com.uchuhimo.konf.source.yaml.YamlProvider
import kotlinx.coroutines.experimental.DefaultDispatcher
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Default loaders for config.
 *
 * If [transform] is provided, source will be applied the given [transform] function when loaded.
 *
 * @param config parent config for loader
 * @param transform the given transformation function
 */
class DefaultLoaders(
    /**
     * Parent config for loader.
     */
    val config: Config,
    /**
     * The given transformation function.
     */
    val transform: ((Source) -> Source)? = null
) {
    private fun Provider.wrap(): Provider =
        if (transform != null) this.map(transform) else this

    private fun Source.wrap(): Source =
        if (transform != null) transform.invoke(this) else this

    /**
     * Loader for HOCON source.
     */
    @JvmField
    val hocon = Loader(config, HoconProvider.wrap())

    /**
     * Loader for JSON source.
     */
    @JvmField
    val json = Loader(config, JsonProvider.wrap())

    /**
     * Loader for properties source.
     */
    @JvmField
    val properties = Loader(config, PropertiesProvider.wrap())

    /**
     * Loader for TOML source.
     */
    @JvmField
    val toml = Loader(config, TomlProvider.wrap())

    /**
     * Loader for XML source.
     */
    @JvmField
    val xml = Loader(config, XmlProvider.wrap())

    /**
     * Loader for YAML source.
     */
    @JvmField
    val yaml = Loader(config, YamlProvider.wrap())

    /**
     * Loader for map source.
     */
    @JvmField
    val map = MapLoader(config, transform)

    /**
     * Returns a child config containing values from system environment.
     *
     * @return a child config containing values from system environment
     */
    fun env(): Config = config.withSource(EnvProvider.fromEnv().wrap())

    /**
     * Returns a child config containing values from system properties.
     *
     * @return a child config containing values from system properties
     */
    fun systemProperties(): Config = config.withSource(PropertiesProvider.fromSystem().wrap())

    /**
     * Returns corresponding loader based on extension.
     *
     * @param extension the file extension
     * @param source the source description for error message
     * @return the corresponding loader based on extension
     */
    fun dispatchExtension(extension: String, source: String = ""): Loader =
        Loader(config, Provider.of(extension)?.wrap()
            ?: throw UnsupportedExtensionException(source))

    /**
     * Returns a child config containing values from specified file.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file
     * @return a child config containing values from specified file
     * @throws UnsupportedExtensionException
     */
    fun file(file: File): Config = dispatchExtension(file.extension, file.name).file(file)

    /**
     * Returns a child config containing values from specified file path.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file path
     * @return a child config containing values from specified file path
     * @throws UnsupportedExtensionException
     */
    fun file(file: String): Config = file(File(file))

    /**
     * Returns a child config containing values from specified file,
     * and reloads values when file content has been changed.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: File,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = DefaultDispatcher
    ): Config = dispatchExtension(file.extension, file.name)
        .watchFile(file, delayTime, unit, context)

    /**
     * Returns a child config containing values from specified file path,
     * and reloads values when file content has been changed.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file path
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = DefaultDispatcher
    ): Config = watchFile(File(file), delayTime, unit, context)

    /**
     * Returns a child config containing values from specified url.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun url(url: URL): Config = dispatchExtension(File(url.path).extension, url.toString()).url(url)

    /**
     * Returns a child config containing values from specified url string.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url string
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun url(url: String): Config = url(URL(url))

    /**
     * Returns a child config containing values from specified url,
     * and reloads values periodically.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: URL,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = DefaultDispatcher
    ): Config = dispatchExtension(File(url.path).extension, url.toString())
        .watchUrl(url, delayTime, unit, context)

    /**
     * Returns a child config containing values from specified url string,
     * and reloads values periodically.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     * - TOML: toml
     * - XML: xml
     * - YAML: yml, yaml
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url string
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = DefaultDispatcher
    ): Config = watchUrl(URL(url), delayTime, unit, context)
}

/**
 * Loader to load source from map of variant formats.
 *
 * If [transform] is provided, source will be applied the given [transform] function when loaded.
 *
 * @param config parent config
 */
class MapLoader(
    /**
     * Parent config for all child configs loading source in this loader.
     */
    val config: Config,
    /**
     * The given transformation function.
     */
    val transform: ((Source) -> Source)? = null
) {
    private fun Source.wrap(): Source =
        if (transform != null) transform.invoke(this) else this

    /**
     * Returns a child config containing values from specified hierarchical map.
     *
     * @param map a hierarchical map
     * @return a child config containing values from specified hierarchical map
     */
    fun hierarchical(map: Map<String, Any>): Config = config.withSource(MapSource(map).wrap())

    /**
     * Returns a child config containing values from specified map in key-value format.
     *
     * @param map a map in key-value format
     * @return a child config containing values from specified map in key-value format
     */
    fun kv(map: Map<String, Any>): Config = config.withSource(KVSource(map).wrap())

    /**
     * Returns a child config containing values from specified map in flat format.
     *
     * @param map a map in flat format
     * @return a child config containing values from specified map in flat format
     */
    fun flat(map: Map<String, String>): Config = config.withSource(FlatSource(map).wrap())
}
