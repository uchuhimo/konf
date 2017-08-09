# Konf

[![Java 8+](https://img.shields.io/badge/Java-8+-4c7e9f.svg)](http://java.oracle.com)
[![Bintray](https://api.bintray.com/packages/uchuhimo/maven/konf/images/download.svg)](https://bintray.com/uchuhimo/maven/konf/_latestVersion)
[![JitPack](https://jitpack.io/v/uchuhimo/konf.svg)](https://jitpack.io/#uchuhimo/konf)
[![Build Status](https://travis-ci.org/uchuhimo/konf.svg?branch=master)](https://travis-ci.org/uchuhimo/konf)
[![codecov](https://codecov.io/gh/uchuhimo/konf/branch/master/graph/badge.svg)](https://codecov.io/gh/uchuhimo/konf)

A type-safe cascading configuration library for Kotlin/Java, supporting most mainstream source types (JSON/XML/YAML/HOCON/TOML/properties/environment/...).

## Features

- Type-safe. Get/set value in config with type-safe APIs.
- Thread-safe. All APIs for config is thread-safe.
- Batteries included. Support sources from JSON, XML, YAML, [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md), [TOML](https://github.com/toml-lang/toml), properties, map, command line and system environment out of box.
- Cascading. Config can fork from another config by adding a new layer on it. Each layer of config can be updated independently. This feature is powerful enough to support complicated situation such as configs with different values share common fallback config, which is automatically updated when configuration file changes.
- Self-documenting. Document config item with type, default value and description when declaring.
- Extensible. Easy to customize new sources for config or expose items in config.

## Prerequisites

- JDK 1.8 or higher

## Using in your projects

This library are published to [JCenter](https://bintray.com/uchuhimo/maven/konf) and [JitPack](https://jitpack.io/#uchuhimo/konf).

### Maven

Add Bintray JCenter repository to `<repositories>` section:

```xml
<repository>
    <id>central</id>
    <url>http://jcenter.bintray.com</url>
</repository>
```

Add dependencies:

```xml
<dependency>
  <groupId>com.uchuhimo</groupId>
  <artifactId>konf</artifactId>
  <version>0.4</version>
  <type>pom</type>
</dependency>
```

### Gradle

Add Bintray JCenter repository:

```groovy
repositories {
    jcenter()
}
```

Add dependencies:

```groovy
compile 'com.uchuhimo:konf:0.4'
```

## Quick start

1. Define items in config spec:

    ```kotlin
    object server : ConfigSpec("server") {
        val host = optional("host", "0.0.0.0")
        val port = required<Int>("port")
    }
    ```

2. Construct config with items in config spec and values from multiple sources:

    ```kotlin
    val config = Config { addSpec(server) }
            .withSourceFrom.yaml.file(File("/path/to/server.yml"))
            .withSourceFrom.json.resource("server.json")
            .withSourceFrom.env()
            .withSourceFrom.systemProperties()
    ```

    This config contains all items defined in `server`, and load values from 4 different sources. Values in resource file `server.json` will override those in file `/path/to/server.yml`, values from system environment will override those in `server.json`, and values from system properties will override those from system environment.

3. Define values in source. You can define in any of these sources:
    - in `/path/to/server.yml`:
        ```yaml
        server:
            host: 0.0.0.0
            port: 8080
        ```
    - in `server.json`:
        ```json
        {
            "server": {
                "host": "0.0.0.0",
                "port": 8080
            }
        }
        ```
    - in system environment:
        ```bash
        SERVER_HOST=0.0.0.0
        SERVER_PORT=8080
        ```
    - in command line for system properties:
        ```bash
        -Dserver.host=0.0.0.0 -Dserver.port=8080
        ```

4. Retrieve values from config with type-safe APIs:
    ```kotlin
    val server = Server(config[server.host], config[server.port])
    server.start()
    ```

## Config spec

Config items is declared in config spec, added to config by `Config#addSpec`. All items in same config spec have same prefix. Define a config spec with prefix `local.server`:

```kotlin
object server : ConfigSpec("local.server") {
}
```

There are three kinds of item:

- Required item. Required item doesn't have default value, thus must be set with value before retrieved in config. Define a required item with description:
    ```kotlin
    val port = required<Int>("port", description = "port of server")
    ```
    Or omit the description:
    ```kotlin
    val port = required<Int>("port")
    ```
- Optional item. Optional item has default value, thus can be safely retrieved before setting. Define an optional item:
    ```kotlin
    val host = optional("host", "0.0.0.0", description = "host IP of server")
    ```
    Description can be omitted.
- Lazy item. Lazy item also has default value, however, the default value is not a constant, it is evaluated from thunk every time when retrieved. Define a lazy item:
    ```kotlin
    val nextPort = lazy("nextPort") { config -> config[port] + 1 }
    ```

You can also define config spec in Java, with a more vorbose API (compared to Kotlin version in "quick start"):

```java
public class ServerConfig {
  public static final ConfigSpec spec = new ConfigSpec("server");

  public static final OptionalItem<String> host =
      new OptionalItem<String>(spec, "host", "0.0.0.0") {};

  public static final RequiredItem<Integer> port = new RequiredItem<Integer>(spec, "port") {};
}
```

Notice that the `{}` part in item declaration is necessary to avoid type erasure of item's type information.

## Config

### Create config

Create an empty new config:

```kotlin
val config = Config()
```

Or an new config with some initial actions:

```kotlin
val config = Config { addSpec(server) }
```

### Add config spec

Add multiple config specs into config:

```kotlin
config.addSpec(server)
config.addSpec(client)
```

### Retrieve value from config

Retrieve associated value with item (type-safe API):

```kotlin
val host = config[server.host]
```

Retrieve associated value with item name (unsafe API):

```kotlin
val host = config.get<String>("server.host")
```

or:

```kotlin
val host = config<String>("server.host")
```

### Check whether value exists in config or not

Check whether value exists in config or not with item:

```kotlin
config.contains(server.host)
```

Check whether value exists in config or not with item name:

```kotlin
config.contains("server.host")
```

### Modify value in config

Associate item with value (type-safe API):

```kotlin
config[server.port] = 80
```

Find item with specified name, and associate it with value (unsafe API):

```kotlin
config["server.port"] = 80
```

Discard associated value of item:

```kotlin
config.unset(server.port)
```

Discard associated value of item with specified name:

```kotlin
config.unset("server.port")
```

Associate item with lazy thunk (type-safe API):

```kotlin
config.lazySet(server.port) { it[basePort] + 1 }
```

Find item with specified name, and associate it with lazy thunk (unsafe API):

```kotlin
config.lazySet("server.port") { it[basePort] + 1 }
```

### Export value in config as property

Export a read-write property from value in config:

```kotlin
var port by config.property(server.port)
port = 9090
check(port == 9090)
```

Export a read-only property from value in config:

```kotlin
val port by config.property(server.port)
check(port == 80)
```

## Source

## Supported item types

## Generate document from config

## Building from source

Build library with Gradle using the following command:

```
gradlew clean assemble
```

Test library with Gradle using the following command:

```
gradlew clean test
```

Since Gradle has excellent incremental build support, you can usually omit executing the `clean` task.

Install library in a local Maven repository for consumption in other projects via the following command:

```
gradlew clean install
```

# License

© uchuhimo, 2017. Licensed under an [Apache 2.0](./LICENSE) license.
