dependencyManagement {
    dependencies {
        dependency("org.graalvm.sdk:graal-sdk:${Versions.graal}")
        dependency("org.graalvm.js:js:${Versions.graal}")
    }
}

dependencies {
    implementation(project(":konf-core"))
    implementation("org.graalvm.sdk:graal-sdk")
    implementation("org.graalvm.js:js")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
