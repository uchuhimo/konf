dependencyManagement {
    dependencies {
        dependency("org.yaml:snakeyaml:${Versions.yaml}")
    }
}

dependencies {
    api(project(":konf-core"))
    implementation("org.yaml:snakeyaml")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
