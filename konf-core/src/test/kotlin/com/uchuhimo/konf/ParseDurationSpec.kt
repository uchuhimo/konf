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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.toDuration
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Duration

object ParseDurationSpec : Spek({
    on("parse empty string") {
        it("throws ParseException") {
            assertThat({ "".toDuration() }, throws<ParseException>())
        }
    }
    on("parse string without unit") {
        it("parse as milliseconds") {
            assertThat("1".toDuration(), equalTo(Duration.ofMillis(1)))
        }
    }
    on("parse string with unit 'ms'") {
        it("parse as milliseconds") {
            assertThat("1ms".toDuration(), equalTo(Duration.ofMillis(1)))
        }
    }
    on("parse string with unit 'millis'") {
        it("parse as milliseconds") {
            assertThat("1 millis".toDuration(), equalTo(Duration.ofMillis(1)))
        }
    }
    on("parse string with unit 'milliseconds'") {
        it("parse as milliseconds") {
            assertThat("1 milliseconds".toDuration(), equalTo(Duration.ofMillis(1)))
        }
    }
    on("parse string with unit 'us'") {
        it("parse as microseconds") {
            assertThat("1us".toDuration(), equalTo(Duration.ofNanos(1000)))
        }
    }
    on("parse string with unit 'micros'") {
        it("parse as microseconds") {
            assertThat("1 micros".toDuration(), equalTo(Duration.ofNanos(1000)))
        }
    }
    on("parse string with unit 'microseconds'") {
        it("parse as microseconds") {
            assertThat("1 microseconds".toDuration(), equalTo(Duration.ofNanos(1000)))
        }
    }
    on("parse string with unit 'ns'") {
        it("parse as nanoseconds") {
            assertThat("1ns".toDuration(), equalTo(Duration.ofNanos(1)))
        }
    }
    on("parse string with unit 'nanos'") {
        it("parse as nanoseconds") {
            assertThat("1 nanos".toDuration(), equalTo(Duration.ofNanos(1)))
        }
    }
    on("parse string with unit 'nanoseconds'") {
        it("parse as nanoseconds") {
            assertThat("1 nanoseconds".toDuration(), equalTo(Duration.ofNanos(1)))
        }
    }
    on("parse string with unit 'd'") {
        it("parse as days") {
            assertThat("1d".toDuration(), equalTo(Duration.ofDays(1)))
        }
    }
    on("parse string with unit 'days'") {
        it("parse as days") {
            assertThat("1 days".toDuration(), equalTo(Duration.ofDays(1)))
        }
    }
    on("parse string with unit 'h'") {
        it("parse as hours") {
            assertThat("1h".toDuration(), equalTo(Duration.ofHours(1)))
        }
    }
    on("parse string with unit 'hours'") {
        it("parse as hours") {
            assertThat("1 hours".toDuration(), equalTo(Duration.ofHours(1)))
        }
    }
    on("parse string with unit 's'") {
        it("parse as seconds") {
            assertThat("1s".toDuration(), equalTo(Duration.ofSeconds(1)))
        }
    }
    on("parse string with unit 'seconds'") {
        it("parse as seconds") {
            assertThat("1 seconds".toDuration(), equalTo(Duration.ofSeconds(1)))
        }
    }
    on("parse string with unit 'm'") {
        it("parse as minutes") {
            assertThat("1m".toDuration(), equalTo(Duration.ofMinutes(1)))
        }
    }
    on("parse string with unit 'minutes'") {
        it("parse as minutes") {
            assertThat("1 minutes".toDuration(), equalTo(Duration.ofMinutes(1)))
        }
    }
    on("parse string with float number") {
        it("parse and convert from double to long") {
            assertThat("1.5ms".toDuration(), equalTo(Duration.ofNanos(1_500_000)))
        }
    }
    on("parse string with invalid unit") {
        it("throws ParseException") {
            assertThat({ "1x".toDuration() }, throws<ParseException>())
        }
    }
    on("parse string with invalid number") {
        it("throws ParseException") {
            assertThat({ "*1s".toDuration() }, throws<ParseException>())
        }
    }
})
