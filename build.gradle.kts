import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    `build-scan`
}

val wrapper by tasks.existing(Wrapper::class)
wrapper {
    gradleVersion = "5.1.1"
    distributionType = Wrapper.DistributionType.ALL
}

subprojects {
    group = "com.uchuhimo"

    repositories {
        //mavenCentral()
        aliyunMaven()
        jcenter()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = Versions.java.toString()
            apiVersion = Versions.kotlinApi
            languageVersion = Versions.kotlinApi
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    setTermsOfServiceAgree("yes")
}

val projectDescription =
        "A type-safe cascading configuration library for Kotlin/Java, supporting most configuration formats"
val projectGroup = project.group as String
val projectName = rootProject.name
val projectVersion = project.version as String
val projectUrl = "https://github.com/uchuhimo/konf"