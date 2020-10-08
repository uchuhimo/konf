/*
 * Copyright 2017-2020 the original author or authors.
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
import com.natpryce.hamkrest.sameInstance
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object RelocatedConfigSpec : SubjectSpek<Config>({

    subject { Prefix("network.buffer") + Config { addSpec(NetworkBuffer) }.at("network.buffer") }

    configTestSpec()
})

object RollUpConfigSpec : SubjectSpek<Config>({

    subject { Prefix("prefix") + Config { addSpec(NetworkBuffer) } }

    configTestSpec("prefix.network.buffer")

    on("prefix is empty string") {
        it("should return itself") {
            assertThat(Prefix() + subject, sameInstance(subject))
        }
    }
})

object DrillDownConfigSpec : SubjectSpek<Config>({

    subject { Config { addSpec(NetworkBuffer) }.at("network") }

    configTestSpec("buffer")

    on("path is empty string") {
        it("should return itself") {
            assertThat(subject.at(""), sameInstance(subject))
        }
    }
})
