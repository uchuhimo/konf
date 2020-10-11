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

package com.uchuhimo.konf.snippet

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec

fun main(args: Array<String>) {
    val config = Config()
    config.addSpec(Server)
    run {
        val host = config[Server.host]
    }
    run {
        val host = config.get<String>("server.host")
    }
    run {
        val host = config<String>("server.host")
    }
    config.contains(Server.host)
    // or
    Server.host in config
    config.contains("server.host")
    // or
    "server.host" in config
    config[Server.tcpPort] = 80
    config["server.tcpPort"] = 80
    config.containsRequired()
    config.validateRequired()
    config.unset(Server.tcpPort)
    config.unset("server.tcpPort")
    val basePort by ConfigSpec("server").required<Int>()
    config.lazySet(Server.tcpPort) { it[basePort] + 1 }
    config.lazySet("server.tcpPort") { it[basePort] + 1 }
    run {
        var port by config.property(Server.tcpPort)
        port = 9090
        check(port == 9090)
    }
    run {
        val port by config.property(Server.tcpPort)
        check(port == 9090)
    }
}
