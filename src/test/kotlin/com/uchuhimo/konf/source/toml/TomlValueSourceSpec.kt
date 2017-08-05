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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.ParseException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object TomlValueSourceSpec : Spek({
    given("a TOML source") {
        on("get integer from long source") {
            it("should succeed") {
                assertThat(1L.asTomlSource().toInt(), equalTo(1))
            }
        }
        on("get integer from long source whose value is out of range of integer") {
            it("should throw ParseException") {
                assertThat({ Long.MAX_VALUE.asTomlSource().toInt() }, throws<ParseException>())
                assertThat({ Long.MIN_VALUE.asTomlSource().toInt() }, throws<ParseException>())
            }
        }
        on("invoke `asTomlSource`") {
            val source = 1.asTomlSource()
            it("should return itself") {
                assertThat(source.asTomlSource(), sameInstance(source))
            }
        }
    }
})
