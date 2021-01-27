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
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object FacadeConfigSpec : SubjectSpek<Config>({
    subject { Config() + Config { addSpec(NetworkBuffer) } }

    configTestSpec()
})

object FacadeConfigUsingWithFallbackSpec : SubjectSpek<Config>({
    subject { Config { addSpec(NetworkBuffer) }.withFallback(Config()) }

    configTestSpec()
})

object FallbackConfigSpec : SubjectSpek<Config>({
    subject { Config { addSpec(NetworkBuffer) } + Config() }

    configTestSpec()
})

object FallbackConfigUsingWithFallbackSpec : SubjectSpek<Config>({
    subject { Config().withFallback(Config { addSpec(NetworkBuffer) }) }

    configTestSpec()
})

object BothConfigSpec : SubjectSpek<Config>({
    subject { Config { addSpec(NetworkBuffer) } + Config { addSpec(NetworkBuffer) } }

    configTestSpec()

    given("a merged config") {
        on("set item in the fallback config") {
            (subject as MergedConfig).fallback[NetworkBuffer.type] = NetworkBuffer.Type.ON_HEAP
            it("should have higher priority than the default value") {
                assertThat((subject as MergedConfig)[NetworkBuffer.type], equalTo(NetworkBuffer.Type.ON_HEAP))
            }
        }
    }
})

class UpdateFallbackConfig(val config: MergedConfig) : MergedConfig(config.facade, config.fallback) {

    override fun rawSet(item: Item<*>, value: Any?) {
        if (item is LazyItem) {
            facade.rawSet(item, value)
        } else {
            fallback.rawSet(item, value)
        }
    }

    override fun unset(item: Item<*>) {
        fallback.unset(item)
    }

    override fun addItem(item: Item<*>, prefix: String) {
        fallback.addItem(item, prefix)
    }

    override fun addSpec(spec: Spec) {
        fallback.addSpec(spec)
    }
}

object UpdateFallbackConfigSpec : SubjectSpek<Config>({
    subject { UpdateFallbackConfig((Config { addSpec(NetworkBuffer) } + Config { addSpec(NetworkBuffer) }) as MergedConfig) }

    configTestSpec()
})
