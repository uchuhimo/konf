sourceSets {
    register("snippet")
}

val implementation by configurations
val snippetImplementation by configurations
snippetImplementation.extendsFrom(implementation)

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
        implementation(project(name))
        testImplementation(project(name).dependencyProject.sourceSets["test"].output)
    }

    val main by sourceSets
    snippetImplementation(main.output)
    val snippet by sourceSets
    testImplementation(snippet.output)
}
