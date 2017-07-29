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

package com.uchuhimo.konf.source

import com.uchuhimo.konf.ConfigException
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.name
import java.io.File

open class SourceException : ConfigException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class WrongTypeException(val source: Source, actual: String, expected: String) :
        SourceException("source ${source.description} has type $actual rather than $expected")

class NoSuchPathException(val source: Source, val path: Path) :
        SourceException("cannot find path \"${path.name}\" in source ${source.description}")

class ParseException : SourceException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class UnsupportedTypeException : SourceException {
    constructor(source: Source, clazz: Class<*>) :
            super("value of type ${clazz.simpleName} is unsupported in source ${source.description}")

    constructor(source: Source, clazz: Class<*>, cause: Throwable) :
            super("value of type ${clazz.simpleName} is unsupported in source ${source.description}", cause)
}

class UnsupportedMapKeyException(val clazz: Class<*>) : SourceException(
        "cannot support map with ${clazz.simpleName} key, only support string key")

class LoadException(val path: Path, cause: Throwable) :
        SourceException("fail to load ${path.name}", cause)

class SourceNotFoundException(message: String) : SourceException(message)

class UnsupportedExtensionException(file: File) : SourceException(
        "cannot detect supported extension for \"${file.name}\"," +
                " supported extensions: conf, json, properties, toml, xml, yml, yaml")
