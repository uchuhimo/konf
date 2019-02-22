pluginManagement {
    repositories {
        jcenter()
//        aliyunGradlePluginPortal()
        gradlePluginPortal()
    }
}

rootProject.name = "konf"

include("konf-core",
        "konf-hocon",
        "konf-toml",
        "konf-xml",
        "konf-yaml",
        "konf-jgit")
