# Konf

[![Java 8+](https://img.shields.io/badge/Java-8+-4c7e9f.svg)](http://java.oracle.com)
[![Bintray](https://api.bintray.com/packages/uchuhimo/maven/konf/images/download.svg)](https://bintray.com/uchuhimo/maven/konf/_latestVersion)
[![JitPack](https://jitpack.io/v/uchuhimo/konf.svg)](https://jitpack.io/#uchuhimo/konf)
[![Build Status](https://travis-ci.org/uchuhimo/konf.svg?branch=master)](https://travis-ci.org/uchuhimo/konf)
[![codecov](https://codecov.io/gh/uchuhimo/konf/branch/master/graph/badge.svg)](https://codecov.io/gh/uchuhimo/konf)

A type safe configuration library for Kotlin, support to load from JSON, XML, YAML, HOCON, TOML, properties, map, command line and environment variables.

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
  <version>0.3</version>
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
compile 'com.uchuhimo:konf:0.3'
```

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
