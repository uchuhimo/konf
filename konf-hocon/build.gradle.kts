dependencies {
    api(project(":konf-core"))
    implementation("com.typesafe", "config", Versions.hocon)

    testImplementation(testFixtures(project(":konf-core")))
}
