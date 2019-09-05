import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.util.Properties

val bintrayUserProperty by extra { getPrivateProperty("bintrayUser") }
val bintrayKeyProperty by extra { getPrivateProperty("bintrayKey") }
val ossUserToken by extra { getPrivateProperty("ossUserToken") }
val ossUserPassword by extra { getPrivateProperty("ossUserPassword") }
val gpgPassphrase by extra { getPrivateProperty("gpgPassphrase") }

val wrapper by tasks.existing(Wrapper::class)
wrapper {
    gradleVersion = "5.6.1"
    distributionType = Wrapper.DistributionType.ALL
}

buildscript {
    repositories {
        if (useAliyun) {
            aliyunMaven()
        }
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintrayPlugin}")
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

allprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlin-allopen")
    apply(plugin = "com.dorongold.task-tree")
    apply(plugin = "me.champeau.gradle.jmh")
    apply(plugin = "com.diffplug.gradle.spotless")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.jfrog.bintray")

    group = "com.uchuhimo"
    version = "0.17.0"

    repositories {
        if (useAliyun) {
            aliyunMaven()
        }
        jcenter()
    }
}

subprojects {
    dependencyManagement {
        dependencies {
            dependency("org.apiguardian:apiguardian-api:${Versions.apiguardian}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
            dependency("org.reflections:reflections:${Versions.reflections}")
            dependency("io.methvin:directory-watcher:${Versions.directoryWatcher}")

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
        implementation("org.reflections:reflections")
        implementation("io.methvin:directory-watcher")
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
    }

    java {
        sourceCompatibility = Versions.java
        targetCompatibility = Versions.java
    }

    val test by tasks.existing(Test::class)
    test {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
        val properties = Properties()
        properties.load(rootProject.file("konf-core/src/test/kotlin/com/uchuhimo/konf/source/env/env.properties").inputStream())
        properties.forEach { key, value ->
            environment(key as String, value)
        }
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
            licenseHeaderFile(rootProject.file("config/spotless/apache-license-2.0.java"))
        }
        kotlin {
            ktlint(Versions.ktlint)
            trimTrailingWhitespace()
            endWithNewline()
            // licenseHeaderFile is unstable for Kotlin (i.e. will remove `@file:JvmName` when formatting)
            licenseHeaderFile(rootProject.file("config/spotless/apache-license-2.0.kt"))
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
        outputFormat = "javadoc"
        outputDirectory = tasks.javadoc.get().destinationDir!!.path
        jdkVersion = 8
        linkMapping(delegateClosureOf<LinkMapping> {
            dir = project.rootDir.toPath().resolve("src/main/kotlin").toFile().path.replace('\\', '/')
            url = "https://github.com/uchuhimo/konf/blob/v${project.version}/src/main/kotlin"
            suffix = "#L"
        })
        externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
            url = URL("http://fasterxml.github.io/jackson-databind/javadoc/${Versions.jacksonMinor}/")
        })
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokka)
    }

    val projectDescription = "A type-safe cascading configuration library for Kotlin/Java, " +
        "supporting most configuration formats"
    val projectGroup = project.group as String
    val projectName = if (project.name == "konf-all") "konf" else project.name
    val projectVersion = project.version as String
    val projectUrl = "https://github.com/uchuhimo/konf"

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(sourcesJar.get())
                artifact(javadocJar.get())

                groupId = projectGroup
                artifactId = projectName
                version = projectVersion
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

    configure<BintrayExtension> {
        user = bintrayUserProperty
        key = bintrayKeyProperty
        publish = true
        dryRun = false
        override = true
        setPublications("maven")

        pkg.apply {
            setLabels("kotlin", "config")
            publicDownloadNumbers = true
            repo = "maven"
            userOrg = "uchuhimo"
            name = projectName
            desc = projectDescription
            websiteUrl = projectUrl
            issueTrackerUrl = "$projectUrl/issues"
            vcsUrl = "$projectUrl.git"
            setLicenses("Apache-2.0")

            //Optional version descriptor
            version.apply {
                name = projectVersion
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
            install.configure { dependsOn(publishToMavenLocal) }
            bintrayUpload { dependsOn(check, install) }
        }
    }

    val dependencyUpdates by tasks.existing(DependencyUpdatesTask::class)
    dependencyUpdates {
        revision = "release"
        outputFormatter = "plain"
        resolutionStrategy {
            componentSelection {
                all {
                    val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap", "pr")
                        .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-+]*") }
                        .any { it.matches(candidate.version) }
                    if (rejected) {
                        reject("Release candidate")
                    }
                }
            }
        }
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
