pluginManagement {
    repositories {
        jcenter()
//        aliyunGradlePluginPortal()
        gradlePluginPortal()
    }
}

rootProject.name = "konf"

include("konf-core",
        "konf-toml",
        "konf-jgit")
