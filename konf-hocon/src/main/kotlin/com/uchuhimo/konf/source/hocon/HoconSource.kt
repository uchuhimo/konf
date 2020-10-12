/*
 * Copyright 2017-2020 the original author or authors.
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

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import com.uchuhimo.konf.ContainerNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.source.ListSourceNode
import com.uchuhimo.konf.source.NullSourceNode
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.ValueSourceNode

private fun ConfigValue.toTree(): TreeNode {
    return when (valueType()!!) {
        ConfigValueType.NULL -> NullSourceNode
        ConfigValueType.BOOLEAN, ConfigValueType.NUMBER, ConfigValueType.STRING -> ValueSourceNode(unwrapped())
        ConfigValueType.LIST -> ListSourceNode(
            mutableListOf<TreeNode>().apply {
                for (value in (this@toTree as ConfigList)) {
                    add(value.toTree())
                }
            }
        )
        ConfigValueType.OBJECT -> ContainerNode(
            mutableMapOf<String, TreeNode>().apply {
                for ((key, value) in (this@toTree as ConfigObject)) {
                    put(key, value.toTree())
                }
            }
        )
    }
}

/**
 * Source from a HOCON value.
 */
class HoconSource(
    val value: Config
) : Source {
    override val info: SourceInfo = SourceInfo("type" to "HOCON")

    override val tree: TreeNode = value.root().toTree()
}
