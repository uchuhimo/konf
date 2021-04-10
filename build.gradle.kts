import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.util.Properties

val ossUserToken by extra { getPrivateProperty("ossUserToken") }
val ossUserPassword by extra { getPrivateProperty("ossUserPassword") }
val signPublications by extra { getPrivateProperty("signPublications") }
val useAliyun by extra { shouldUseAliyun() }

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "7.0"
    distributionType = Wrapper.DistributionType.ALL
}

buildscript {
    repositories {
        if (shouldUseAliyun()) {
            aliyunMaven()
        } else {
            mavenCentral()
        }
    }
}

plugins {
    java
    `java-test-fixtures`
    jacoco
    `maven-publish`
    signing
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.allopen") version Versions.kotlin
    id("com.dorongold.task-tree") version Versions.taskTree
    id("me.champeau.gradle.jmh") version Versions.jmhPlugin
    id("com.diffplug.spotless") version Versions.spotless
    id("com.github.ben-manes.versions") version Versions.dependencyUpdate
    id("org.jetbrains.dokka") version Versions.dokka
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-test-fixtures")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlin-allopen")
    apply(plugin = "com.dorongold.task-tree")
    apply(plugin = "me.champeau.gradle.jmh")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jetbrains.dokka")

    group = "com.uchuhimo"
    version = "1.1.2"

    repositories {
        if (useAliyun) {
            aliyunMaven()
        } else {
            mavenCentral()
        }
        maven {
            url=uri("https://kotlin.bintray.com/kotlinx")
        }
    }

    val dependencyUpdates by tasks.existing(DependencyUpdatesTask::class)
    dependencyUpdates {
        revision = "release"
        outputFormatter = "plain"
        resolutionStrategy {
            componentSelection {
                all {
                    val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap", "pr", "dev", "mt")
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

subprojects {
    configurations.testFixturesImplementation.get().extendsFrom(configurations.implementation.get())
    configurations.testImplementation.get().extendsFrom(configurations.testFixturesImplementation.get())

    dependencies {
        api(kotlin("stdlib-jdk8", Versions.kotlin))
        api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)
        implementation(kotlin("reflect", Versions.kotlin))
        implementation("org.reflections", "reflections", Versions.reflections)
        implementation("org.apache.commons", "commons-text", Versions.commonsText)
        arrayOf("core", "annotations", "databind").forEach { name ->
            api(jacksonCore(name, Versions.jackson))
        }
        implementation(jackson("module", "kotlin", Versions.jackson))
        implementation(jackson("datatype", "jsr310", Versions.jackson))

        testFixturesImplementation(kotlin("test", Versions.kotlin))
        testFixturesImplementation("com.natpryce", "hamkrest", Versions.hamkrest)
        testFixturesImplementation("org.hamcrest", "hamcrest-all", Versions.hamcrest)
        testImplementation(junit("jupiter", "api", Versions.junit))
        testImplementation("com.sparkjava", "spark-core", Versions.spark)
        arrayOf("api", "data-driven-extension", "subject-extension").forEach { name ->
            testFixturesImplementation(spek(name, Versions.spek))
        }

        testRuntimeOnly(junit("platform", "launcher", Versions.junitPlatform))
        testRuntimeOnly(junit("jupiter", "engine", Versions.junit))
        testRuntimeOnly(spek("junit-platform-engine", Versions.spek))
        testRuntimeOnly("org.slf4j", "slf4j-simple", Versions.slf4j)
    }

    java {
        sourceCompatibility = Versions.java
        targetCompatibility = Versions.java
    }

    val test by tasks.existing(Test::class)
    test {
        useJUnitPlatform()
        testLogging.apply {
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        systemProperties["org.slf4j.simpleLogger.defaultLogLevel"] = "warn"
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
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
            languageVersion = Versions.kotlinLanguage
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

    tasks.dokkaHtml {
        outputDirectory.set(tasks.javadoc.get().destinationDir)
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(9)
                reportUndocumented.set(false)
                sourceLink {
                    localDirectory.set(file("./"))
                    remoteUrl.set(URL("https://github.com/uchuhimo/konf/blob/v${project.version}/"))
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(tasks.dokkaHtml)
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

                suppressPomMetadataWarningsFor("testFixturesApiElements")
                suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
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
        repositories {
            maven {
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                credentials {
                    username = ossUserToken
                    password = ossUserPassword
                }
            }
        }
    }

    signing {
        setRequired({ signPublications == "true" })
        sign(publishing.publications["maven"])
    }

    tasks {
        val install by registering
        afterEvaluate {
            val publishToMavenLocal by existing
            val publish by existing
            install.configure { dependsOn(publishToMavenLocal) }
            publish { dependsOn(check, install) }
        }
    }
}
