package com.uchuhimo.konf

open class ConfigException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class RepeatedItemException(val name: String) : ConfigException("item $name has been added")

class NameConflictException(message: String) : ConfigException(message)

class InvalidLazySetException(message: String) : ConfigException(message)

class UnsetValueException(val name: String) : ConfigException("${name} is unset")

class NoSuchItemException(val name: String) : ConfigException("cannot find $name in config")

class SpecFrozenException(val config: Config) :
        ConfigException("config ${config.name} has children layer, cannot add new spec")
