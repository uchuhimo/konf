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

package com.uchuhimo.konf.source.hocon

import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import java.io.OutputStream

/**
 * Writer for HOCON source.
 */
class HoconWriter(val config: Config) : Writer {

    private val renderOpts = ConfigRenderOptions.defaults()
        .setOriginComments(false)
        .setComments(false)
        .setJson(false)

    override fun toWriter(writer: java.io.Writer) {
        writer.write(toText())
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }

    private fun TreeNode.toConfigValue(): ConfigValue {
        val value = when (this) {
            is ValueNode -> ConfigValueFactory.fromAnyRef(value)
            is ListNode -> ConfigValueFactory.fromIterable(list.map { it.toConfigValue() })
            else -> ConfigValueFactory.fromMap(children.mapValues { (_, value) -> value.toConfigValue() })
        }
        val comments = comments
        if (comments != null) {
            return value.withOrigin(value.origin().withComments(comments.split("\n")))
        }
        return value
    }

    override fun toText(): String {
        val output = if (config.isEnabled(Feature.WRITE_DESCRIPTIONS_AS_COMMENTS)) {
            config.toTree().toConfigValue().render(renderOpts.setComments(true))
        } else {
            ConfigValueFactory.fromMap(config.toHierarchicalMap()).render(renderOpts)
        }
        return output.replace("\n", System.lineSeparator())
    }
}

/**
 * Returns writer for HOCON source.
 */
val Config.toHocon: Writer get() = HoconWriter(this)
