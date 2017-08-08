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
import com.uchuhimo.konf.name
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.Source

/**
 * Source from a map in key-value format.
 */
open class KVSource(
        val map: Map<String, Any>,
        type: String = "",
        context: Map<String, String> = mapOf()
) : ValueSource(map, type.notEmptyOr("KV"), context) {
    override fun contains(path: Path): Boolean = map.contains(path.name)

    override fun getOrNull(path: Path): Source? = map[path.name]?.castToSource(context)
}

fun Map<String, Any>.asKVSource() = KVSource(this)
