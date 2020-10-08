sourceSets {
    register("snippet")
}

val snippetImplementation by configurations
snippetImplementation.extendsFrom(configurations.implementation.get())

dependencyManagement {
    dependencies {
        dependency("com.typesafe:config:${Versions.hocon}")
        dependency("org.yaml:snakeyaml:${Versions.yaml}")
        dependency("com.moandjiezana.toml:toml4j:${Versions.toml4j}")
        dependency("org.dom4j:dom4j:${Versions.dom4j}")
        dependency("jaxen:jaxen:${Versions.jaxen}")
        dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    }
}

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
