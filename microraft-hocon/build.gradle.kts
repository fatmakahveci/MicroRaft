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
    moduleName = "io.microraft.hocon"
    readableName = "MicroRaft HOCON Config Parser"
    description = "HOCON config parser for MicroRaft"
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
    api(libs.typesafe.config)
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
                implementation(testFixtures(project(":microraft")))
            }
        }
    }
}
