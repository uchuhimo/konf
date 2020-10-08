dependencyManagement {
    dependencies {
        dependency("com.typesafe:config:${Versions.hocon}")
    }
}

dependencies {
    api(project(":konf-core"))
    implementation("com.typesafe:config")

    testImplementation(testFixtures(project(":konf-core")))
}
