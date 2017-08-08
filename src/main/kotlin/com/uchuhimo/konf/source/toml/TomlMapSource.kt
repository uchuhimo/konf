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

package com.uchuhimo.konf.source.toml

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.MapSource

/**
 * Source from a TOML map.
 */
class TomlMapSource(
        map: Map<String, Any>,
        context: Map<String, String> = mapOf()
) : MapSource(map, "TOML", context) {
    override fun Any.castToSource(context: Map<String, String>): Source = asTomlSource(context)
}
