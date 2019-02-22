/*
 * Copyright 2017-2018 the original author or authors.
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

package com.uchuhimo.konf.source.hocon

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.WrongTypeException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object HoconValueSourceSpec : Spek({
    given("a HOCON value source") {
        on("access the underlying config value") {
            it("should return the corresponding config value") {
                assertThat("1".toHoconValueSource().value.unwrapped(), equalTo(1 as Any))
            }
        }
        on("treat object value source as HOCON source") {
            val source = "{key = 1}".toHoconValueSource()
            it("should contain specified value") {
                assertTrue("key" in source)
                assertThat(source["key"].toInt(), equalTo(1))
            }
        }
        on("treat number value source as HOCON source") {
            val source = "1".toHoconValueSource()
            it("should throw WrongTypeException") {
                assertThat({ source["key"] }, throws<WrongTypeException>())
            }
        }
        on("get integer from integer value source") {
            it("should succeed") {
                assertThat("1".toHoconValueSource().toInt(), equalTo(1))
            }
        }
        on("get long from long value source") {
            val source = "123456789000".toHoconValueSource()
            it("should succeed") {
                assertTrue(source.isLong())
                assertThat(source.toLong(), equalTo(123_456_789_000L))
            }
        }
        on("get long from integer value source") {
            val source = "1".toHoconValueSource()
            it("should succeed") {
                assertFalse(source.isLong())
                assertThat(source.toLong(), equalTo(1L))
            }
        }
        on("get double from double value source") {
            val source = "1.5".toHoconValueSource()
            it("should succeed") {
                assertTrue(source.isDouble())
                assertThat(source.toDouble(), equalTo(1.5))
            }
        }
        on("get double from int value source") {
            val source = "1".toHoconValueSource()
            it("should succeed") {
                assertFalse(source.isDouble())
                assertThat(source.toDouble(), equalTo(1.0))
            }
        }
        on("get double from long value source") {
            val source = "123456789000".toHoconValueSource()
            it("should succeed") {
                assertFalse(source.isDouble())
                assertThat(source.toDouble(), equalTo(123456789000.0))
            }
        }
    }
})

private fun String.toHoconValueSource(): HoconSource {
    return HoconProvider.fromString("key = $this")["key"] as HoconSource
}
