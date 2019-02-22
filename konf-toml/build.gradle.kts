plugins {
    kotlin("jvm")
}

version = "0.13.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":konf-core"))
    implementation(Dependencies.toml)

    //testImplementation(project(":konf-core", "test"))
}

applyTestDependencies()