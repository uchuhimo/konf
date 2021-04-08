import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.w3c.dom.Element
import java.util.Properties

fun Project.getPrivateProperty(key: String): String {
    return if (file("private.properties").exists()) {
        val properties = Properties()
        properties.load(file("private.properties").inputStream())
        properties.getProperty(key)
    } else {
        ""
    }
}

fun Project.shouldUseAliyun(): Boolean = if (file("private.properties").exists()) {
    val properties = Properties()
    properties.load(file("private.properties").inputStream())
    properties.getProperty("useAliyun")?.toBoolean() ?: false
} else {
    false
}

fun RepositoryHandler.aliyunMaven() = maven(url = "https://maven.aliyun.com/repository/central")

fun RepositoryHandler.aliyunGradlePluginPortal() = maven(url = "https://maven.aliyun.com/repository/gradle-plugin")

fun Element.appendNode(key: String, action: Element.() -> Unit): Element {
    return apply {
        appendChild(ownerDocument.createElement(key).apply {
            action()
        })
    }
}

fun Element.appendNode(key: String, value: String): Element {
    return appendNode(key) {
        appendChild(ownerDocument.createTextNode(value))
    }
}
