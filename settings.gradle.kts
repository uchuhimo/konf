pluginManagement {
    repositories {
        jcenter()
//        aliyunGradlePluginPortal()
        gradlePluginPortal()
    }
}

rootProject.name = "konf"

include(
    "konf-core",
    "konf-git",
    "konf-hocon",
    "konf-toml",
    "konf-xml",
    "konf-yaml",
    "konf-all"
)