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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.name
import com.uchuhimo.konf.source.base.asKVSource
import com.uchuhimo.konf.source.base.asSource
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object FallbackSourceSpec : Spek({
    given("a source with fallback") {
        it("contains fallback info") {
            val fallbackSource = 2.asSource()
            val source = 1.asSource().withFallback(fallbackSource)
            assertThat(source.info["fallback"], equalTo(fallbackSource.description))
        }
        on("path/key is in facade source") {
            val path = listOf("a", "b")
            val key = path.name
            val fallbackSource = mapOf(key to "fallback").asKVSource()
            val facadeSource = mapOf(key to "facade").asKVSource()
            val source = facadeSource.withFallback(fallbackSource)
            it("gets value from facade source") {
                assertTrue(path in source)
                assertTrue(key in source)
                assertThat(source[path].toText(), equalTo(facadeSource[path].toText()))
                assertThat(source[key].toText(), equalTo(facadeSource[key].toText()))
                assertThat(source.getOrNull(path)?.toText(),
                        equalTo(facadeSource.getOrNull(path)?.toText()))
                assertThat(source.getOrNull(key)?.toText(),
                        equalTo(facadeSource.getOrNull(key)?.toText()))
            }
        }
        on("path/key is in fallback source") {
            val path = listOf("a", "b")
            val key = path.name
            val fallbackSource = mapOf(key to "fallback").asKVSource()
            val facadePath = listOf("a", "c")
            val facadeKey = facadePath.name
            val facadeSource = mapOf(facadeKey to "facade").asKVSource()
            val source = facadeSource.withFallback(fallbackSource)
            it("gets value from fallback source") {
                assertTrue(path in source)
                assertTrue(key in source)
                assertThat(source[path].toText(), equalTo(fallbackSource[path].toText()))
                assertThat(source[key].toText(), equalTo(fallbackSource[key].toText()))
                assertThat(source.getOrNull(path)?.toText(),
                        equalTo(fallbackSource.getOrNull(path)?.toText()))
                assertThat(source.getOrNull(key)?.toText(),
                        equalTo(fallbackSource.getOrNull(key)?.toText()))
            }
            it("contains value in facade source") {
                assertTrue(facadePath in source)
                assertTrue(facadeKey in source)
            }
            it("does not contain value which is not existed in both facade source and fallback source") {
                assertFalse("a.d".toPath() in source)
                assertFalse("a.d" in source)
            }
        }
    }
})
