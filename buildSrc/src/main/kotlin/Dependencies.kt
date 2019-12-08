import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8
    const val kotlin = "1.3.61"
    const val kotlinApi = "1.3"
    const val junit = "5.5.2"
    const val junitPlatform = "1.5.2"
    const val spek = "1.2.1"
    const val jacksonMinor = "2.10"
    const val jackson = "$jacksonMinor.1"
    const val bintrayPlugin = "1.8.4"
    const val taskTree = "1.4"
    const val jmh = "1.22"
    const val jmhPlugin = "0.5.0"
    const val spotless = "3.26.1"
    const val dependencyManagement = "1.0.8.RELEASE"
    const val dependencyUpdate = "0.27.0"
    // wait for bugfix if using JDK11 (issue: https://github.com/Kotlin/dokka/issues/428)
    const val dokka = "0.10.0"
    const val reflections = "0.9.11"
    const val commonsText = "1.8"
    const val hocon = "1.4.0"
    const val yaml = "1.25"
    const val toml4j = "0.7.2"
    const val graal = "19.3.0"
    const val dom4j = "2.1.1"
    const val jaxen = "1.2.0"
    const val coroutines = "1.3.2-1.3.60"
    const val jgit = "5.5.1.201910021850-r"
    const val hamkrest = "1.7.0.0"
    const val hamcrest = "1.3"
    const val spark = "2.9.1"
    const val slf4j = "1.7.29"
    const val jacoco = "0.8.5"

    const val googleJavaFormat = "1.7"
    const val ktlint = "0.36.0"
}


fun String?.withColon() = this?.let { ":$this" } ?: ""

fun kotlin(module: String, version: String? = null) =
    "org.jetbrains.kotlin:kotlin-$module${version.withColon()}"

fun spek(module: String, version: String? = null) =
    "org.jetbrains.spek:spek-$module${version.withColon()}"

fun jackson(scope: String, module: String, version: String? = null) =
    "com.fasterxml.jackson.$scope:jackson-$scope-$module${version.withColon()}"

fun jacksonCore(module: String = "core", version: String? = null) =
    "com.fasterxml.jackson.core:jackson-$module${version.withColon()}"

fun junit(scope: String, module: String, version: String? = null) =
    "org.junit.$scope:junit-$scope-$module${version.withColon()}"
