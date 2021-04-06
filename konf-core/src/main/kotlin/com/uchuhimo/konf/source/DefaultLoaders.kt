/*
 * Copyright 2017-2021 the original author or authors.
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
import com.uchuhimo.konf.source.base.FlatSource
import com.uchuhimo.konf.source.base.KVSource
import com.uchuhimo.konf.source.base.MapSource
import com.uchuhimo.konf.source.env.EnvProvider
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

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
    private val transform: ((Source) -> Source)? = null
) {
    val optional = config.isEnabled(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
    fun Provider.orMapped(): Provider =
        if (transform != null) this.map(transform) else this

    fun Source.orMapped(): Source = transform?.invoke(this) ?: this

    /**
     * Returns default loaders applied the given [transform] function.
     *
     * @param transform the given transformation function
     * @return the default loaders applied the given [transform] function
     */
    fun mapped(transform: (Source) -> Source): DefaultLoaders = DefaultLoaders(config) {
        transform(it.orMapped())
    }

    /**
     * Returns default loaders where sources have specified additional prefix.
     *
     * @param prefix additional prefix
     * @return the default loaders where sources have specified additional prefix
     */
    fun prefixed(prefix: String): DefaultLoaders = mapped { it.withPrefix(prefix) }

    /**
     * Returns default loaders where sources are scoped in specified path.
     *
     * @param path path that is the scope of sources
     * @return the default loaders where sources are scoped in specified path
     */
    fun scoped(path: String): DefaultLoaders = mapped { it[path] }

    fun enabled(feature: Feature): DefaultLoaders = mapped { it.enabled(feature) }

    fun disabled(feature: Feature): DefaultLoaders = mapped { it.disabled(feature) }

    /**
     * Loader for JSON source.
     */
    @JvmField
    val json = Loader(config, JsonProvider.orMapped())

    /**
     * Loader for properties source.
     */
    @JvmField
    val properties = Loader(config, PropertiesProvider.orMapped())

    /**
     * Loader for map source.
     */
    @JvmField
    val map = MapLoader(config, transform)

    /**
     * Loader for a source from the specified provider.
     *
     * @param provider the specified provider
     * @return a loader for a source from the specified provider
     */
    fun source(provider: Provider) = Loader(config, provider.orMapped())

    /**
     * Returns a child config containing values from system environment.
     *
     * @param nested whether to treat "AA_BB_CC" as nested format "AA.BB.CC" or not. True by default.
     * @return a child config containing values from system environment
     */
    @JvmOverloads
    fun env(nested: Boolean = true): Config = config.withSource(EnvProvider.env(nested).orMapped())

    @JvmOverloads
    fun envMap(map: Map<String, String>, nested: Boolean = true): Config = config.withSource(EnvProvider.envMap(map, nested).orMapped())

    /**
     * Returns a child config containing values from system properties.
     *
     * @return a child config containing values from system properties
     */
    fun systemProperties(): Config = config.withSource(PropertiesProvider.system().orMapped())

    /**
     * Returns corresponding loader based on extension.
     *
     * @param extension the file extension
     * @param source the source description for error message
     * @return the corresponding loader based on extension
     */
    fun dispatchExtension(extension: String, source: String = ""): Loader =
        Loader(
            config,
            Provider.of(extension)?.orMapped()
                ?: throw UnsupportedExtensionException(source)
        )

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
     * @param optional whether the source is optional
     * @return a child config containing values from specified file
     * @throws UnsupportedExtensionException
     */
    fun file(file: File, optional: Boolean = this.optional): Config = dispatchExtension(file.extension, file.name).file(file, optional)

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
     * @param optional whether the source is optional
     * @return a child config containing values from specified file path
     * @throws UnsupportedExtensionException
     */
    fun file(file: String, optional: Boolean = this.optional): Config = file(File(file), optional)

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
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @param onLoad function invoked after the updated file is loaded
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: File,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional,
        onLoad: (Source.(Config) -> Unit)? = null
    ): Config = dispatchExtension(file.extension, file.name)
        .watchFile(file, delayTime, unit, context, optional, onLoad)

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
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @param onLoad function invoked after the updated file is loaded
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional,
        onLoad: (Source.(Config) -> Unit)? = null
    ): Config = watchFile(File(file), delayTime, unit, context, optional, onLoad)

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
     * @param optional whether the source is optional
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun url(url: URL, optional: Boolean = this.optional): Config = dispatchExtension(File(url.path).extension, url.toString()).url(url, optional)

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
     * @param optional whether the source is optional
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun url(url: String, optional: Boolean = this.optional): Config = url(URL(url), optional)

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
     * @param period reload period. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @param onLoad function invoked after the updated URL is loaded
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: URL,
        period: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional,
        onLoad: (Source.(Config) -> Unit)? = null
    ): Config = dispatchExtension(File(url.path).extension, url.toString())
        .watchUrl(url, period, unit, context, optional, onLoad)

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
     * @param period reload period. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [Dispatchers.Default].
     * @param optional whether the source is optional
     * @param onLoad function invoked after the updated URL is loaded
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: String,
        period: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default,
        optional: Boolean = this.optional,
        onLoad: (Source.(Config) -> Unit)? = null
    ): Config = watchUrl(URL(url), period, unit, context, optional, onLoad)
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
    private val transform: ((Source) -> Source)? = null
) {
    fun Source.orMapped(): Source = transform?.invoke(this) ?: this

    /**
     * Returns a child config containing values from specified hierarchical map.
     *
     * @param map a hierarchical map
     * @return a child config containing values from specified hierarchical map
     */
    fun hierarchical(map: Map<String, Any>): Config = config.withSource(MapSource(map).orMapped())

    /**
     * Returns a child config containing values from specified map in key-value format.
     *
     * @param map a map in key-value format
     * @return a child config containing values from specified map in key-value format
     */
    fun kv(map: Map<String, Any>): Config = config.withSource(KVSource(map).orMapped())

    /**
     * Returns a child config containing values from specified map in flat format.
     *
     * @param map a map in flat format
     * @return a child config containing values from specified map in flat format
     */
    fun flat(map: Map<String, String>): Config = config.withSource(FlatSource(map).orMapped())
}
