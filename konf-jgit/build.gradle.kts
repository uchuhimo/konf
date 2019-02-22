plugins {
    kotlin("jvm")
}

version = "0.13.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":konf-core"))
    implementation(Dependencies.stdlibJdk8)
    implementation(Dependencies.coroutines)
    implementation(Dependencies.jgit)
}

applyTestDependencies()