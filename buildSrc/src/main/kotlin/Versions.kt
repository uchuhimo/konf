import org.gradle.api.JavaVersion

object Versions {
    val java = JavaVersion.VERSION_1_8

    val kotlin = "1.3.20"
    val kotlinApi = "1.3"
    val coroutines = "1.1.1"

    // Other
    val bimap = "1.2"
    val jgit = "5.2.1.201812262042-r"
    val apiguardian = "1.0.0"

    // Jackson
    val jacksonMinor = "2.9"
    val jackson = "$jacksonMinor.8"

    // Formats
    val hocon = "1.3.3"
    val yaml = "1.23"
    val toml4j = "0.7.2"
    val dom4j = "2.1.0" // don't upgrade to 2.1.1

    // --- Testing

    val junit = "5.3.2"
    val junitPlatform = "1.3.2"
    val spek = "1.2.1"
    val hamkrest = "1.7.0.0"
    val hamcrest = "1.3"
    val spark = "2.8.0"
    val slf4j = "1.7.25"

    val bintrayPlugin = "1.8.4"
    val taskTree = "1.3.1"
    val jmh = "1.21"
    val jmhPlugin = "0.4.8"
    val spotless = "3.17.0"
    val dependencyUpdate = "0.20.0"
    val dokka = "0.9.17"

    val googleJavaFormat = "1.6"
    val ktlint = "0.29.0"
    val jacoco = "0.8.2"
}
