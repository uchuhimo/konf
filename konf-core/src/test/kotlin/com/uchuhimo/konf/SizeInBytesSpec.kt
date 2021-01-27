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

package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.ParseException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object SizeInBytesSpec : Spek({
    on("parse valid string") {
        it("parse as valid size in bytes") {
            assertThat(SizeInBytes.parse("1k").bytes, equalTo(1024L))
        }
    }
    on("init with negative number") {
        it("should throw IllegalArgumentException") {
            assertThat({ SizeInBytes(-1L) }, throws<IllegalArgumentException>())
        }
    }
    on("parse string without number part") {
        it("should throw ParseException") {
            assertThat({ SizeInBytes.parse("m") }, throws<ParseException>())
        }
    }
    on("parse string with float number") {
        it("parse and convert from double to long") {
            assertThat(SizeInBytes.parse("1.5kB").bytes, equalTo(1500L))
        }
    }
    on("parse string with invalid unit") {
        it("throws ParseException") {
            assertThat({ SizeInBytes.parse("1kb") }, throws<ParseException>())
        }
    }
    on("parse string with invalid number") {
        it("throws ParseException") {
            assertThat({ SizeInBytes.parse("*1k") }, throws<ParseException>())
        }
    }
    on("parse number out of range for a 64-bit long") {
        it("throws ParseException") {
            assertThat({ SizeInBytes.parse("1z") }, throws<ParseException>())
        }
    }
})
