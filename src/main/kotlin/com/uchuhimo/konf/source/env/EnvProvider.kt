/*
 * Copyright 2017 the original author or authors.
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

package com.uchuhimo.konf.source.env

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.FlatSource

/**
 * Provider for system environment source.
 */
object EnvProvider {
    /**
     * Returns a new source from system environment.
     *
     * @return a new source from system environment
     */
    fun fromEnv(): Source {
        return FlatSource(System.getenv(), type = "system-environment")
    }
}
