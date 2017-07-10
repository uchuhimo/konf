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

class DefaultLoaders(val config: Config) {
    @JvmField
    val hocon = Loader(config, HoconProvider)

    @JvmField
    val json = Loader(config, JsonProvider)

    @JvmField
    val properties = Loader(config, PropertiesProvider)

    @JvmField
    val toml = Loader(config, TomlProvider)

    @JvmField
    val xml = Loader(config, XmlProvider)

    @JvmField
    val yaml = Loader(config, YamlProvider)

    @JvmField
    val map = MapLoader(config)

    fun env(): Config = config.load(EnvProvider.fromEnv())

    fun systemProperties(): Config = config.load(PropertiesProvider.fromSystem())

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

class MapLoader(val config: Config) {
    fun hierarchical(map: Map<String, Any>): Config = config.load(MapSource(map))

    fun kv(map: Map<String, Any>): Config = config.load(KVSource(map))

    fun flat(map: Map<String, String>): Config = config.load(FlatSource(map))
}
