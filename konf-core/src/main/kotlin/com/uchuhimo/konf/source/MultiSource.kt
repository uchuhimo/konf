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

package com.uchuhimo.konf.source

import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.TreeNode

class MultiSource(val sources: Collection<Source>) : Source {
    override val info: SourceInfo
        get() = SourceInfo(
            *sources.mapIndexed { index, source -> "source{$index" to source.description }.toTypedArray()
        )

    override val tree: TreeNode
        get() {
            throw NotImplementedError()
        }

    override val features: Map<Feature, Boolean>
        get() {
            throw NotImplementedError()
        }

    override fun substituted(root: Source, enabled: Boolean, errorWhenUndefined: Boolean): Source {
        throw NotImplementedError()
    }

    override fun lowercased(enabled: Boolean): Source {
        throw NotImplementedError()
    }

    override fun littleCamelCased(enabled: Boolean): Source {
        throw NotImplementedError()
    }

    override fun normalized(lowercased: Boolean, littleCamelCased: Boolean): Source {
        throw NotImplementedError()
    }

    override fun getNodeOrNull(path: Path, lowercased: Boolean, littleCamelCased: Boolean): TreeNode? {
        val nodes = sources.map {
            it.getNodeOrNull(path, lowercased, littleCamelCased)
        }.filterNotNull()
        if (nodes.isEmpty()) {
            return null
        } else {
            return nodes.reduce { acc, node -> acc.withFallback(node) }
        }
    }
}
