dependencyManagement {
    dependencies {
        dependency("com.moandjiezana.toml:toml4j:${Versions.toml4j}")
    }
}

dependencies {
    implementation(project(":konf-core"))
    implementation("com.moandjiezana.toml:toml4j")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
