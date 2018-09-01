import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8
    val kotlin = "1.2.61"
    val kotlinApi = "1.2"
    val junit = "5.2.0"
    val junitPlatform = "1.2.0"
    val spek = "1.2.0"
    val jacksonMinor = "2.9"
    val jackson = "$jacksonMinor.6"
    val bintrayPlugin = "1.8.4"
    val bintrayRelease = "0.8.1"
    val taskTree = "1.3"
    val jmh = "1.21"
    val jmhPlugin = "0.4.7"
    val spotless = "3.14.0"
    val dependencyManagement = "1.0.6.RELEASE"
    val dependencyUpdate = "0.20.0"
    val dokka = "0.9.17"
    val bimap = "1.1"
    val apiguardian = "1.0.0"
    val hocon = "1.3.3"
    val yaml = "1.23"
    val toml4j = "0.7.2"
    val dom4j = "2.1.0"
    val coroutines = "0.25.0"
    val jgit = "5.0.2.201807311906-r"
    val hamkrest = "1.6.0.0"
    val hamcrest = "1.3"
    val spark = "2.7.2"
    val slf4j = "1.7.25"
    val googleJavaFormat = "1.6"
    val ktlint = "0.24.0"
    val jacoco = "0.8.1"
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
