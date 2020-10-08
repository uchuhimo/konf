dependencyManagement {
    dependencies {
        dependency("org.yaml:snakeyaml:${Versions.yaml}")
    }
}

dependencies {
    api(project(":konf-core"))
    implementation("org.yaml:snakeyaml")

    testImplementation(testFixtures(project(":konf-core")))
}
