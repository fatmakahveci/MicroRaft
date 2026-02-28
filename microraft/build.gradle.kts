import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    `java-library`
    `java-test-fixtures`
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.javadocLinks)
    `maven-publish`
    signing
    alias(libs.plugins.mavenCentralPublishing)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.pitest)
    checkstyle
}

metadata {
    moduleName = "io.microraft"
    readableName = "Microraft"
    description = "Feature-complete implementation of the Raft consensus algorithm"
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

tasks.javadoc {
    exclude("io/microraft/model/**")
    exclude("**/impl/**")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(rootProject.file("config/spotbugs/spotbugs-ignore.xml"))
}

val sourceSets = the<SourceSetContainer>()
val mainSourceSet = sourceSets.named("main").get()
val testSourceSet = sourceSets.named("test").get()

val jmhSourceSet = sourceSets.create("jmh") {
    java.srcDir("src/jmh/java")
    resources.srcDir("src/jmh/resources")
    compileClasspath += mainSourceSet.output + testSourceSet.runtimeClasspath
    runtimeClasspath += output + compileClasspath
}

dependencies {
    implementation(libs.slf4j.api)
    compileOnly(libs.findbugs.annotations)
    "jmhImplementation"(libs.jmh.core)
    "jmhAnnotationProcessor"(libs.jmh.generator.annprocess)
}

@Suppress("UnstableApiUsage") //
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnit(libs.versions.junit)
        }
        named<JvmTestSuite>("test") {
            targets.all {
                testTask.configure {
                    maxParallelForks = 4
                }
            }
            dependencies {
                implementation(libs.assertj)
                implementation(libs.mockito)
                runtimeOnly(libs.log4j.slf4j.impl)
                compileOnly(libs.findbugs.annotations)
            }
        }
    }
}

dependencies {
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(libs.slf4j.api)
    testFixturesCompileOnly(libs.findbugs.annotations)
}

// Do not publish test fixtures for now
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
javaComponent.withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }

publishing {
    // TODO Remove after debugging
    repositories {
        maven {
            this.name = "TestPublish"
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri(layout.buildDirectory.dir("repos/releases"))
            val snapshotsRepoUrl = uri(layout.buildDirectory.dir("repos/snapshots"))
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

tasks.register<JavaExec>("jmh") {
    description = "Runs the MicroRaft JMH benchmark suite."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    dependsOn(tasks.named(jmhSourceSet.classesTaskName))
    classpath = jmhSourceSet.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    val reportFile = layout.buildDirectory.file("reports/jmh/results.json")
    doFirst {
        reportFile.get().asFile.parentFile.mkdirs()
    }
    args(
        "-rf",
        "json",
        "-rff",
        reportFile.get().asFile.absolutePath,
    )
}

configure<PitestPluginExtension> {
    targetClasses.set(
        setOf(
            "io.microraft.impl.util.OrderedFuture",
        ),
    )
    targetTests.set(
        setOf(
            "io.microraft.impl.util.OrderedFutureTest",
        ),
    )
    threads.set(1)
    outputFormats.set(setOf("HTML", "XML"))
    reportDir.set(layout.buildDirectory.dir("reports/pitest").get().asFile)
    timestampedReports.set(false)
    mainSourceSets.set(setOf(mainSourceSet))
    testSourceSets.set(setOf(testSourceSet))
    useClasspathFile.set(true)
    timeoutFactor.set(2.5.toBigDecimal())
    timeoutConstInMillis.set(8000)
}

listOf("checkstyleJmh", "spotbugsJmh").forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        enabled = false
    }
}
