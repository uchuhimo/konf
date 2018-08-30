import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.novoda.gradle.release.PublishExtension
import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

fun getPrivateProperty(key: String): String {
    return if (project.rootProject.file("private.properties").exists()) {
        val properties = Properties()
        properties.load(File("private.properties").inputStream())
        properties.getProperty(key)
    } else {
        ""
    }
}

val junitPlatformVersion by extra { "1.2.0" }
val bintrayUserProperty by extra { getPrivateProperty("bintrayUser") }
val bintrayKeyProperty by extra { getPrivateProperty("bintrayKey") }

buildscript {
    repositories {
        val aliyunMavenUrl by extra { "https://maven.aliyun.com/repository/central" }
        maven(url = aliyunMavenUrl)
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
        classpath("com.novoda:bintray-release:0.8.1")
    }
}

plugins {
    `build-scan`
    java
    jacoco
    kotlin("jvm") version "1.2.61"
    kotlin("plugin.allopen") version "1.2.61"
    id("com.dorongold.task-tree") version "1.3"
    id("me.champeau.gradle.jmh") version "0.4.7"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.jetbrains.dokka") version "0.9.17"
}

apply(plugin = "com.novoda.bintray-release")
apply(plugin = "com.jfrog.bintray")

group = "com.uchuhimo"
version = "0.11"

repositories {
    maven(url = extra.get("aliyunMavenUrl") as String)
    jcenter()
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "4.10"
    distributionType = Wrapper.DistributionType.ALL
}

sourceSets {
    register("snippet")
}

val implementation by configurations
val snippetImplementation by configurations
snippetImplementation.extendsFrom(implementation)

fun DependenciesHandler.dependencySet(group: String, version: String, action: DependencySetHandler.() -> Unit) {
    dependencySet(mapOf("group" to group, "version" to version), action)
}

configure<DependencyManagementExtension> {
    dependencies {
        dependency("com.uchuhimo:kotlinx-bimap:1.0")
        dependency("org.apiguardian:apiguardian-api:1.0.0")
        dependency("com.typesafe:config:1.3.3")
        dependency("org.yaml:snakeyaml:1.21")
        dependency("com.moandjiezana.toml:toml4j:0.7.2")
        dependency("org.dom4j:dom4j:2.1.0")
        dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
        dependency("org.eclipse.jgit:org.eclipse.jgit:5.0.1.201806211838-r")

        dependencySet(group = "org.jetbrains.kotlin", version = "1.2.61") {
            entry("kotlin-stdlib")
            entry("kotlin-reflect")
            entry("kotlin-stdlib-jdk8")
        }

        val jacksonVersion = "2.9.6"
        dependencySet(group = "com.fasterxml.jackson.core", version = jacksonVersion) {
            entry("jackson-core")
            entry("jackson-annotations")
            entry("jackson-databind")
        }
        dependency("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    }

    val testImplementation by configurations
    testImplementation.withDependencies {
        dependencies {
            dependency("org.jetbrains.kotlin:kotlin-test:1.2.61")
            dependency("com.natpryce:hamkrest:1.4.2.2")
            dependency("org.hamcrest:hamcrest-all:1.3")
            dependency("com.sparkjava:spark-core:2.7.2")
            dependency("org.slf4j:slf4j-simple:1.7.25")

            dependency("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
            dependencySet(group = "org.junit.jupiter", version = "5.2.0") {
                entry("junit-jupiter-api")
                entry("junit-jupiter-engine")
            }

            dependencySet(group = "org.jetbrains.spek", version = "1.1.5") {
                entry("spek-api")
                entry("spek-data-driven-extension")
                entry("spek-subject-extension")
                entry("spek-junit-platform-engine")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.apiguardian:apiguardian-api")
    implementation("com.uchuhimo:kotlinx-bimap:1.0")
    implementation("com.typesafe:config")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.yaml:snakeyaml")
    implementation("com.moandjiezana.toml:toml4j")
    implementation("org.dom4j:dom4j")
    implementation("org.eclipse.jgit:org.eclipse.jgit")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.natpryce:hamkrest")
    testImplementation("org.hamcrest:hamcrest-all")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.jetbrains.spek:spek-api")
    testImplementation("org.jetbrains.spek:spek-data-driven-extension")
    testImplementation("org.jetbrains.spek:spek-subject-extension")
    testImplementation("com.sparkjava:spark-core")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine")
    testRuntimeOnly("org.slf4j:slf4j-simple")

    jmhImplementation("org.jetbrains.kotlin:kotlin-stdlib")

    val main by sourceSets
    snippetImplementation(main.output)
}

tasks.named("build").configure {
    dependsOn(tasks.named("snippetClasses"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    experimental {
        coroutines = Coroutines.ENABLE
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    environment("SOURCE_TEST_TYPE", "env")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.2"
        languageVersion = "1.2"
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.BenchmarkMode")
    annotation("org.openjdk.jmh.annotations.State")
}

jmh {
    //jvmArgs = ["-Djmh.separateClasspathJAR=true"]
    iterations = 10 // Number of measurement iterations to do.
    //benchmarkMode = ["thrpt"] // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    batchSize = 1
    // Batch size: number of benchmark method calls per operation. (some benchmark modes can ignore this setting)
    fork = 1 // How many times to forks a single benchmark. Use 0 to disable forking altogether
    //operationsPerInvocation = 1 // Operations per invocation.
    timeOnIteration = "1s" // Time to spend at each measurement iteration.
    threads = 4 // Number of worker threads to run with.
    timeout = "10s" // Timeout for benchmark iteration.
    //timeUnit = "ns" // Output time unit. Available time units are: [m, s, ms, us, ns].
    verbosity = "NORMAL" // Verbosity mode. Available modes are: [SILENT, NORMAL, EXTRA]
    warmup = "1s" // Time to spend at each warmup iteration.
    warmupBatchSize = 1 // Warmup batch size: number of benchmark method calls per operation.
    //warmupForks = 0 // How many warmup forks to make for a single benchmark. 0 to disable warmup forks.
    warmupIterations = 10 // Number of warmup iterations to do.
    isZip64 = false // Use ZIP64 format for bigger archives
    jmhVersion = "1.21" // Specifies JMH version
}

spotless {
    java {
        googleJavaFormat("1.6")
        trimTrailingWhitespace()
        endWithNewline()
        // licenseHeaderFile will fail with an empty line after license header,
        // disable it by default
        //licenseHeaderFile rootProject.file("config/spotless/apache-license-2.0.java")
    }
    kotlin {
        ktlint("0.24.0")
        trimTrailingWhitespace()
        endWithNewline()
        // licenseHeaderFile is unstable for Kotlin
        // (i.e. will remove `@file:JvmName` when formatting), disable it by default
        //licenseHeaderFile rootProject.file("config/spotless/apache-license-2.0.kt")
    }
}

jacoco {
    toolVersion = "0.8.1"
}

val jacocoTestReport by tasks.existing(JacocoReport::class) {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

val check by tasks.existing {
    dependsOn(jacocoTestReport)
}

val dokka by tasks.existing(DokkaTask::class) {
    outputFormat = "html"
    val javadoc: Javadoc by tasks
    outputDirectory = javadoc.destinationDir!!.path
    jdkVersion = 8
    kotlinTasks(closureOf<Any> {
        val compileKotlin by tasks
        listOf(compileKotlin)
    })
    linkMapping(delegateClosureOf<LinkMapping> {
        dir = project.rootDir.toPath().resolve("src/main/kotlin").toFile().path
        url = "https://github.com/uchuhimo/konf/blob/v${project.version}/src/main/kotlin"
        suffix = "#L"
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("http://fasterxml.github.io/jackson-databind/javadoc/2.8/")
    })
}

configure<PublishExtension> {
    userOrg = "uchuhimo"
    groupId = project.group as String
    artifactId = rootProject.name
    publishVersion = project.version as String
    licences[0] = "Apache-2.0"
    desc = "A type-safe cascading configuration library for Kotlin/Java," +
        " supporting most configuration formats"
    website = "https://github.com/uchuhimo/konf"
    bintrayUser = bintrayUserProperty
    bintrayKey = bintrayKeyProperty
    dryRun = false
}

tasks {
    val install by registering
    afterEvaluate {
        named("mavenJavadocJar").configure { dependsOn(dokka) }
        install.configure { dependsOn(named("publishToMavenLocal")) }
        named("bintrayUpload").configure { dependsOn(check, install) }
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    revision = "release"
    outputFormatter = "plain"
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
}
