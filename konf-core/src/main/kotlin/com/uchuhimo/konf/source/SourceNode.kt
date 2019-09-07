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

import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.MapNode
import com.uchuhimo.konf.NullNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.emptyMutableMap
import java.util.Collections

class ValueSourceNode(override val value: Any) : ValueNode

object NullSourceNode : NullNode {
    override val children: MutableMap<String, TreeNode> = emptyMutableMap
}

class ListSourceNode(override val list: List<TreeNode>) : ListNode, MapNode {
    override val children: MutableMap<String, TreeNode>
        get() = Collections.unmodifiableMap(
            list.withIndex().associate { (key, value) -> key.toString() to value })
}
