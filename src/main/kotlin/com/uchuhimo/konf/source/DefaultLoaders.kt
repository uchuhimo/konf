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
import java.io.File

/**
 * Default loaders for config.
 *
 * @param config parent config for loader
 */
class DefaultLoaders(
        /**
         * Parent config for loader.
         */
        val config: Config
) {
    /**
     * Loader for HOCON source.
     */
    @JvmField
    val hocon = Loader(config, HoconProvider)

    /**
     * Loader for JSON source.
     */
    @JvmField
    val json = Loader(config, JsonProvider)

    /**
     * Loader for properties source.
     */
    @JvmField
    val properties = Loader(config, PropertiesProvider)

    /**
     * Loader for TOML source.
     */
    @JvmField
    val toml = Loader(config, TomlProvider)

    /**
     * Loader for XML source.
     */
    @JvmField
    val xml = Loader(config, XmlProvider)

    /**
     * Loader for YAML source.
     */
    @JvmField
    val yaml = Loader(config, YamlProvider)

    /**
     * Loader for map source.
     */
    @JvmField
    val map = MapLoader(config)

    /**
     * Returns a child config containing values from system environment.
     *
     * @return a child config containing values from system environment
     */
    fun env(): Config = config.withSource(EnvProvider.fromEnv())

    /**
     * Returns a child config containing values from system properties.
     *
     * @return a child config containing values from system properties
     */
    fun systemProperties(): Config = config.withSource(PropertiesProvider.fromSystem())

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
    fun file(file: File): Config {
        return when (file.extension) {
            "conf" -> hocon.file(file)
            "json" -> json.file(file)
            "properties" -> properties.file(file)
            "toml" -> toml.file(file)
            "xml" -> xml.file(file)
            "yml", "yaml" -> yaml.file(file)
            else -> throw UnsupportedExtensionException(file)
        }
    }
}

/**
 * Loader to load source from map of variant formats.
 *
 * @param config parent config
 */
class MapLoader(
        /**
         * Parent config for all child configs loading source in this loader.
         */
        val config: Config
) {
    /**
     * Returns a child config containing values from specified hierarchical map.
     *
     * @param map a hierarchical map
     * @return a child config containing values from specified hierarchical map
     */
    fun hierarchical(map: Map<String, Any>): Config = config.withSource(MapSource(map))

    /**
     * Returns a child config containing values from specified map in key-value format.
     *
     * @param map a map in key-value format
     * @return a child config containing values from specified map in key-value format
     */
    fun kv(map: Map<String, Any>): Config = config.withSource(KVSource(map))

    /**
     * Returns a child config containing values from specified map in flat format.
     *
     * @param map a map in flat format
     * @return a child config containing values from specified map in flat format
     */
    fun flat(map: Map<String, String>): Config = config.withSource(FlatSource(map))
}
