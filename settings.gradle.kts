pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
    }
}

rootProject.name = "konf"

include(
    "konf-core",
    "konf-git",
    "konf-hocon",
    "konf-js",
    "konf-toml",
    "konf-xml",
    "konf-yaml",
    "konf-all"
)

plugins {
    id("com.gradle.enterprise") version "3.0"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
