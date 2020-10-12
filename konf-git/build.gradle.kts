dependencies {
    api(project(":konf-core"))
    api("org.eclipse.jgit", "org.eclipse.jgit", Versions.jgit)

    testImplementation(testFixtures(project(":konf-core")))
}
