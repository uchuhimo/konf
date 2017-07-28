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

package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.parseDuration
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.concurrent.TimeUnit

object ParseDurationSpec : Spek({
    on("parse empty string") {
        it("throws ParseException") {
            assertThat({ parseDuration("") }, throws<ParseException>())
        }
    }
    on("parse string without unit") {
        it("parse as milliseconds") {
            assertThat(parseDuration("1"), equalTo(TimeUnit.MILLISECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'ms'") {
        it("parse as milliseconds") {
            assertThat(parseDuration("1ms"), equalTo(TimeUnit.MILLISECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'millis'") {
        it("parse as milliseconds") {
            assertThat(parseDuration("1 millis"), equalTo(TimeUnit.MILLISECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'milliseconds'") {
        it("parse as milliseconds") {
            assertThat(parseDuration("1 milliseconds"), equalTo(TimeUnit.MILLISECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'us'") {
        it("parse as microseconds") {
            assertThat(parseDuration("1us"), equalTo(TimeUnit.MICROSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'micros'") {
        it("parse as microseconds") {
            assertThat(parseDuration("1 micros"), equalTo(TimeUnit.MICROSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'microseconds'") {
        it("parse as microseconds") {
            assertThat(parseDuration("1 microseconds"), equalTo(TimeUnit.MICROSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'ns'") {
        it("parse as nanoseconds") {
            assertThat(parseDuration("1ns"), equalTo(TimeUnit.NANOSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'nanos'") {
        it("parse as nanoseconds") {
            assertThat(parseDuration("1 nanos"), equalTo(TimeUnit.NANOSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'nanoseconds'") {
        it("parse as nanoseconds") {
            assertThat(parseDuration("1 nanoseconds"), equalTo(TimeUnit.NANOSECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'd'") {
        it("parse as days") {
            assertThat(parseDuration("1d"), equalTo(TimeUnit.DAYS.toNanos(1)))
        }
    }
    on("parse string with unit 'days'") {
        it("parse as days") {
            assertThat(parseDuration("1 days"), equalTo(TimeUnit.DAYS.toNanos(1)))
        }
    }
    on("parse string with unit 'h'") {
        it("parse as hours") {
            assertThat(parseDuration("1h"), equalTo(TimeUnit.HOURS.toNanos(1)))
        }
    }
    on("parse string with unit 'hours'") {
        it("parse as hours") {
            assertThat(parseDuration("1 hours"), equalTo(TimeUnit.HOURS.toNanos(1)))
        }
    }
    on("parse string with unit 's'") {
        it("parse as seconds") {
            assertThat(parseDuration("1s"), equalTo(TimeUnit.SECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'seconds'") {
        it("parse as seconds") {
            assertThat(parseDuration("1 seconds"), equalTo(TimeUnit.SECONDS.toNanos(1)))
        }
    }
    on("parse string with unit 'm'") {
        it("parse as minutes") {
            assertThat(parseDuration("1m"), equalTo(TimeUnit.MINUTES.toNanos(1)))
        }
    }
    on("parse string with unit 'minutes'") {
        it("parse as minutes") {
            assertThat(parseDuration("1 minutes"), equalTo(TimeUnit.MINUTES.toNanos(1)))
        }
    }
    on("parse string with float number") {
        it("parse and convert from double to long") {
            assertThat(parseDuration("1.5ms"), equalTo(TimeUnit.MICROSECONDS.toNanos(1500)))
        }
    }
    on("parse string with invalid unit") {
        it("throws ParseException") {
            assertThat({ parseDuration("1x") }, throws<ParseException>())
        }
    }
    on("parse string with invalid number") {
        it("throws ParseException") {
            assertThat({ parseDuration("*1s") }, throws<ParseException>())
        }
    }
})
