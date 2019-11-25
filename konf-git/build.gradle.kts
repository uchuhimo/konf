dependencyManagement {
    dependencies {
        dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    }
}

dependencies {
    api(project(":konf-core"))
    api("org.eclipse.jgit:org.eclipse.jgit")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
