# Konf

[![Java 8+](https://img.shields.io/badge/Java-8+-4c7e9f.svg)](http://java.oracle.com)
[![Maven metadata URL](https://img.shields.io/maven-central/v/com.uchuhimo/konf)](https://search.maven.org/artifact/com.uchuhimo/konf)
[![JitPack](https://img.shields.io/jitpack/v/github/uchuhimo/konf)](https://jitpack.io/#uchuhimo/konf)
[![Build Status](https://travis-ci.org/uchuhimo/konf.svg?branch=master)](https://travis-ci.org/uchuhimo/konf)
[![codecov](https://codecov.io/gh/uchuhimo/konf/branch/master/graph/badge.svg)](https://codecov.io/gh/uchuhimo/konf)
[![codebeat badge](https://codebeat.co/badges/f69a1574-9d4c-4da5-be73-56fa7b180d2d)](https://codebeat.co/projects/github-com-uchuhimo-konf-master)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)

A type-safe cascading configuration library for Kotlin/Java/Android, supporting most configuration formats.

## Features

- **Type-safe**. Get/set value in config with type-safe APIs.
- **Thread-safe**. All APIs for config is thread-safe.
- **Batteries included**. Support sources from JSON, XML, YAML, [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md), [TOML](https://github.com/toml-lang/toml), properties, map, command line and system environment out of box.
- **Cascading**. Config can fork from another config by adding a new layer on it. Each layer of config can be updated independently. This feature is powerful enough to support complicated situation such as configs with different values share common fallback config, which is automatically updated when configuration file changes.
- **Self-documenting**. Document config item with type, default value and description when declaring.
- **Extensible**. Easy to customize new sources for config or expose items in config.

## Contents

- [Konf](#konf)
  - [Features](#features)
  - [Contents](#contents)
  - [Prerequisites](#prerequisites)
  - [Use in your projects](#use-in-your-projects)
    - [Maven](#maven)
    - [Gradle](#gradle)
    - [Gradle Kotlin DSL](#gradle-kotlin-dsl)
    - [Maven (master snapshot)](#maven-master-snapshot)
    - [Gradle (master snapshot)](#gradle-master-snapshot)
    - [Gradle Kotlin DSL (master snapshot)](#gradle-kotlin-dsl-master-snapshot)
  - [Quick start](#quick-start)
  - [Define items](#define-items)
  - [Use config](#use-config)
    - [Create config](#create-config)
    - [Add config spec](#add-config-spec)
    - [Retrieve value from config](#retrieve-value-from-config)
    - [Cast config to value](#cast-config-to-value)
    - [Check whether an item exists in config or not](#check-whether-an-item-exists-in-config-or-not)
    - [Modify value in config](#modify-value-in-config)
    - [Subscribe the update event](#subscribe-the-update-event)
    - [Export value in config as property](#export-value-in-config-as-property)
    - [Fork from another config](#fork-from-another-config)
  - [Load values from source](#load-values-from-source)
    - [Subscribe the update event for load operation](#subscribe-the-update-event-for-load-operation)
    - [Strict parsing when loading](#strict-parsing-when-loading)
    - [Path substitution](#path-substitution)
  - [Prefix/Merge operations for source/config/config spec](#prefixmerge-operations-for-sourceconfigconfig-spec)
  - [Export/Reload values in config](#exportreload-values-in-config)
  - [Supported item types](#supported-item-types)
  - [Optional features](#optional-features)
  - [Build from source](#build-from-source)
  - [Breaking Changes](#breaking-changes)
    - [v0.19.0](#v0190)
    - [v0.17.0](#v0170)
    - [v0.15](#v015)
    - [v0.10](#v010)
- [License](#license)

## Prerequisites

- JDK 8 or higher
- tested on Android SDK 23 or higher

## Use in your projects

This library has been published to [Maven Central](https://search.maven.org/artifact/com.uchuhimo/konf) and [JitPack](https://jitpack.io/#uchuhimo/konf).

Konf is modular, you can use different modules for different sources:

- `konf-core`: for built-in sources (JSON, properties, map, command line and system environment)
- `konf-hocon`: for built-in + [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) sources
- `konf-toml`: for built-in + [TOML](https://github.com/toml-lang/toml) sources
- `konf-xml`: for built-in + XML sources
- `konf-yaml`: for built-in + YAML sources
- `konf-git`: for built-in + Git sources
- `konf`: for all sources mentioned above
- `konf-js`: for built-in + JavaScript (use GraalVM JavaScript) sources

### Maven

```xml
<dependency>
  <groupId>com.uchuhimo</groupId>
  <artifactId>konf</artifactId>
  <version>1.1.1</version>
</dependency>
```

### Gradle

```groovy
compile 'com.uchuhimo:konf:1.1.1'
```

### Gradle Kotlin DSL

```kotlin
compile(group = "com.uchuhimo", name = "konf", version = "1.1.1")
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
compile 'com.github.uchuhimo.konf:konf:master-SNAPSHOT'
```

### Gradle Kotlin DSL (master snapshot)

Add JitPack repository:

```kotlin
repositories {
    maven(url = "https://jitpack.io")
}
```

Add dependencies:

```kotlin
compile(group = "com.github.uchuhimo.konf", name = "konf", version = "master-SNAPSHOT")
```

## Quick start

1. Define items in config spec:

    ```kotlin
    object ServerSpec : ConfigSpec() {
        val host by optional("0.0.0.0")
        val tcpPort by required<Int>()
    }
    ```

2. Construct config with items in config spec and values from multiple sources:

    ```kotlin
    val config = Config { addSpec(ServerSpec) }
            .from.yaml.file("server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    ```
   
   or:

    ```kotlin
    val config = Config { addSpec(ServerSpec) }.withSource(
        Source.from.yaml.file("server.yml") +
        Source.from.json.resource("server.json") +
        Source.from.env() +
        Source.from.systemProperties()
    )
    ```

    This config contains all items defined in `ServerSpec`, and load values from 4 different sources. Values in resource file `server.json` will override those in file `server.yml`, values from system environment will override those in `server.json`, and values from system properties will override those from system environment.

    If you want to watch file `server.yml` and reload values when file content is changed, you can use `watchFile` instead of `file`:

    ```kotlin
    val config = Config { addSpec(ServerSpec) }
            .from.yaml.watchFile("server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
    ```

3. Define values in source. You can define in any of these sources:
    - in `server.yml`:
        ```yaml
        server:
            host: 0.0.0.0
            tcp_port: 8080
        ```
    - in `server.json`:
        ```json
        {
            "server": {
                "host": "0.0.0.0",
                "tcp_port": 8080
            }
        }
        ```
    - in system environment:
        ```bash
        SERVER_HOST=0.0.0.0
        SERVER_TCPPORT=8080
        ```
    - in command line for system properties:
        ```bash
        -Dserver.host=0.0.0.0 -Dserver.tcp_port=8080
        ```

4. Retrieve values from config with type-safe APIs:
    ```kotlin
    data class Server(val host: String, val tcpPort: Int) {
        fun start() {}
    }
    
    val server = Server(config[ServerSpec.host], config[ServerSpec.tcpPort])
    server.start()
    ```

5. Retrieve values from multiple sources without using config spec:

    ```kotlin
    val server = Config()
            .from.yaml.file("server.yml")
            .from.json.resource("server.json")
            .from.env()
            .from.systemProperties()
            .at("server")
            .toValue<Server>()
    server.start()
    ```

## Define items

Config items is declared in config spec, added to config by `Config#addSpec`. All items in same config spec have same prefix. Define a config spec with prefix `local.server`:

```kotlin
object ServerSpec : ConfigSpec("server") {
}
```

If the config spec is binding with single class, you can declare config spec as companion object of the class:

```kotlin
class Server {
    companion object : ConfigSpec("server") {
        val host by optional("0.0.0.0")
        val tcpPort by required<Int>()
    }
}
```

The config spec prefix can be automatically inferred from the class name, leading to further simplification like:

```kotlin
object ServerSpec : ConfigSpec() {
}
```

or

```kotlin
class Server {
    companion object : ConfigSpec() {
    }
}
```

Here are some examples showing the inference convention: `Uppercase` to `uppercase`, `lowercase` to `lowercase`, `SuffixSpec` to `suffix`, `TCPService` to `tcpService`.

The config spec can also be nested. For example, the path of `Service.Backend.Login.user` in the following example will be inferred as "service.backend.login.user":

```kotlin
object Service : ConfigSpec() {
    object Backend : ConfigSpec() {
        object Login : ConfigSpec() {
            val user by optional("admin")
        }
    }
}
```

There are three kinds of item:

- Required item. Required item doesn't have default value, thus must be set with value before retrieved in config. Define a required item with description:
    ```kotlin
    val tcpPort by required<Int>(description = "port of server")
    ```
    Or omit the description:
    ```kotlin
    val tcpPort by required<Int>()
    ```
- Optional item. Optional item has default value, thus can be safely retrieved before setting. Define an optional item:
    ```kotlin
    val host by optional("0.0.0.0", description = "host IP of server")
    ```
    Description can be omitted.
- Lazy item. Lazy item also has default value, however, the default value is not a constant, it is evaluated from thunk every time when retrieved. Define a lazy item:
    ```kotlin
    val nextPort by lazy { config -> config[tcpPort] + 1 }
    ```

You can also define config spec in Java, with a more verbose API (compared to Kotlin version in "quick start"):

```java
public class ServerSpec {
  public static final ConfigSpec spec = new ConfigSpec("server");

  public static final OptionalItem<String> host =
      new OptionalItem<String>(spec, "host", "0.0.0.0") {};

  public static final RequiredItem<Integer> tcpPort = new RequiredItem<Integer>(spec, "tcpPort") {};
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

### Cast config to value

Cast config to a value with the target type:

```kotlin
val server = config.toValue<Server>()
```

### Check whether an item exists in config or not

Check whether an item exists in config or not:

```kotlin
config.contains(Server.host)
// or
Server.host in config
```

Check whether an item name exists in config or not:

```kotlin
config.contains("server.host")
// or
"server.host" in config
```

Check whether all values of required items exist in config or not:

```kotlin
config.containsRequired()
```

Throw exception if some required items in config don't have values:

```kotlin
config.validateRequired()
```

### Modify value in config

Associate item with value (type-safe API):

```kotlin
config[Server.tcpPort] = 80
```

Find item with specified name, and associate it with value (unsafe API):

```kotlin
config["server.tcpPort"] = 80
```

Discard associated value of item:

```kotlin
config.unset(Server.tcpPort)
```

Discard associated value of item with specified name:

```kotlin
config.unset("server.tcpPort")
```

Associate item with lazy thunk (type-safe API):

```kotlin
config.lazySet(Server.tcpPort) { it[basePort] + 1 }
```

Find item with specified name, and associate it with lazy thunk (unsafe API):

```kotlin
config.lazySet("server.tcpPort") { it[basePort] + 1 }
```

### Subscribe the update event

Subscribe the update event of an item:

```kotlin
val handler = Server.host.onSet { value -> println("the host has changed to $value") }
```

Subscribe the update event before every set operation:

```kotlin
val handler = Server.host.beforeSet { config, value -> println("the host will change to $value") }
```

or

```kotlin
val handler = config.beforeSet { item, value -> println("${item.name} will change to $value") }
```

Subscribe the update event after every set operation:

```kotlin
val handler = Server.host.afterSet { config, value -> println("the host has changed to $value") }
```

or

```kotlin
val handler = config.afterSet { item, value -> println("${item.name} has changed to $value") }
```

Cancel the subscription:

```kotlin
handler.cancel()
```

### Export value in config as property

Export a read-write property from value in config:

```kotlin
var port by config.property(Server.tcpPort)
port = 9090
check(port == 9090)
```

Export a read-only property from value in config:

```kotlin
val port by config.property(Server.tcpPort)
check(port == 9090)
```

### Fork from another config

```kotlin
val config = Config { addSpec(Server) }
config[Server.tcpPort] = 1000
// fork from parent config
val childConfig = config.withLayer("child")
// child config inherit values from parent config
check(childConfig[Server.tcpPort] == 1000)
// modifications in parent config affect values in child config
config[Server.tcpPort] = 2000
check(config[Server.tcpPort] == 2000)
check(childConfig[Server.tcpPort] == 2000)
// modifications in child config don't affect values in parent config
childConfig[Server.tcpPort] = 3000
check(config[Server.tcpPort] == 2000)
check(childConfig[Server.tcpPort] == 3000)
```

## Load values from source

Use `from` to load values from source doesn't affect values in config, it will return a new child config by loading all values into new layer in child config:

```kotlin
val config = Config { addSpec(Server) }
// values in source is loaded into new layer in child config
val childConfig = config.from.env()
check(childConfig.parent === config)
```

All out-of-box supported sources are declared in [`DefaultLoaders`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/DefaultLoaders.kt), shown below (the corresponding config spec for these samples is [`ConfigForLoad`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/kotlin/com/uchuhimo/konf/source/ConfigForLoad.kt)):

| Type                                                         | Usage                            | Provider                                                     | Sample                                                       |
| ------------------------------------------------------------ | -------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) | `config.from.hocon`              | [`HoconProvider`](https://github.com/uchuhimo/konf/blob/master/konf-hocon/src/main/kotlin/com/uchuhimo/konf/source/hocon/HoconProvider.kt) | [`source.conf`](https://github.com/uchuhimo/konf/blob/master/konf-hocon/src/test/resources/source/source.conf) |
| JSON                                                         | `config.from.json`               | [`JsonProvider`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/json/JsonProvider.kt) | [`source.json`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/resources/source/source.json) |
| properties                                                   | `config.from.properties`         | [`PropertiesProvider`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/properties/PropertiesProvider.kt) | [`source.properties`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/resources/source/source.properties) |
| [TOML](https://github.com/toml-lang/toml)                    | `config.from.toml`               | [`TomlProvider`](https://github.com/uchuhimo/konf/blob/master/konf-toml/src/main/kotlin/com/uchuhimo/konf/source/toml/TomlProvider.kt) | [`source.toml`](https://github.com/uchuhimo/konf/blob/master/konf-toml/src/test/resources/source/source.toml) |
| XML                                                          | `config.from.xml`                | [`XmlProvider`](https://github.com/uchuhimo/konf/blob/master/konf-xml/src/main/kotlin/com/uchuhimo/konf/source/xml/XmlProvider.kt) | [`source.xml`](https://github.com/uchuhimo/konf/blob/master/konf-xml/src/test/resources/source/source.xml) |
| YAML                                                         | `config.from.yaml`               | [`YamlProvider`](https://github.com/uchuhimo/konf/blob/master/konf-yaml/src/main/kotlin/com/uchuhimo/konf/source/yaml/YamlProvider.kt) | [`source.yaml`](https://github.com/uchuhimo/konf/blob/master/konf-yaml/src/test/resources/source/source.yaml) |
| JavaScript                                                   | `config.from.js`                 | [`JsProvider`](https://github.com/uchuhimo/konf/blob/master/konf-js/src/main/kotlin/com/uchuhimo/konf/source/js/JsProvider.kt) | [`source.js`](https://github.com/uchuhimo/konf/blob/master/konf-js/src/test/resources/source/source.js) |
| hierarchical map                                             | `config.from.map.hierarchical`   | -                                                            | [`MapSourceLoadSpec`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/kotlin/com/uchuhimo/konf/source/base/MapSourceLoadSpec.kt) |
| map in key-value format                                      | `config.from.map.kv`             | -                                                            | [`KVSourceSpec`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/kotlin/com/uchuhimo/konf/source/base/KVSourceSpec.kt) |
| map in flat format                                           | `config.from.map.flat`           | -                                                            | [`FlatSourceLoadSpec`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/test/kotlin/com/uchuhimo/konf/source/base/FlatSourceLoadSpec.kt) |
| system environment                                           | `config.from.env()`              | [`EnvProvider`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/env/EnvProvider.kt) | -                                                            |
| system properties                                            | `config.from.systemProperties()` | [`PropertiesProvider`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/properties/PropertiesProvider.kt) | -                                                            |

These sources can also be manually created using their provider, and then loaded into config by `config.withSource(source)`.

All `from` APIs also have their standalone version that return sources without loading them into the config, shown below:

| Type                                                         | Usage                            |
| ------------------------------------------------------------ | -------------------------------- |
| [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) | `Source.from.hocon`              |
| JSON                                                         | `Source.from.json`               |
| properties                                                   | `Source.from.properties`         |
| [TOML](https://github.com/toml-lang/toml)                    | `Source.from.toml`               |
| XML                                                          | `Source.from.xml`                |
| YAML                                                         | `Source.from.yaml`               |
| JavaScript                                                   | `Source.from.js`                 |
| hierarchical map                                             | `Source.from.map.hierarchical`   |
| map in key-value format                                      | `Source.from.map.kv`             |
| map in flat format                                           | `Source.from.map.flat`           |
| system environment                                           | `Source.from.env()`              |
| system properties                                            | `Source.from.systemProperties()` |

Format of system properties source is same with that of properties source. System environment source follows the same mapping convention with properties source, but with the following name convention:

- All letters in name are in uppercase
- `.` in name is replaced with `_`

HOCON/JSON/properties/TOML/XML/YAML/JavaScript source can be loaded from a variety of input format. Use properties source as example:

- From file: `config.from.properties.file("/path/to/file")`
- From watched file: `config.from.properties.watchFile("/path/to/file", 100, TimeUnit.MILLISECONDS)`
  - You can re-trigger the setup process every time the updated file is loaded by `watchFile("/path/to/file") { config, source -> setup(config) }`
- From string: `config.from.properties.string("server.port = 8080")`
- From URL: `config.from.properties.url("http://localhost:8080/source.properties")`
- From watched URL: `config.from.properties.watchUrl("http://localhost:8080/source.properties", 1, TimeUnit.MINUTES)`
  - You can re-trigger the setup process every time the URL is loaded by `watchUrl("http://localhost:8080/source.properties") { config, source -> setup(config) }`
- From Git repository: `config.from.properties.git("https://github.com/uchuhimo/konf.git", "/path/to/source.properties", branch = "dev")`
- From watched Git repository: `config.from.properties.watchGit("https://github.com/uchuhimo/konf.git", "/path/to/source.properties", period = 1, unit = TimeUnit.MINUTES)`
  - You can re-trigger the setup process every time the Git file is loaded by `watchGit("https://github.com/uchuhimo/konf.git", "/path/to/source.properties") { config, source -> setup(config) }`
- From resource: `config.from.properties.resource("source.properties")`
- From reader: `config.from.properties.reader(reader)`
- From input stream: `config.from.properties.inputStream(inputStream)`
- From byte array: `config.from.properties.bytes(bytes)`
- From portion of byte array: `config.from.properties.bytes(bytes, 1, 12)`

If source is from file, file extension can be auto detected. Thus, you can use `config.from.file("/path/to/source.json")` instead of `config.from.json.file("/path/to/source.json")`, or use `config.from.watchFile("/path/to/source.json")` instead of `config.from.json.watchFile("/path/to/source.json")`. Source from URL also support auto-detected extension (use `config.from.url` or `config.from.watchUrl`). The following file extensions can be supported:

| Type       | Extension  |
| ---------- | ---------- |
| HOCON      | conf       |
| JSON       | json       |
| Properties | properties |
| TOML       | toml       |
| XML        | xml        |
| YAML       | yml, yaml  |
| JavaScript | js         |

You can also implement [`Source`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/Source.kt) to customize your new source, which can be loaded into config by `config.withSource(source)`.

### Subscribe the update event for load operation

Subscribe the update event before every load operation:

```kotlin
val handler = config.beforeLoad { source -> println("$source will be loaded") }
```

You can re-trigger the setup process by subscribing the update event after every load operation:

```kotlin
val handler = config.afterLoad { source -> setup(config) }
```

Cancel the subscription:

```kotlin
handler.cancel()
```

### Strict parsing when loading

By default, Konf extracts desired paths from sources and ignores other unknown paths in sources. If you want Konf to throws exception when unknown paths are found, you can enable `FAIL_ON_UNKNOWN_PATH` feature:

```kotlin
config.enable(Feature.FAIL_ON_UNKNOWN_PATH)
    .from.properties.file("server.properties")
    .from.json.resource("server.json")
```

Then `config` will validate paths from both the properties file and the JSON resource. Furthermore, If you want to validate the properties file only, you can use:

```kotlin
config.from.enable(Feature.FAIL_ON_UNKNOWN_PATH).properties.file("/path/to/file")
    .from.json.resource("server.json")
```

### Path substitution

Path substitution is a feature that path references in source will be substituted by their values.

Path substitution rules are shown below:

- Only quoted string value will be substituted. It means that Konf's path substitutions will not conflict with HOCON's substitutions.
- The definition of a path variable is `${path}`, e.g., `${java.version}`.
- The path variable is resolved in the context of the current source.
- If the string value only contains the path variable, it will be replaced by the whole sub-tree in the path; otherwise, it will be replaced by the string value in the path.
- Use `${path:-default}` to provide a default value when the path is unresolved, e.g., `${java.version:-8}`.
- Use `$${path}` to escape the path variable, e.g., `$${java.version}` will be resolved to `${java.version}` instead of the value in `java.version`.
- Path substitution works in a recursive way, so nested path variables like `${jre-${java.specification.version}}` are allowed.
- Konf also supports all key prefix of [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html)'s default interpolator. 

By default, Konf will perform path substitution for every source (except system environment source) when loading them into the config.
You can disable this behaviour by using `config.disable(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)` for the config 
or `source.disabled(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)` for a single source.

By default, Konf will throw exception when some path variables are unresolved. You can use `source.substituted(false)` manually to ignore these unresolved variables.

To resolve path variables refer to other sources, you can merge these sources before loading them into the config.
For example, if we have two sources `source1.json` and `source2.properties`,
`source1.json` is:

```json
{ 
  "base" : { "user" : "konf" , "password" : "passwd" }
}
```

`source2.properties` is:

```properties
connection.jdbc=mysql://${base.user}:${base.password}@server:port
```

use:

```kotlin
config.withSource(
    Source.from.file("source1.json") +
    Source.from.file("source2.properties")
)
```

We can resolve `mysql://${base.user}:${base.password}@server:port` as `mysql://konf:passwd@server:port`.

## Prefix/Merge operations for source/config/config spec

All of source/config/config spec support add prefix operation, remove prefix operation and merge operation as shown below:

| Type     | Add Prefix                                                   | Remove Prefix                                               | Merge                                                  |
| -------- | ------------------------------------------------------------ | ----------------------------------------------------------- | ------------------------------------------------------ |
| `Source` | `source.withPrefix(prefix)` or `Prefix(prefix) + source` or `config.from.prefixed(prefix).file(file)` | `source[prefix]` or `config.from.scoped(prefix).file(file)` | `fallback + facade` or `facade.withFallback(fallback)` |
| `Config` | `config.withPrefix(prefix)` or `Prefix(prefix) + config`     | `config.at(prefix)`                                         | `fallback + facade` or `facade.withFallback(fallback)` |
| `Spec`   | `spec.withPrefix(prefix)` or `Prefix(prefix) + spec`         | `spec[prefix]`                                              | `fallback + facade` or `facade.withFallback(fallback)` |

## Export/Reload values in config

Export all values in config as a tree:

```kotlin
val tree = config.toTree()
```

Export all values in config to map in key-value format:

```kotlin
val map = config.toMap()
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

Config can be saved to a variety of output format in HOCON/JSON/properties/TOML/XML/YAML/JavaScript. Use JSON as example:

- To file: `config.toJson.toFile("/path/to/file")`
- To string: `config.toJson.toText()`
- To writer: `config.toJson.toWriter(writer)`
- To output stream: `config.toJson.toOutputStream(outputStream)`
- To byte array: `config.toJson.toBytes()`

You can also implement [`Writer`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/Writer.kt) to customize your new writer (see [`JsonWriter`](https://github.com/uchuhimo/konf/blob/master/konf-core/src/main/kotlin/com/uchuhimo/konf/source/json/JsonWriter.kt) for how to integrate your writer with config).

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

## Optional features

There are some optional features that you can enable/disable in the config scope or the source scope by `Config#enable(Feature)`/`Config#disable(Feature)` or `Source#enabled(Feature)`/`Source#disable(Feature)`. You can use `Config#isEnabled()` or `Source#isEnabled()` to check whether a feature is enabled.

These features include:

- `FAIL_ON_UNKNOWN_PATH`: feature that determines what happens when unknown paths appear in the source. If enabled, an exception is thrown when loading from the source to indicate it contains unknown paths. This feature is disabled by default.
- `LOAD_KEYS_CASE_INSENSITIVELY`: feature that determines whether loading keys from sources case-insensitively. This feature is disabled by default except for system environment.
- `LOAD_KEYS_AS_LITTLE_CAMEL_CASE`: feature that determines whether loading keys from sources as little camel case. This feature is enabled by default.
- `OPTIONAL_SOURCE_BY_DEFAULT`: feature that determines whether sources are optional by default. This feature is disabled by default.
- `SUBSTITUTE_SOURCE_BEFORE_LOADED`: feature that determines whether sources should be substituted before loaded into config. This feature is enabled by default.

## Build from source

Build library with Gradle using the following command:

```
./gradlew clean assemble
```

Test library with Gradle using the following command:

```
./gradlew clean test
```

Since Gradle has excellent incremental build support, you can usually omit executing the `clean` task.

Install library in a local Maven repository for consumption in other projects via the following command:

```
./gradlew clean install
```

## Breaking Changes

### v0.19.0

Since all sources are substituted before loaded into config by default, all path variables will be substituted now. You can use `config.disable(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)` to disable this change.

### v0.17.0

After migrated to tree-based source APIs, many deprecated APIs are removed, including:

- `Source`: all `isXXX` and `toXXX` APIs
- `Config`: `layer`, `addSource` and `withSourceFrom`

### v0.15

After modularized Konf, `hocon`/`toml`/`xml`/`yaml`/`git`/`watchGit` in `DefaultLoaders` become extension properties/functions and should be imported explicitly.
For example, you should import `com.uchuhimo.konf.source.hocon` before using `config.from.hocon`; in Java, `config.from().hocon` is unavailable, please use `config.from().source(HoconProvider.INSTANCE)` instead.

If you use JitPack, you should use `com.github.uchuhimo.konf:konf:<version>` instead of `com.github.uchuhimo:konf:<version>` now.

### v0.10

APIs in `ConfigSpec` have been updated to support item name's auto-detection, please migrate to new APIs. Here are some examples:

- `val host = optional("host", "0.0.0.0")` to `val host by optional("0.0.0.0")`
- `val port = required<Int>("port")` to `val port by required<Int>()`
- `val nextPort = lazy("nextPort") { config -> config[port] + 1 }` to `val nextPort by lazy { config -> config[port] + 1 }`

# License

© uchuhimo, 2017-2019. Licensed under an [Apache 2.0](./LICENSE) license.
