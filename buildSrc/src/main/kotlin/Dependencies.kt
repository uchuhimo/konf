import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8
    val kotlin = "1.3.50"
    val kotlinApi = "1.3"
    val junit = "5.5.1"
    val junitPlatform = "1.5.1"
    val spek = "1.2.1"
    val jacksonMinor = "2.9"
    val jackson = "$jacksonMinor.9"
    val bintrayPlugin = "1.8.4"
    val taskTree = "1.4"
    val jmh = "1.21"
    val jmhPlugin = "0.4.8"
    val spotless = "3.24.2"
    val dependencyManagement = "1.0.8.RELEASE"
    val dependencyUpdate = "0.24.0"
    // don't upgrade to 0.9.18 (issue: https://github.com/Kotlin/dokka/issues/464)
    // wait for 0.9.20 if using JDK11 (issue: https://github.com/Kotlin/dokka/issues/428)
    val dokka = "0.9.17"
    val apiguardian = "1.1.0"
    val reflections = "0.9.11"
    val directoryWatcher = "0.9.6"
    val hocon = "1.3.4"
    val yaml = "1.25"
    val toml4j = "0.7.2"
    val graal = "19.2.0"
    // don't upgrade to 2.1.1
    val dom4j = "2.1.0"
    val coroutines = "1.3.0"
    val jgit = "5.4.2.201908231537-r"
    val hamkrest = "1.7.0.0"
    val hamcrest = "1.3"
    val spark = "2.9.1"
    val slf4j = "1.7.28"

    val googleJavaFormat = "1.7"
    val ktlint = "0.33.0"
    val jacoco = "0.8.4"
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
