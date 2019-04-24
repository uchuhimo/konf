import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8
    val kotlin = "1.3.21"
    val kotlinApi = "1.3"
    val junit = "5.4.1"
    val junitPlatform = "1.4.1"
    val spek = "1.2.1"
    val jacksonMinor = "2.9"
    val jackson = "$jacksonMinor.8"
    val bintrayPlugin = "1.8.4"
    val taskTree = "1.3.1"
    val jmh = "1.21"
    val jmhPlugin = "0.4.8"
    val spotless = "3.20.0"
    val dependencyManagement = "1.0.7.RELEASE"
    val dependencyUpdate = "0.21.0"
    // don't upgrade to 0.9.18
    val dokka = "0.9.17"
    val apiguardian = "1.0.0"
    val hocon = "1.3.3"
    val yaml = "1.24"
    val toml4j = "0.7.2"
    // don't upgrade to 2.1.1
    val dom4j = "2.1.0"
    val coroutines = "1.1.1"
    val jgit = "5.3.0.201903130848-r"
    val hamkrest = "1.7.0.0"
    val hamcrest = "1.3"
    val spark = "2.8.0"
    val slf4j = "1.7.26"

    val googleJavaFormat = "1.7"
    val ktlint = "0.31.0"
    val jacoco = "0.8.3"
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
