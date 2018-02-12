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
 * Exception for config.
 */
open class ConfigException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

/**
 * Exception indicates that there is existed item with same name in config.
 */
class RepeatedItemException(val name: String) : ConfigException("item $name has been added")

/**
 * Exception indicates that there is existed item with conflicted name in config.
 */
class NameConflictException(message: String) : ConfigException(message)

/**
 * Exception indicates that the evaluated result of lazy thunk is invalid.
 */
class InvalidLazySetException(message: String) : ConfigException(message)

/**
 * Exception indicates that the specified item is in unset state.
 */
class UnsetValueException(val name: String) : ConfigException("$name is unset")

/**
 * Exception indicates that the specified item is not in this config.
 */
class NoSuchItemException(val name: String) : ConfigException("cannot find $name in config")

/**
 * Exception indicates that config spec cannot be added to this config because it has child layer.
 */
class SpecFrozenException(val config: Config) :
        ConfigException("config ${config.name} has child layer, cannot add new spec")
