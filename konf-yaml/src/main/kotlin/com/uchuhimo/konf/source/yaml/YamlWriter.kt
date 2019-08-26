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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer
import java.io.OutputStream

/**
 * Writer for YAML source.
 */
class YamlWriter(val config: Config) : Writer {
    private val yaml = Yaml(SafeConstructor(), Representer(), DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        lineBreak = DumperOptions.LineBreak.getPlatformLineBreak()
    })

    override fun toWriter(writer: java.io.Writer) {
        yaml.dump(config.toHierarchicalMap(), writer)
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }
}

/**
 * Returns writer for YAML source.
 */
val Config.toYaml: Writer get() = YamlWriter(this)
