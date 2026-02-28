import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

group = "io.microraft.buildlogic"

val configuredJavaVersion = providers.gradleProperty("microraft.javaVersion").map(String::toInt).orElse(11)

project.group = "io.microraft"
project.version = "0.9-SNAPSHOT"

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(configuredJavaVersion.get())
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Jar>().configureEach {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Vendor" to "MicroRaft",
        "Implementation-Version" to provider { project.version.toString() },
    )
}

tasks.withType<Test>().configureEach {
    reports.html.required.set(true)
    reports.junitXml.required.set(true)
    testLogging {
        events("failed", "skipped")
    }
}

pluginManager.withPlugin("checkstyle") {
    extensions.configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        configDirectory.set(rootProject.layout.projectDirectory.dir("config/checkstyle"))
    }

    tasks.withType<org.gradle.api.plugins.quality.Checkstyle>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
}

tasks.matching { it.name.startsWith("spotbugs") }.configureEach {
    notCompatibleWithConfigurationCache("SpotBugs exclude filter is wired dynamically in the convention plugin.")
    inputs.file(rootProject.file("config/spotbugs/spotbugs-ignore.xml"))
    doFirst {
        val taskClass = javaClass
        val method = taskClass.methods.find { candidate ->
            candidate.name == "setExcludeFilter" && candidate.parameterTypes.contentEquals(arrayOf(java.io.File::class.java))
        }
        method?.invoke(this, rootProject.file("config/spotbugs/spotbugs-ignore.xml"))
    }
}

pluginManager.withPlugin("maven-publish") {
    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        if (publications.findByName("main") == null) {
            publications.create<org.gradle.api.publish.maven.MavenPublication>("main") {
                from(components["java"])
            }
        }
    }
}

pluginManager.withPlugin("signing") {
    extensions.configure<org.gradle.plugins.signing.SigningExtension> {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)

        val publishing = extensions.getByType<org.gradle.api.publish.PublishingExtension>()
        sign(publishing.publications.getByName("main"))
    }
}
