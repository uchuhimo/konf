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

package com.uchuhimo.konf.example

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec

public object ServerSpec : ConfigSpec("server") {
    val host by optional("0.0.0.0")
    val port by required<Int>()
}

fun main(args: Array<String>) {
    val config = Config { addSpec(ServerSpec) }
        .from.yaml.file("/path/to/server.yml")
        .from.json.resource("server.json")
        .from.env()
        .from.systemProperties()
    run {
        val config = Config { addSpec(ServerSpec) }
            .from.yaml.watchFile("/path/to/server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    }
    val server = Server(config[ServerSpec.host], config[ServerSpec.port])
    server.start()
}
