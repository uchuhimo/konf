dependencyManagement {
    dependencies {
        dependency("org.dom4j:dom4j:${Versions.dom4j}")
        dependency("jaxen:jaxen:${Versions.jaxen}")
    }
}

dependencies {
    api(project(":konf-core"))
    implementation("org.dom4j:dom4j")
    implementation("jaxen:jaxen")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
