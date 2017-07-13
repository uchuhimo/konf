/*
 * Copyright 2017 the original author or authors.
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

import com.uchuhimo.konf.Path
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toDescription

open class MapSource(
        val map: Map<String, Any>,
        type: String = "",
        context: Map<String, String> = mapOf()
) : ValueSource(map, type.notEmptyOr("map"), context) {
    override fun contains(path: Path): Boolean {
        if (path.isEmpty()) {
            return true
        } else {
            val key = path.first()
            val rest = path.drop(1)
            return map[key]?.castToSource(context)?.contains(rest) ?: false
        }
    }

    override fun getOrNull(path: Path): Source? {
        if (path.isEmpty()) {
            return this
        } else {
            val key = path.first()
            val rest = path.drop(1)
            val result = map[key]
            if (result != null) {
                return result.castToSource(context).getOrNull(rest)
            } else {
                return null
            }
        }
    }

    override fun isMap(): Boolean = true

    override fun toMap(): Map<String, Source> = map.mapValues { (_, value) ->
        value.castToSource(context).apply { addInfo("inMap", this@MapSource.info.toDescription()) }
    }
}
