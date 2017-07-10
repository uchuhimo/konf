package com.uchuhimo.konf.source.yaml

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import com.uchuhimo.konf.source.base.asSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.InputStream
import java.io.Reader

object YamlProvider : SourceProvider {
    override fun fromReader(reader: Reader): Source {
        val yaml = Yaml(SafeConstructor())
        return yaml.load(reader).asSource("YAML")
    }

    override fun fromInputStream(inputStream: InputStream): Source {
        val yaml = Yaml(SafeConstructor())
        return yaml.load(inputStream).asSource("YAML")
    }
}
