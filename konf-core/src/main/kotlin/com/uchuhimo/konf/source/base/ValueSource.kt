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

import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.asTree

/**
 * Source from a single value.
 */
open class ValueSource(
    val value: Any,
    type: String = "",
    final override val info: SourceInfo = SourceInfo()
) : Source {
    init {
        info["type"] = type.notEmptyOr("value")
    }

    override val tree: TreeNode = value.asTree()
}
