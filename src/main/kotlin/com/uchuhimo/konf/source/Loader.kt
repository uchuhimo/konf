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

package com.uchuhimo.konf.source

import com.uchuhimo.konf.Config
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL

/**
 * Loader to load source from various input formats.
 *
 * @param config parent config
 */
class Loader(
        /**
         * Parent config for all child configs loading source in this loader.
         */
        val config: Config,
        /**
         * Source provider to provide source from various input format.
         */
        val provider: Provider
) {
    /**
     * Returns a child config containing values from specified reader.
     *
     * @param reader specified reader for reading character streams
     * @return a child config containing values from specified reader
     */
    fun reader(reader: Reader): Config =
            config.withSource(provider.fromReader(reader))

    /**
     * Returns a child config containing values from specified input stream.
     *
     * @param inputStream specified input stream of bytes
     * @return a child config containing values from specified input stream
     */
    fun inputStream(inputStream: InputStream): Config =
            config.withSource(provider.fromInputStream(inputStream))

    /**
     * Returns a child config containing values from specified file.
     *
     * @param file specified file
     * @return a child config containing values from specified file
     */
    fun file(file: File): Config =
            config.withSource(provider.fromFile(file))

    /**
     * Returns a child config containing values from specified file path.
     *
     * @param file specified file path
     * @return a child config containing values from specified file path
     */
    fun file(file: String): Config =
            config.withSource(provider.fromFile(file))

    /**
     * Returns a child config containing values from specified string.
     *
     * @param content specified string
     * @return a child config containing values from specified string
     */
    fun string(content: String): Config =
            config.withSource(provider.fromString(content))

    /**
     * Returns a child config containing values from specified byte array.
     *
     * @param content specified byte array
     * @return a child config containing values from specified byte array
     */
    fun bytes(content: ByteArray): Config =
            config.withSource(provider.fromBytes(content))

    /**
     * Returns a child config containing values from specified portion of byte array.
     *
     * @param content specified byte array
     * @param offset the start offset of the portion of the array to read
     * @param length the length of the portion of the array to read
     * @return a child config containing values from specified portion of byte array
     */
    fun bytes(content: ByteArray, offset: Int, length: Int): Config =
            config.withSource(provider.fromBytes(content, offset, length))

    /**
     * Returns a child config containing values from specified url.
     *
     * @param url specified url
     * @return a child config containing values from specified url
     */
    fun url(url: URL): Config =
            config.withSource(provider.fromUrl(url))

    /**
     * Returns a child config containing values from specified url string.
     *
     * @param url specified url string
     * @return a child config containing values from specified url string
     */
    fun url(url: String): Config =
            config.withSource(provider.fromUrl(url))

    /**
     * Returns a child config containing values from specified resource.
     *
     * @param resource path of specified resource
     * @return a child config containing values from specified resource
     */
    fun resource(resource: String): Config =
            config.withSource(provider.fromResource(resource))
}
