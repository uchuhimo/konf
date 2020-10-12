sourceSets {
    register("snippet")
}

val snippetImplementation by configurations
snippetImplementation.extendsFrom(configurations.implementation.get())

dependencies {
    for (name in listOf(
        ":konf-core",
        ":konf-git",
        ":konf-hocon",
        ":konf-toml",
        ":konf-xml",
        ":konf-yaml"
    )) {
        api(project(name))
        testImplementation(testFixtures(project(name)))
    }

    snippetImplementation(sourceSets.main.get().output)
    val snippet by sourceSets
    testImplementation(snippet.output)
}
