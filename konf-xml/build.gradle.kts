dependencies {
    api(project(":konf-core"))
    implementation("org.dom4j", "dom4j", Versions.dom4j)
    implementation("jaxen", "jaxen", Versions.jaxen)

    testImplementation(testFixtures(project(":konf-core")))
}
