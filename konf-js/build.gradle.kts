dependencies {
    api(project(":konf-core"))
    implementation("org.graalvm.sdk", "graal-sdk", Versions.graal)
    implementation("org.graalvm.js", "js", Versions.graal)

    testImplementation(testFixtures(project(":konf-core")))
}
