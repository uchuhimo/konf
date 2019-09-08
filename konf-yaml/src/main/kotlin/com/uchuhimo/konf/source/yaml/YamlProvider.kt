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

package com.uchuhimo.konf.source.yaml

import com.uchuhimo.konf.source.Provider
import com.uchuhimo.konf.source.RegisterExtension
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.asSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import java.io.InputStream
import java.io.Reader

/**
 * Provider for YAML source.
 */
@RegisterExtension(["yml", "yaml"])
object YamlProvider : Provider {
    override fun reader(reader: Reader): Source {
        val yaml = Yaml(YamlConstructor())
        return yaml.load<Any>(reader).asSource("YAML")
    }

    override fun inputStream(inputStream: InputStream): Source {
        val yaml = Yaml(YamlConstructor())
        return yaml.load<Any>(inputStream).asSource("YAML")
    }
}

class YamlConstructor : SafeConstructor() {
    init {
        yamlConstructors[Tag.NULL] = object : AbstractConstruct() {
            override fun construct(node: Node): Any? {
                constructScalar(node as ScalarNode)
                return "null"
            }
        }
    }
}
