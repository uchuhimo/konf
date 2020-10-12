dependencies {
    api(project(":konf-core"))
    implementation("com.moandjiezana.toml", "toml4j", Versions.toml4j)

    testImplementation(testFixtures(project(":konf-core")))
}
