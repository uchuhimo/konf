plugins {
    kotlin("jvm")
}

version = "0.13.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.stdlibJdk8)
    implementation(project(":konf-core"))
    implementation(Dependencies.yaml)
}

applyTestDependencies()