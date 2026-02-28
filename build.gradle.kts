import org.gradle.kotlin.dsl.register

plugins {
    base
    id("microraft.java-conventions") apply false
}

subprojects {
    pluginManager.withPlugin("java-library") {
        apply(plugin = "microraft.java-conventions")
    }
}

tasks.named("check") {
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("qualityDashboard") {
    description = "Generates a single HTML index for quality reports."
    group = "verification"

    val outputFile = layout.buildDirectory.file("reports/quality/index.html")
    val repositoryRoot = rootProject.layout.projectDirectory.asFile
    outputs.file(outputFile)

    doLast {
        val reportFiles = repositoryRoot.walkTopDown()
            .filter { file ->
                file.isFile && file.extension == "html" && file.invariantSeparatorsPath.contains("/build/reports/")
            }
            .map { report ->
                report.relativeTo(repositoryRoot).invariantSeparatorsPath
            }
            .sorted()
            .toList()

        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head><meta charset=\"utf-8\"><title>MicroRaft Quality Reports</title></head>")
            appendLine("<body>")
            appendLine("<h1>MicroRaft Quality Reports</h1>")
            if (reportFiles.isEmpty()) {
                appendLine("<p>No quality reports were generated.</p>")
            } else {
                appendLine("<ul>")
                reportFiles.forEach { report ->
                    appendLine("<li><a href=\"../../../$report\">$report</a></li>")
                }
                appendLine("</ul>")
            }
            appendLine("</body></html>")
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(html)
        }
    }
}

tasks.register("testAll") {
    description = "Runs the full test suite across all subprojects."
    group = "verification"
    dependsOn(subprojects.map { "${it.path}:test" })
}

tasks.register("benchmark") {
    description = "Runs benchmark tasks for modules that provide them."
    group = "verification"
    dependsOn(":microraft:jmh")
}

tasks.register("mutationTest") {
    description = "Runs mutation testing tasks for modules that provide them."
    group = "verification"
    dependsOn(":microraft:pitest")
}
