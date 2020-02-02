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

package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ContainerNode
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.PathConflictException
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.ListSourceNode
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.SubstitutableNode
import com.uchuhimo.konf.source.ValueSourceNode
import com.uchuhimo.konf.source.asTree
import java.util.Collections

/**
 * Source from a map in flat format.
 */
open class FlatSource(
    val map: Map<String, String>,
    type: String = "",
    final override val info: SourceInfo = SourceInfo(),
    private val allowConflict: Boolean = false
) : Source {
    init {
        info["type"] = type.notEmptyOr("flat")
    }

    override val tree: TreeNode = ContainerNode(mutableMapOf()).apply {
        map.forEach { (path, value) ->
            try {
                set(path, value.asTree())
            } catch (ex: PathConflictException) {
                if (!allowConflict) {
                    throw ex
                }
            }
        }
    }.promoteToList()
}

object EmptyStringNode : SubstitutableNode, ListNode {
    override val value: Any = ""
    override val list: List<TreeNode> = listOf()
    override val originalValue: Any? = null
    override val substituted: Boolean = false
    override var comments: String = ""
    override fun substitute(value: String): TreeNode {
        check(value.isEmpty())
        return this
    }
}

class SingleStringListNode(
    override val value: String,
    override val substituted: Boolean = false,
    override val originalValue: Any? = null,
    override var comments: String = ""
) : SubstitutableNode, ListNode {
    override val children: MutableMap<String, TreeNode> = Collections.unmodifiableMap(
        mutableMapOf("0" to value.asTree()))
    override val list: List<TreeNode> = listOf(value.asTree())
    override fun substitute(value: String): TreeNode = value.promoteToList(true, originalValue
        ?: this.value)
}

class ListStringNode(
    override val value: String,
    override val substituted: Boolean = false,
    override val originalValue: Any? = null,
    override var comments: String = ""
) : ListSourceNode(value.split(',').map { ValueSourceNode(it) }, comments = comments), SubstitutableNode {
    override fun substitute(value: String): TreeNode =
        value.promoteToList(true, originalValue ?: this.value)

    override val children: MutableMap<String, TreeNode> get() = super<ListSourceNode>.children
}

fun String.promoteToList(substitute: Boolean = false, originalValue: Any? = null): TreeNode {
    return when {
        ',' in this -> ListStringNode(this, substitute, originalValue)
        this == "" -> EmptyStringNode
        else -> SingleStringListNode(this, substitute, originalValue)
    }
}

fun ContainerNode.promoteToList(): TreeNode {
    for ((key, child) in children) {
        if (child is ContainerNode) {
            children[key] = child.promoteToList()
        } else if (child is ValueNode) {
            val value = child.value
            if (value is String) {
                children[key] = value.promoteToList()
            }
        }
    }
    val list = generateSequence(0) { it + 1 }.map {
        val key = it.toString()
        if (key in children) key else null
    }.takeWhile {
        it != null
    }.filterNotNull().toList()
    if (list.isNotEmpty() && list.toSet() == children.keys) {
        return ListSourceNode(list.map { children[it]!! })
    } else {
        return this
    }
}

/**
 * Returns a map in flat format for this config.
 *
 * The returned map contains all items in this config.
 * This map can be loaded into config as [com.uchuhimo.konf.source.base.FlatSource] using
 * `config.from.map.flat(map)`.
 */
fun Config.toFlatMap(): Map<String, String> {
    fun MutableMap<String, String>.putFlat(key: String, value: Any) {
        when (value) {
            is List<*> -> {
                if (value.isNotEmpty()) {
                    val first = value[0]
                    when (first) {
                        is List<*>, is Map<*, *> ->
                            value.forEachIndexed { index, child ->
                                putFlat("$key.$index", child!!)
                            }
                        else -> {
                            if (value.map { it.toString() }.any { it.contains(',') }) {
                                value.forEachIndexed { index, child ->
                                    putFlat("$key.$index", child!!)
                                }
                            } else {
                                put(key, value.joinToString(","))
                            }
                        }
                    }
                } else {
                    put(key, "")
                }
            }
            is Map<*, *> ->
                value.forEach { (suffix, child) ->
                    putFlat("$key.$suffix", child!!)
                }
            else -> put(key, value.toString())
        }
    }
    return mutableMapOf<String, String>().apply {
        for ((key, value) in this@toFlatMap.toMap()) {
            putFlat(key, value)
        }
    }
}
