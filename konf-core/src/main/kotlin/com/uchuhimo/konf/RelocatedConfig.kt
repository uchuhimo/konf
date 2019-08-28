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

package com.uchuhimo.konf

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import java.util.ArrayDeque
import java.util.Deque
import kotlin.concurrent.read

abstract class RelocatedConfig(parent: BaseConfig, name: String = "") : BaseConfig(name, parent, parent.mapper) {
    abstract fun relocateInOrNull(path: Path): Path?

    fun relocateInOrNull(path: String): String? = relocateInOrNull(path.toPath())?.name

    abstract fun relocateOutOrNull(name: Path): Path?

    fun relocateOutOrNull(name: String): String? = relocateOutOrNull(name.toPath())?.name

    override fun contains(name: String): Boolean {
        return if (containsInLayer(name)) {
            true
        } else {
            val relocatedName = relocateInOrNull(name)
            if (relocatedName != null) {
                parent?.contains(relocatedName) ?: false
            } else {
                false
            }
        }
    }

    override fun contains(path: Path): Boolean =
        containsInLayer(path) || relocateInOrNull(path)?.let { parent?.contains(it) } ?: false

    override fun getItemOrNull(name: String): Item<*>? {
        val item = getItemInLayerOrNull(name)
        return item ?: relocateInOrNull(name)?.let { parent?.getItemOrNull(it) }
    }

    override val itemWithNames: List<Pair<Item<*>, String>>
        get() = nameByItem.entries.map { it.toPair() } +
            (parent?.itemWithNames ?: listOf()).mapNotNull { (item, name) ->
                relocateOutOrNull(name)?.let { item to it }
            }

    override fun nameOf(item: Item<*>): String {
        val name = lock.read { nameByItem[item] }
        return name ?: parent?.nameOf(item)?.let { relocateOutOrNull(it) }
        ?: throw NoSuchItemException(item)
    }

    fun Source.relocated(): Source {
        return object : Source {
            override val info: SourceInfo = SourceInfo(
                "type" to "relocated",
                "source" to this@relocated.description
            )

            override fun contains(path: Path): Boolean =
                relocateInOrNull(path)?.let { this@relocated.contains(it) } ?: false

            override fun getOrNull(path: Path): Source? =
                relocateInOrNull(path)?.let { this@relocated.getOrNull(it) }
        }
    }

    override val sources: Deque<Source>
        get() {
            return lock.read { ArrayDeque(listOf(source)) }.apply {
                for (source in parent?.sources?.mapTo(ArrayDeque()) {
                    it.relocated()
                } ?: ArrayDeque<Source>()) {
                    addLast(source)
                }
            }
        }

    override fun clear() {
        parent?.clear()
    }
}

open class DrillDownConfig(val prefix: String, parent: BaseConfig, name: String = "") : RelocatedConfig(parent, name) {
    init {
        checkPath(prefix)
    }

    override fun relocateInOrNull(path: Path): Path? = relocateInOrNull(prefix.toPath(), path)

    private fun relocateInOrNull(prefix: Path, path: Path): Path? = prefix + path

    override fun relocateOutOrNull(name: Path): Path? =
        relocateOutOrNull(prefix.toPath(), name)

    private fun relocateOutOrNull(prefix: Path, name: Path): Path? {
        return if (name.size >= prefix.size && name.subList(0, prefix.size) == prefix) {
            name.subList(prefix.size, name.size)
        } else {
            null
        }
    }
}

open class RollUpConfig(val prefix: String, parent: BaseConfig, name: String = "") : RelocatedConfig(parent, name) {
    init {
        checkPath(prefix)
    }

    override fun relocateOutOrNull(name: Path): Path? = relocateOutOrNull(prefix.toPath(), name)

    private fun relocateOutOrNull(prefix: Path, name: Path): Path? = prefix + name

    override fun relocateInOrNull(path: Path): Path? =
        relocateInOrNull(prefix.toPath(), path)

    private fun relocateInOrNull(prefix: Path, path: Path): Path? {
        return if (path.size >= prefix.size && path.subList(0, prefix.size) == prefix) {
            path.subList(prefix.size, path.size)
        } else {
            null
        }
    }
}
