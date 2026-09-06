plugins {
    `java-library`
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.javadocLinks)
    `maven-publish`
    signing
    alias(libs.plugins.mavenCentralPublishing)
    alias(libs.plugins.spotbugs)
    checkstyle
}

metadata {
    moduleName = "io.microraft.tutorial"
    readableName = "MicroRaft Tutorial"
    description = "Tutorial for MicroRaft"
    license {
        apache2()
    }
    organization {
        name = "MicroRaft"
        url = "https://microraft.io"
    }
    developers {
        register("metanet") {
            fullName = "Ensar Basri Kahveci"
            email = "ebkahveci@gmail.com"
        }
        register("mdogan") {
            fullName = "Mehmet Dogan"
            email = "mehmet@dogan.io"
        }
    }
    github {
        org = "MicroRaft"
        pages()
        issues()
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(rootProject.file("config/spotbugs/spotbugs-ignore.xml"))
}

dependencies {
    api(project(":microraft"))
    implementation(libs.slf4j.api)
    compileOnly(libs.findbugs.annotations)
}

@Suppress("UnstableApiUsage") //
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnit(libs.versions.junit)
        }
        named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.assertj)
                runtimeOnly(libs.log4j.slf4j.impl)
            }
        }
    }
}
