dependencyManagement {
    dependencies {
        dependency("com.typesafe:config:${Versions.hocon}")
    }
}

dependencies {
    implementation(project(":konf-core"))
    implementation("com.typesafe:config")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
