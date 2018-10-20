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

package com.uchuhimo.konf.snippet

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec

object Outer : ConfigSpec() {
    object Inner : ConfigSpec() {
        val host by optional("0.0.0.0")
        val port by required<Int>()

        object Login : ConfigSpec() {
            val user by optional("admin")
            val password by optional("123456")
        }
    }

    object Inner2 : ConfigSpec() {
        val port by optional(8000)
    }
}

fun main(args: Array<String>) {
    val config = Config {
        addSpec(Outer)
    }.from.map.kv(mapOf(
        "outer.inner.host" to "127.0.0.1",
        "outer.inner.port" to 8080))
    check(config[Outer.Inner.host] == "127.0.0.1")
    check(config[Outer.Inner.port] == 8080)
    check(config[Outer.Inner.Login.user] == "admin")
    check(config[Outer.Inner.Login.password] == "123456")
    check(config[Outer.Inner2.port] == 8000)
}
