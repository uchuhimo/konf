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

package com.uchuhimo.konf

/**
 * Enumeration that defines simple on/off features.
 */
enum class Feature(val enabledByDefault: Boolean) {
    /**
     * Feature that determines what happens when unknown paths appear in the source.
     * If enabled, an exception is thrown when loading from the source
     * to indicate it contains unknown paths.
     *
     * Feature is disabled by default.
     */
    FAIL_ON_UNKNOWN_PATH(false),
    /**
     * Feature that determines whether loading keys from sources case-insensitively.
     *
     * Feature is disabled by default.
     */
    LOAD_KEYS_CASE_INSENSITIVELY(false),

    /**
     * Feature that determines what happens when unfound sources are requested
     * while loading configuration values.
     * If enabled, exceptions will be thrown when trying to ouse unfound
     * resources or files to indicate sources cannot be found.
     * If disabled, unfound sources will silently be ignored.
     *
     * Feature is enabled by default.
     */
    FAIL_ON_UNFOUND_SOURCES(true)
}
