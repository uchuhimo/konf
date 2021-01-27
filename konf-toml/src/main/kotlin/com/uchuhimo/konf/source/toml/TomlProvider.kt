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

package com.uchuhimo.konf.source.toml

import com.moandjiezana.toml.Toml
import com.uchuhimo.konf.annotation.JavaApi
import com.uchuhimo.konf.source.Provider
import com.uchuhimo.konf.source.RegisterExtension
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.asSource
import java.io.InputStream
import java.io.Reader

/**
 * Provider for TOML source.
 */
@RegisterExtension(["toml"])
object TomlProvider : Provider {
    override fun reader(reader: Reader): Source =
        Toml().read(reader).toMap().asSource(type = "TOML")

    override fun inputStream(inputStream: InputStream): Source =
        Toml().read(inputStream).toMap().asSource(type = "TOML")

    @JavaApi
    @JvmStatic
    fun get() = this
}
