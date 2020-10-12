dependencies {
    api(project(":konf-core"))
    implementation("org.yaml", "snakeyaml", Versions.yaml)

    testImplementation(testFixtures(project(":konf-core")))
}
