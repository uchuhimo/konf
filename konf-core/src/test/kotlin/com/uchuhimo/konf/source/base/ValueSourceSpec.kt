/*
 * Copyright 2017-2021 the original author or authors.
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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.asSource
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ValueSourceSpec : Spek({
    given("a value source") {
        on("get with non-empty path") {
            it("should throw NoSuchPathException") {
                assertThat({ 1.asSource()["a"] }, throws<NoSuchPathException>())
            }
        }
        on("invoke `asSource`") {
            val source = 1.asSource()
            it("should return itself") {
                assertThat(source.asSource(), sameInstance(source))
            }
        }
    }
})
