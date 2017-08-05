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

package com.uchuhimo.konf

open class ConfigException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class RepeatedItemException(val name: String) : ConfigException("item $name has been added")

class NameConflictException(message: String) : ConfigException(message)

class InvalidLazySetException(message: String) : ConfigException(message)

class UnsetValueException(val name: String) : ConfigException("$name is unset")

class NoSuchItemException(val name: String) : ConfigException("cannot find $name in config")

class SpecFrozenException(val config: Config) :
        ConfigException("config ${config.name} has children layer, cannot add new spec")
