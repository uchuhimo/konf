import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import com.novoda.gradle.release.PublishExtension
import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

val bintrayUserProperty by extra { getPrivateProperty("bintrayUser") }
val bintrayKeyProperty by extra { getPrivateProperty("bintrayKey") }
val ossUserToken by extra { getPrivateProperty("ossUserToken") }
val ossUserPassword by extra { getPrivateProperty("ossUserPassword") }
val gpgPassphrase by extra { getPrivateProperty("gpgPassphrase") }

buildscript {
    repositories {
        aliyunMaven()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintrayPlugin}")
        classpath("com.novoda:bintray-release:${Versions.bintrayRelease}")
    }
}

plugins {
    `build-scan`
    java
    jacoco
    `maven-publish`
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.allopen") version Versions.kotlin
    id("com.dorongold.task-tree") version Versions.taskTree
    id("me.champeau.gradle.jmh") version Versions.jmhPlugin
    id("com.diffplug.gradle.spotless") version Versions.spotless
    id("io.spring.dependency-management") version Versions.dependencyManagement
    id("com.github.ben-manes.versions") version Versions.dependencyUpdate
    id("org.jetbrains.dokka") version Versions.dokka
}

apply(plugin = "com.novoda.bintray-release")
apply(plugin = "com.jfrog.bintray")

group = "com.uchuhimo"
version = "0.12"

repositories {
    aliyunMaven()
    jcenter()
}

val wrapper by tasks.existing(Wrapper::class)
wrapper {
    gradleVersion = "5.0"
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
        dependency("com.uchuhimo:kotlinx-bimap:${Versions.bimap}")
        dependency("org.apiguardian:apiguardian-api:${Versions.apiguardian}")
        dependency("com.typesafe:config:${Versions.hocon}")
        dependency("org.yaml:snakeyaml:${Versions.yaml}")
        dependency("com.moandjiezana.toml:toml4j:${Versions.toml4j}")
        dependency("org.dom4j:dom4j:${Versions.dom4j}")
        dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
        dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")

        arrayOf("stdlib", "reflect", "stdlib-jdk8").forEach { name ->
            dependency(kotlin(name, Versions.kotlin))
        }

        arrayOf("core", "annotations", "databind").forEach { name ->
            dependency(jacksonCore(name, Versions.jackson))
        }
        dependency(jackson("module", "kotlin", Versions.jackson))
        dependency(jackson("datatype", "jsr310", Versions.jackson))
    }

    val testImplementation by configurations
    testImplementation.withDependencies {
        dependencies {
            dependency(kotlin("test", Versions.kotlin))
            dependency("com.natpryce:hamkrest:${Versions.hamkrest}")
            dependency("org.hamcrest:hamcrest-all:${Versions.hamcrest}")
            dependency("com.sparkjava:spark-core:${Versions.spark}")
            dependency("org.slf4j:slf4j-simple:${Versions.slf4j}")

            dependency(junit("platform", "launcher", Versions.junitPlatform))
            dependency(junit("jupiter", "api", Versions.junit))
            dependency(junit("jupiter", "engine", Versions.junit))

            arrayOf("api", "data-driven-extension", "subject-extension", "junit-platform-engine").forEach { name ->
                dependency(spek(name, Versions.spek))
            }
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.apiguardian:apiguardian-api")
    implementation("com.uchuhimo:kotlinx-bimap")
    implementation("com.typesafe:config")
    implementation("org.yaml:snakeyaml")
    implementation("com.moandjiezana.toml:toml4j")
    implementation("org.dom4j:dom4j")
    implementation("org.eclipse.jgit:org.eclipse.jgit")
    arrayOf("core", "annotations", "databind").forEach { name ->
        implementation(jacksonCore(name))
    }
    implementation(jackson("module", "kotlin"))
    implementation(jackson("datatype", "jsr310"))

    testImplementation(kotlin("test"))
    testImplementation("com.natpryce:hamkrest")
    testImplementation("org.hamcrest:hamcrest-all")
    testImplementation(junit("jupiter", "api"))
    testImplementation("com.sparkjava:spark-core")
    arrayOf("api", "data-driven-extension", "subject-extension").forEach { name ->
        testImplementation(spek(name))
    }

    testRuntimeOnly(junit("platform", "launcher"))
    testRuntimeOnly(junit("jupiter", "engine"))
    testRuntimeOnly(spek("junit-platform-engine"))
    testRuntimeOnly("org.slf4j:slf4j-simple")

    jmhImplementation(kotlin("stdlib"))

    val main by sourceSets
    snippetImplementation(main.output)
}

val build by tasks.existing
val snippetClasses by tasks.existing
build { dependsOn(snippetClasses) }

java {
    sourceCompatibility = Versions.java
    targetCompatibility = Versions.java
}

val test by tasks.existing(Test::class)
test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    environment("SOURCE_TEST_TYPE", "env")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.java.toString()
        apiVersion = Versions.kotlinApi
        languageVersion = Versions.kotlinApi
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
    jmhVersion = Versions.jmh // Specifies JMH version
}

spotless {
    java {
        googleJavaFormat(Versions.googleJavaFormat)
        trimTrailingWhitespace()
        endWithNewline()
        // licenseHeaderFile will fail with an empty line after license header,
        // disable it by default
        //licenseHeaderFile rootProject.file("config/spotless/apache-license-2.0.java")
    }
    kotlin {
        ktlint(Versions.ktlint)
        trimTrailingWhitespace()
        endWithNewline()
        // licenseHeaderFile is unstable for Kotlin
        // (i.e. will remove `@file:JvmName` when formatting), disable it by default
        //licenseHeaderFile rootProject.file("config/spotless/apache-license-2.0.kt")
    }
}

jacoco {
    toolVersion = Versions.jacoco
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
    linkMapping(delegateClosureOf<LinkMapping> {
        dir = project.rootDir.toPath().resolve("src/main/kotlin").toFile().path
        url = "https://github.com/uchuhimo/konf/blob/v${project.version}/src/main/kotlin"
        suffix = "#L"
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("http://fasterxml.github.io/jackson-databind/javadoc/${Versions.jacksonMinor}/")
    })
}

val projectDescription = "A type-safe cascading configuration library for Kotlin/Java, " +
    "supporting most configuration formats"
val projectGroup = project.group as String
val projectName = rootProject.name
val projectVersion = project.version as String
val projectUrl = "https://github.com/uchuhimo/konf"

configure<PublishExtension> {
    userOrg = "uchuhimo"
    groupId = projectGroup
    artifactId = projectName
    publishVersion = projectVersion
    setLicences("Apache-2.0")
    desc = projectDescription
    website = projectUrl
    bintrayUser = bintrayUserProperty
    bintrayKey = bintrayKeyProperty
    dryRun = false
    override = false
}

publishing {
    publications {
        afterEvaluate {
            getByName<MavenPublication>("maven") {
                pom {
                    name.set(rootProject.name)
                    description.set(projectDescription)
                    url.set(projectUrl)
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("uchuhimo")
                            name.set("uchuhimo")
                            email.set("uchuhimo@outlook.com")
                        }
                    }
                    scm {
                        url.set(projectUrl)
                    }
                }
            }
        }
    }
}

configure<BintrayExtension> {
    pkg.apply {
        setLabels("kotlin", "config")
        publicDownloadNumbers = true

        //Optional version descriptor
        version.apply {
            vcsTag = "v$projectVersion"
            //Optional configuration for GPG signing
            gpg.apply {
                sign = true //Determines whether to GPG sign the files. The default is false
                passphrase = gpgPassphrase //Optional. The passphrase for GPG signing'
            }
            //Optional configuration for Maven Central sync of the version
            mavenCentralSync.apply {
                sync = true //[Default: true] Determines whether to sync the version to Maven Central.
                user = ossUserToken //OSS user token: mandatory
                password = ossUserPassword //OSS user password: mandatory
                close = "1" //Optional property. By default the staging repository is closed and artifacts are released to Maven Central. You can optionally turn this behaviour off (by puting 0 as value) and release the version manually.
            }
        }
    }
}

tasks {
    val install by registering
    afterEvaluate {
        val publishToMavenLocal by existing
        val bintrayUpload by existing
        publishToMavenLocal { dependsOn(dokka) }
        install.configure { dependsOn(publishToMavenLocal) }
        bintrayUpload { dependsOn(check, install) }
    }
}

val dependencyUpdates by tasks.existing(DependencyUpdatesTask::class)
dependencyUpdates {
    revision = "release"
    outputFormatter = "plain"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    setTermsOfServiceAgree("yes")
}
