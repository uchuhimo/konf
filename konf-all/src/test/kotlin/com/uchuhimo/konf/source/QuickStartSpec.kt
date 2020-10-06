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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.snippet.Server
import com.uchuhimo.konf.snippet.ServerSpec
import com.uchuhimo.konf.toValue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File

object QuickStartSpec : Spek({
    on("use default loaders") {
        val config = useFile {
            Config { addSpec(ServerSpec) }
                .from.yaml.file("server.yml")
                .from.json.resource("server.json")
                .from.env()
                .from.systemProperties()
        }
        it("should load all values") {
            assertThat(
                config.toMap(),
                equalTo(mapOf("server.host" to "127.0.0.1", "server.port" to 8080))
            )
        }
    }
    on("use default providers") {
        val config = useFile {
            Config { addSpec(ServerSpec) }.withSource(
                Source.from.yaml.file("server.yml") +
                    Source.from.json.resource("server.json") +
                    Source.from.env() +
                    Source.from.systemProperties()
            )
        }
        it("should load all values") {
            assertThat(
                config.toMap(),
                equalTo(mapOf("server.host" to "127.0.0.1", "server.port" to 8080))
            )
        }
    }
    on("watch file") {
        val config = useFile {
            Config { addSpec(ServerSpec) }
                .from.yaml.watchFile("server.yml")
                .from.json.resource("server.json")
                .from.env()
                .from.systemProperties()
        }
        it("should load all values") {
            assertThat(
                config.toMap(),
                equalTo(mapOf("server.host" to "127.0.0.1", "server.port" to 8080))
            )
        }
    }
    on("cast config to value") {
        val config = useFile {
            Config()
                .from.yaml.file("server.yml")
                .from.json.resource("server.json")
                .from.env()
                .from.systemProperties()
                .at("server")
        }
        val server = config.toValue<Server>()
        it("should load all values") {
            assertThat(server, equalTo(Server(host = "127.0.0.1", port = 8080)))
        }
    }
    on("cast source to value") {
        val source = useFile {
            (
                Source.from.yaml.file("server.yml") +
                    Source.from.json.resource("server.json") +
                    Source.from.env() +
                    Source.from.systemProperties()
                )["server"]
        }
        val server = source.toValue<Server>()
        it("should load all values") {
            assertThat(server, equalTo(Server(host = "127.0.0.1", port = 8080)))
        }
    }
})

private fun <T> useFile(block: () -> T): T {
    val file = File("server.yml")
    //language=YAML
    file.writeText(
        """
        server:
            host: 127.0.0.1
            port: 8080
        """.trimIndent()
    )
    try {
        return block()
    } finally {
        file.delete()
    }
}
