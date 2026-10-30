plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Checks Clean Architecture boundaries and portable commonMain sources."
    doLast {
        val violations = mutableListOf<String>()
        listOf("domain", "data", "presentation").forEach { layer ->
            fileTree("$layer/src/commonMain/kotlin").matching { include("**/*.kt") }.forEach { source ->
                source.readLines().forEachIndexed { index, line ->
                    val text = line.trim()
                    val platformImport = Regex("^import (android\\.|java\\.|javax\\.|androidx\\.(activity|work|core)\\.)").containsMatchIn(text)
                    val transportInPresentation = layer == "presentation" &&
                        (text.startsWith("import io.ktor.") || text.contains("pe.edu.hzg.tesoreria.data.") ||
                            Regex("pe\\.edu\\.hzg\\.tesoreria\\.(features\\.\\w+|admin)\\.data\\.").containsMatchIn(text))
                    val frameworkInDomain = layer == "domain" &&
                        Regex("^import (androidx\\.|io\\.ktor\\.|app\\.cash\\.|org\\.koin\\.|kotlinx\\.serialization\\.)").containsMatchIn(text)
                    if (platformImport || transportInPresentation || frameworkInDomain) {
                        violations += "${source.relativeTo(rootDir)}:${index + 1}: $text"
                    }
                }
            }
        }
        check(violations.isEmpty()) { "Architecture violations:\n${violations.joinToString("\n")}" }
        check(!file("presentation/build.gradle.kts").readText().contains("project(\":data\")")) {
            "Presentation must depend on domain contracts, never on data implementations."
        }
        logger.lifecycle("Clean Architecture boundaries verified for domain, data and presentation.")
    }
}
