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

package com.uchuhimo.konf.snippet

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toValue
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import java.io.File

object ServerSpec : ConfigSpec() {
    val host by optional("0.0.0.0")
    val port by required<Int>()
}

fun main(args: Array<String>) {
    val file = File("server.yml")
    file.writeText("""
        server:
            host: 127.0.0.1
            port: 8080
    """.trimIndent())
    file.deleteOnExit()
    val config = Config { addSpec(ServerSpec) }
        .from.yaml.file("server.yml")
        .from.json.resource("server.json")
        .from.env()
        .from.systemProperties()
    run {
        val config = Config { addSpec(ServerSpec) }.withSource(
            Source.from.yaml.file("server.yml") +
                Source.from.json.resource("server.json") +
                Source.from.env() +
                Source.from.systemProperties()
        )
    }
    run {
        val config = Config { addSpec(ServerSpec) }
            .from.yaml.watchFile("server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    }
    val server = Server(config[ServerSpec.host], config[ServerSpec.port])
    server.start()
    run {
        val server = Config()
            .from.yaml.file("server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
            .at("server")
            .toValue<Server>()
        server.start()
    }
    run {
        val server = (
            Source.from.yaml.file("server.yml") +
                Source.from.json.resource("server.json") +
                Source.from.env() +
                Source.from.systemProperties()
            )["server"]
            .toValue<Server>()
        server.start()
    }
}
