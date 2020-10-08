dependencyManagement {
    dependencies {
        dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    }
}

dependencies {
    api(project(":konf-core"))
    api("org.eclipse.jgit:org.eclipse.jgit")

    testImplementation(testFixtures(project(":konf-core")))
}
