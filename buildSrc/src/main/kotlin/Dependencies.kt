import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8
    const val kotlin = "1.3.50"
    const val kotlinApi = "1.3"
    const val junit = "5.5.1"
    const val junitPlatform = "1.5.1"
    const val spek = "1.2.1"
    const val jacksonMinor = "2.9"
    const val jackson = "$jacksonMinor.9"
    const val bintrayPlugin = "1.8.4"
    const val taskTree = "1.4"
    const val jmh = "1.21"
    const val jmhPlugin = "0.4.8"
    const val spotless = "3.24.2"
    const val dependencyManagement = "1.0.8.RELEASE"
    const val dependencyUpdate = "0.24.0"
    // don't upgrade to 0.9.18 (issue: https://github.com/Kotlin/dokka/issues/464)
    // wait for 0.9.20 if using JDK11 (issue: https://github.com/Kotlin/dokka/issues/428)
    const val dokka = "0.9.17"
    const val reflections = "0.9.11"
    const val commonsText = "1.8"
    const val hocon = "1.3.4"
    const val yaml = "1.25"
    const val toml4j = "0.7.2"
    const val graal = "19.2.0"
    // don't upgrade to 2.1.1
    const val dom4j = "2.1.0"
    const val coroutines = "1.3.0"
    const val jgit = "5.4.2.201908231537-r"
    const val hamkrest = "1.7.0.0"
    const val hamcrest = "1.3"
    const val spark = "2.9.1"
    const val slf4j = "1.7.28"

    const val googleJavaFormat = "1.7"
    const val ktlint = "0.33.0"
    const val jacoco = "0.8.4"
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
