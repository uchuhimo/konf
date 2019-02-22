import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

object Dependencies {

    // Kotlin
    val stdlib = kotlin("stdlib", Versions.kotlin)
    val stdlibJdk8 = kotlin("stdlib-jdk8", Versions.kotlin)
    val reflect = kotlin("reflect", Versions.kotlin)
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"

    // Other
    val bimap = "com.uchuhimo:kotlinx-bimap:${Versions.bimap}"
    val jgit = "org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}"
    val apiGuardian = "org.apiguardian:apiguardian-api:${Versions.apiguardian}"

    // Jackson
    val jacksonCore = jacksonCore("core", Versions.jackson)
    val jacksonAnnotations = jacksonCore("annotations", Versions.jackson)
    val jacksonDatabind = jacksonCore("databind", Versions.jackson)

    val jacksonKotlin = jackson("module", "kotlin", Versions.jackson)
    val jacksonJsr310 = jackson("datatype", "jsr310", Versions.jackson)

    // Formats
    val hocon = "com.typesafe:config:${Versions.hocon}"
    val yaml = "org.yaml:snakeyaml:${Versions.yaml}"
    val toml = "com.moandjiezana.toml:toml4j:${Versions.toml4j}"
    val dom4j = "org.dom4j:dom4j:${Versions.dom4j}"

    // --- Testing

    val kotlinTest = kotlin("test", Versions.kotlin)
    val hamkrest = "com.natpryce:hamkrest:${Versions.hamkrest}"
    val hamcrest = "org.hamcrest:hamcrest-all:${Versions.hamcrest}"
    val sparkJava = "com.sparkjava:spark-core:${Versions.spark}"
    val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"

    // Junit
    val junitLauncher = junit("platform", "launcher", Versions.junitPlatform)
    val junitApi = junit("jupiter", "api", Versions.junit)
    val junitEngine = junit("jupiter", "engine", Versions.junit)

    // Spek
    val spekApi = spek("api", Versions.spek)
    val spekDataDriven = spek("data-driven-extension", Versions.spek)
    val spekSubject = spek("subject-extension", Versions.spek)
    val spekPlatformEngine = spek("junit-platform-engine", Versions.spek)
}

fun Project.applyTestDependencies() {
    dependencies {
        "testImplementation"(Dependencies.kotlinTest)
        "testImplementation"(Dependencies.hamkrest)
        "testImplementation"(Dependencies.hamcrest)
        "testImplementation"(Dependencies.sparkJava)

        "testImplementation"(Dependencies.junitApi)
        "testImplementation"(Dependencies.spekApi)
        "testImplementation"(Dependencies.spekDataDriven)
        "testImplementation"(Dependencies.spekSubject)

        // Runtime
        "testRuntimeOnly"(Dependencies.junitLauncher)
        "testRuntimeOnly"(Dependencies.junitEngine)
        "testRuntimeOnly"(Dependencies.spekPlatformEngine)
        "testRuntimeOnly"(Dependencies.slf4j)
    }
}

fun kotlin(module: String, version: String) =
    "org.jetbrains.kotlin:kotlin-$module:$version"

fun spek(module: String, version: String) =
    "org.jetbrains.spek:spek-$module:$version"

fun jackson(scope: String, module: String, version: String) =
    "com.fasterxml.jackson.$scope:jackson-$scope-$module:$version"

fun jacksonCore(module: String, version: String) =
    "com.fasterxml.jackson.core:jackson-$module:$version"

fun junit(scope: String, module: String, version: String) =
    "org.junit.$scope:junit-$scope-$module:$version"