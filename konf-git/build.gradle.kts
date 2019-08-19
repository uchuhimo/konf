dependencyManagement {
    dependencies {
        dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    }
}

dependencies {
    implementation(project(":konf-core"))
    implementation("org.eclipse.jgit:org.eclipse.jgit")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
