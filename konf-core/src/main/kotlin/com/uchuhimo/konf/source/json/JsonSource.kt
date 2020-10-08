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

package com.uchuhimo.konf.source.json

import com.fasterxml.jackson.databind.JsonNode
import com.uchuhimo.konf.ContainerNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.source.ListSourceNode
import com.uchuhimo.konf.source.NullSourceNode
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.ValueSourceNode

/**
 * Source from a JSON node.
 */
class JsonSource(
    val node: JsonNode
) : Source {
    override val info: SourceInfo = SourceInfo("type" to "JSON")

    override val tree: TreeNode = node.toTree()
}

fun JsonNode.toTree(): TreeNode {
    return when {
        isNull -> NullSourceNode
        isBoolean -> ValueSourceNode(booleanValue())
        isNumber -> ValueSourceNode(numberValue())
        isTextual -> ValueSourceNode(textValue())
        isArray -> ListSourceNode(
            mutableListOf<TreeNode>().apply {
                elements().forEach {
                    add(it.toTree())
                }
            }
        )
        isObject -> ContainerNode(
            mutableMapOf<String, TreeNode>().apply {
                for ((key, value) in fields()) {
                    put(key, value.toTree())
                }
            }
        )
        isMissingNode -> ContainerNode(mutableMapOf())
        else -> throw NotImplementedError()
    }
}
