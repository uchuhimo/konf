dependencyManagement {
    dependencies {
        dependency("org.dom4j:dom4j:${Versions.dom4j}")
    }
}

dependencies {
    implementation(project(":konf-core"))
    implementation("org.dom4j:dom4j")

    val test by project(":konf-core").dependencyProject.sourceSets
    testImplementation(test.output)
}
