# Konf

[![Java 8+](https://img.shields.io/badge/Java-8+-4c7e9f.svg)](http://java.oracle.com)
[![Bintray](https://api.bintray.com/packages/uchuhimo/maven/konf/images/download.svg)](https://bintray.com/uchuhimo/maven/konf/_latestVersion)
[![JitPack](https://jitpack.io/v/uchuhimo/konf.svg)](https://jitpack.io/#uchuhimo/konf)
[![Build Status](https://travis-ci.org/uchuhimo/konf.svg?branch=master)](https://travis-ci.org/uchuhimo/konf)
[![codecov](https://codecov.io/gh/uchuhimo/konf/branch/master/graph/badge.svg)](https://codecov.io/gh/uchuhimo/konf)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)

A type-safe cascading configuration library for Kotlin/Java, supporting most configuration formats.

## Features

- **Type-safe**. Get/set value in config with type-safe APIs.
- **Thread-safe**. All APIs for config is thread-safe.
- **Batteries included**. Support sources from JSON, XML, YAML, [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md), [TOML](https://github.com/toml-lang/toml), properties, map, command line and system environment out of box.
- **Cascading**. Config can fork from another config by adding a new layer on it. Each layer of config can be updated independently. This feature is powerful enough to support complicated situation such as configs with different values share common fallback config, which is automatically updated when configuration file changes.
- **Self-documenting**. Document config item with type, default value and description when declaring.
- **Extensible**. Easy to customize new sources for config or expose items in config.

## Contents

- [Prerequisites](#prerequisites)
- [Use in your projects](#use-in-your-projects)
- [Quick start](#quick-start)
- [Define items](#define-items)
- [Use config](#use-config)
- [Load values from source](#load-values-from-source)
- [Export/Reload values in config](#exportreload-values-in-config)
- [Supported item types](#supported-item-types)
- [Build from source](#build-from-source)

## Prerequisites

- JDK 1.8 or higher

## Use in your projects

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
  <version>0.12</version>
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
compile 'com.uchuhimo:konf:0.12'
```

### Maven (master snapshot)

Add JitPack repository to `<repositories>` section:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

Add dependencies:

```xml
<dependency>
    <groupId>com.github.uchuhimo</groupId>
    <artifactId>konf</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

### Gradle (master snapshot)

Add JitPack repository:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add dependencies:

```groovy
compile 'com.github.uchuhimo:konf:master-SNAPSHOT'
```

## Quick start

1. Define items in config spec:

    ```kotlin
    object ServerSpec : ConfigSpec("server") {
        val host by optional("0.0.0.0")
        val port by required<Int>()
    }
    ```

2. Construct config with items in config spec and values from multiple sources:

    ```kotlin
    val config = Config { addSpec(ServerSpec) }
            .from.yaml.file("/path/to/server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    ```

    This config contains all items defined in `ServerSpec`, and load values from 4 different sources. Values in resource file `server.json` will override those in file `/path/to/server.yml`, values from system environment will override those in `server.json`, and values from system properties will override those from system environment.

    If you want to watch file `/path/to/server.yml` and reload values when file content is changed, you can use `watchFile` instead of `file`:

    ```kotlin
    val config = Config { addSpec(ServerSpec) }
            .from.yaml.watchFile("/path/to/server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    ```

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

## Define items

Config items is declared in config spec, added to config by `Config#addSpec`. All items in same config spec have same prefix. Define a config spec with prefix `local.server`:

```kotlin
object ServerSpec : ConfigSpec("local.server") {
}
```

If the config spec is binding with single class, you can declare config spec as companion object of the class:

```kotlin
class Server {
    companion object : ConfigSpec("server") {
        val host by optional("0.0.0.0")
        val port by required<Int>()
    }
}
```

There are three kinds of item:

- Required item. Required item doesn't have default value, thus must be set with value before retrieved in config. Define a required item with description:
    ```kotlin
    val port by required<Int>(description = "port of server")
    ```
    Or omit the description:
    ```kotlin
    val port by required<Int>()
    ```
- Optional item. Optional item has default value, thus can be safely retrieved before setting. Define an optional item:
    ```kotlin
    val host by optional("0.0.0.0", description = "host IP of server")
    ```
    Description can be omitted.
- Lazy item. Lazy item also has default value, however, the default value is not a constant, it is evaluated from thunk every time when retrieved. Define a lazy item:
    ```kotlin
    val nextPort by lazy { config -> config[port] + 1 }
    ```

You can also define config spec in Java, with a more vorbose API (compared to Kotlin version in "quick start"):

```java
public class ServerSpec {
  public static final ConfigSpec spec = new ConfigSpec("server");

  public static final OptionalItem<String> host =
      new OptionalItem<String>(spec, "host", "0.0.0.0") {};

  public static final RequiredItem<Integer> port = new RequiredItem<Integer>(spec, "port") {};
}
```

Notice that the `{}` part in item declaration is necessary to avoid type erasure of item's type information.

## Use config

### Create config

Create an empty new config:

```kotlin
val config = Config()
```

Or an new config with some initial actions:

```kotlin
val config = Config { addSpec(Server) }
```

### Add config spec

Add multiple config specs into config:

```kotlin
config.addSpec(Server)
config.addSpec(Client)
```

### Retrieve value from config

Retrieve associated value with item (type-safe API):

```kotlin
val host = config[Server.host]
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
config.contains(Server.host)
```

Check whether value exists in config or not with item name:

```kotlin
config.contains("server.host")
```

### Modify value in config

Associate item with value (type-safe API):

```kotlin
config[Server.port] = 80
```

Find item with specified name, and associate it with value (unsafe API):

```kotlin
config["server.port"] = 80
```

Discard associated value of item:

```kotlin
config.unset(Server.port)
```

Discard associated value of item with specified name:

```kotlin
config.unset("server.port")
```

Associate item with lazy thunk (type-safe API):

```kotlin
config.lazySet(Server.port) { it[basePort] + 1 }
```

Find item with specified name, and associate it with lazy thunk (unsafe API):

```kotlin
config.lazySet("server.port") { it[basePort] + 1 }
```

### Export value in config as property

Export a read-write property from value in config:

```kotlin
var port by config.property(Server.port)
port = 9090
check(port == 9090)
```

Export a read-only property from value in config:

```kotlin
val port by config.property(Server.port)
check(port == 9090)
```

### Fork from another config

```kotlin
val config = Config { addSpec(Server) }
config[Server.port] = 1000
// fork from parent config
val childConfig = config.withLayer("child")
// child config inherit values from parent config
check(childConfig[Server.port] == 1000)
// modifications in parent config affect values in child config
config[Server.port] = 2000
check(config[Server.port] == 2000)
check(childConfig[Server.port] == 2000)
// modifications in child config don't affect values in parent config
childConfig[Server.port] = 3000
check(config[Server.port] == 2000)
check(childConfig[Server.port] == 3000)
```

## Load values from source

Use `from` to load values from source doesn't affect values in config, it will return a new child config by loading all values into new layer in child config:

```kotlin
val config = Config { addSpec(Server) }
// values in source is loaded into new layer in child config
val childConfig = config.from.env()
check(childConfig.parent === config)
```

All out-of-box supported sources are declared in [`DefaultLoaders`](https://github.com/uchuhimo/konf/blob/master/src/main/kotlin/com/uchuhimo/konf/source/DefaultLoaders.kt), shown below (the corresponding config spec for these samples is [`ConfigForLoad`](https://github.com/uchuhimo/konf/blob/master/src/test/kotlin/com/uchuhimo/konf/source/ConfigForLoad.kt)):

| Type | Sample |
| - | - |
| [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) | [`source.conf`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.conf) |
| JSON | [`source.json`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.json) |
| properties | [`source.properties`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.properties) |
| [TOML](https://github.com/toml-lang/toml) | [`source.toml`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.toml) |
| XML | [`source.xml`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.xml) |
| YAML | [`source.yaml`](https://github.com/uchuhimo/konf/blob/master/src/test/resources/source/source.yaml) |
| hierarchical map | [`MapSourceLoadSpec`](https://github.com/uchuhimo/konf/blob/master/src/test/kotlin/com/uchuhimo/konf/source/base/MapSourceLoadSpec.kt) |
| map in key-value format | [`KVSourceSpec`](https://github.com/uchuhimo/konf/blob/master/src/test/kotlin/com/uchuhimo/konf/source/base/KVSourceSpec.kt) |
| map in flat format | [`FlatSourceLoadSpec`](https://github.com/uchuhimo/konf/blob/master/src/test/kotlin/com/uchuhimo/konf/source/base/FlatSourceLoadSpec.kt) |
| system properties | - |
| system environment | - |

Format of system properties source is same with that of properties source. System environment source follows the same mapping convention with properties source, but with the following name convention:

- All letters in name are in uppercase
- `.` in name is replaced with `_`

HOCON/JSON/properties/TOML/XML/YAML source can be loaded from a variety of input format. Use properties source as example:

- From file: `config.from.properties.file("/path/to/file")`
- From watched file: `config.from.properties.watchFile("/path/to/file", 100, TimeUnit.MILLISECONDS)`
- From string: `config.from.properties.string("server.port = 8080")`
- From URL: `config.from.properties.url("http://localhost:8080/source.properties")`
- From watched URL: `config.from.properties.watchUrl("http://localhost:8080/source.properties", 1, TimeUnit.MINUTES)`
- From Git repository: `config.from.properties.git("https://github.com/uchuhimo/konf.git", "/path/to/source.properties", branch = "dev")`
- From watched Git repository: `config.from.properties.watchGit("https://github.com/uchuhimo/konf.git", "/path/to/source.properties", period = 1, unit = TimeUnit.MINUTES)`
- From resource: `config.from.properties.resource("source.properties")`
- From reader: `config.from.properties.reader(reader)`
- From input stream: `config.from.properties.inputStream(inputStream)`
- From byte array: `config.from.properties.bytes(bytes)`
- From portion of byte array: `config.from.properties.bytes(bytes, 1, 12)`

If source is from file, file extension can be auto detected. Thus, you can use `config.from.file("/path/to/source.json")` instead of `config.from.json.file("/path/to/source.json")`, or use `config.from.watchFile("/path/to/source.json")` instead of `config.from.json.watchFile("/path/to/source.json")`. Source from URL also support auto-detected extension (use `config.from.url` or `config.from.watchUrl`). The following file extensions can be supported:

| Type | Extension |
| - | - |
| HOCON | conf |
| JSON | json |
| Properties | properties |
| TOML | toml |
| XML | xml |
| YAML | yml, yaml |

You can also implement [`Source`](https://github.com/uchuhimo/konf/blob/master/src/main/kotlin/com/uchuhimo/konf/source/Source.kt) to customize your new source, which can be loaded into config by `config.withSource(source)`.

## Export/Reload values in config

Export all values in config to map in key-value format:

```kotlin
val map = config.toMap()
```

Export all values in facade layer of config to map:

```kotlin
val map = config.layer.toMap()
```

Export all values in config to hierarchical map:

```kotlin
val map = config.toHierarchicalMap()
```

Export all values in config to map in flat format:

```kotlin
val map = config.toFlatMap()
```

Export all values in config to JSON:

```kotlin
val file = createTempFile(suffix = ".json")
config.toJson.toFile(file)
```

Reload values from JSON:

```kotlin
val newConfig = Config {
    addSpec(Server)
}.from.json.file(file)
check(config == newConfig)
```

Config can be saved to a variety of output format in HOCON/JSON/properties/TOML/XML/YAML. Use JSON as example:

- To file: `config.toJson.toFile("/path/to/file")`
- To string: `config.toJson.toText()`
- To writer: `config.toJson.toWriter(writer)`
- To output stream: `config.toJson.toOutputStream(outputStream)`
- To byte array: `config.toJson.toBytes()`

You can also implement [`Writer`](https://github.com/uchuhimo/konf/blob/master/src/main/kotlin/com/uchuhimo/konf/source/Writer.kt) to customize your new writer (see [`JsonWriter`](https://github.com/uchuhimo/konf/blob/master/src/main/kotlin/com/uchuhimo/konf/source/json/JsonWriter.kt) for how to integrate your writer with config).

## Supported item types

Supported item types include:

- All primitive types
- All primitive array types
- `BigInteger`
- `BigDecimal`
- `String`
- Date and Time
    - `java.util.Date`
    - `OffsetTime`
    - `OffsetDateTime`
    - `ZonedDateTime`
    - `LocalDate`
    - `LocalTime`
    - `LocalDateTime`
    - `Year`
    - `YearMonth`
    - `Instant`
    - `Duration`
- `SizeInBytes`
- Enum
- Array
- Collection
    - `List`
    - `Set`
    - `SortedSet`
    - `Map`
    - `SortedMap`
- Kotlin Built-in classes
    - `Pair`
    - `Triple`
    - `IntRange`
    - `CharRange`
    - `LongRange`
- Data classes
- POJOs supported by Jackson core modules

Konf supports size in bytes format described in [HOCON document](https://github.com/typesafehub/config/blob/master/HOCON.md#size-in-bytes-format) with class `SizeInBytes`.

Konf supports both [ISO-8601 duration format](https://en.wikipedia.org/wiki/ISO_8601#Durations) and [HOCON duration format](https://github.com/typesafehub/config/blob/master/HOCON.md#duration-format) for `Duration`.

Konf uses [Jackson](https://github.com/FasterXML/jackson) to support Kotlin Built-in classes, Data classes and POJOs. You can use `config.mapper` to access `ObjectMapper` instance used by config, and configure it to support more types from third-party Jackson modules. Default modules registered by Konf include:

- Jackson core modules
- `JavaTimeModule` in [jackson-modules-java8](https://github.com/FasterXML/jackson-modules-java8)
- [jackson-module-kotlin](https://github.com/FasterXML/jackson-module-kotlin)

## Build from source

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
