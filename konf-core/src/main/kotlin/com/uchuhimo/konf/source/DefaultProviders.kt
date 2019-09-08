/*
 * Copyright 2017-2019 the original author or authors.
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

import com.uchuhimo.konf.source.base.FlatSource
import com.uchuhimo.konf.source.base.KVSource
import com.uchuhimo.konf.source.base.MapSource
import com.uchuhimo.konf.source.env.EnvProvider
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import java.io.File
import java.net.URL

/**
 * Default providers.
 */
object DefaultProviders {
    /**
     * Provider for JSON source.
     */
    @JvmField
    val json = JsonProvider

    /**
     * Provider for properties source.
     */
    @JvmField
    val properties = PropertiesProvider

    /**
     * Provider for map source.
     */
    @JvmField
    val map = DefaultMapProviders

    /**
     * Returns a source from system environment.
     *
     * @return a source from system environment
     */
    fun env(): Source = EnvProvider.env()

    /**
     * Returns a source from system properties.
     *
     * @return a source from system properties
     */
    fun systemProperties(): Source = PropertiesProvider.system()

    /**
     * Returns corresponding provider based on extension.
     *
     * @param extension the file extension
     * @param source the source description for error message
     * @return the corresponding provider based on extension
     */
    fun dispatchExtension(extension: String, source: String = ""): Provider =
        Provider.of(extension) ?: throw UnsupportedExtensionException(source)

    /**
     * Returns a source from specified file.
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
     * @return a source from specified file
     * @throws UnsupportedExtensionException
     */
    fun file(file: File, optional: Boolean = false): Source = dispatchExtension(file.extension, file.name).file(file, optional)

    /**
     * Returns a source from specified file path.
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
     * @return a source from specified file path
     * @throws UnsupportedExtensionException
     */
    fun file(file: String, optional: Boolean = false): Source = file(File(file), optional)

    /**
     * Returns a source from specified url.
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
     * @return a source from specified url
     * @throws UnsupportedExtensionException
     */
    fun url(url: URL, optional: Boolean = false): Source =
        dispatchExtension(File(url.path).extension, url.toString()).url(url, optional)

    /**
     * Returns a source from specified url string.
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
     * @return a source from specified url string
     * @throws UnsupportedExtensionException
     */
    fun url(url: String, optional: Boolean = false): Source = url(URL(url), optional)
}

/**
 * Providers for map of variant formats.
 */
object DefaultMapProviders {
    /**
     * Returns a source from specified hierarchical map.
     *
     * @param map a hierarchical map
     * @return a source from specified hierarchical map
     */
    fun hierarchical(map: Map<String, Any>): Source = MapSource(map)

    /**
     * Returns a source from specified map in key-value format.
     *
     * @param map a map in key-value format
     * @return a source from specified map in key-value format
     */
    fun kv(map: Map<String, Any>): Source = KVSource(map)

    /**
     * Returns a source from specified map in flat format.
     *
     * @param map a map in flat format
     * @return a source from specified map in flat format
     */
    fun flat(map: Map<String, String>): Source = FlatSource(map)
}
