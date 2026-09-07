import groovy.json.JsonOutput
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-fabric"

stonecutter parameters {
    constants {
        match(current.project.substringAfter('-'), "fabric", "forge", "neoforge")
    }
    constants["forge_without_mixins"] = current.project.endsWith("-forge") && current.parsed < "1.15.2"
    val targetProperties = Properties().apply {
        rootProject.file("versions/${current.project}/gradle.properties").inputStream().use(::load)
    }
    val mcpMembers = targetProperties.getProperty("mappings_channel") == "snapshot"
    constants["mcp_names"] = mcpMembers
    val legacyNames = Properties().apply {
        rootProject.file("gradle/legacy-forge-names.properties").inputStream().use(::load)
    }
    replacements.regex(current.project.endsWith("-forge") && current.parsed < "1.17") {
        legacyNames.stringPropertyNames().sorted().forEach { named ->
            val legacy = legacyNames.getProperty(named)
            replace("\\b${Regex.escape(named)}\\b", legacy, "\\b${Regex.escape(legacy)}\\b", named)
            val namedClass = named.substringAfterLast('.')
            val legacyClass = legacy.substringAfterLast('.')
            if (namedClass != legacyClass) {
                replace("\\b$namedClass\\b", legacyClass, "\\b$legacyClass\\b", namedClass)
            }
        }
    }
    val memberNames = Properties().apply {
        rootProject.file("gradle/mcp-member-names.properties").inputStream().use(::load)
    }
    replacements.regex(mcpMembers) {
        memberNames.stringPropertyNames().sorted().forEach { named ->
            if (named == "children" && current.parsed < "1.16") return@forEach
            val legacy = memberNames.getProperty(named)
            if (named == "screen") {
                replace("(?<=\\.)$named\\b(?!\\s*\\()", legacy,
                    "(?<=\\.)$legacy\\b(?!\\s*\\()", named)
            } else {
                replace("\\b$named\\b", legacy, "\\b$legacy\\b", named)
            }
        }
    }
}

gradle.projectsEvaluated {
    val targets = subprojects.filter { it.name != "core" }
    tasks.register<Sync>("buildAndCollect") {
        group = "build"
        dependsOn(":core:test")
        into(layout.buildDirectory.dir("libs"))
        for (target in targets) {
            dependsOn("${target.path}:build")
            val loader = target.name.substringAfter('-')
            val minecraft = target.property("minecraft_version")
            from(target.layout.buildDirectory.file("libs/item-name-copy-${target.version}+$loader-mc$minecraft.jar"))
        }
        doLast {
            val count = destinationDir.listFiles { file -> file.extension == "jar" }?.size ?: 0
            check(count == targets.size) { "Expected ${targets.size} jars but collected $count" }
            val artifacts = targets.map { target ->
                val loader = target.name.substringAfter('-')
                val minecraft = target.property("minecraft_version")
                val file = destinationDir.resolve("item-name-copy-${target.version}+$loader-mc$minecraft.jar")
                val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                mapOf("target" to target.name, "file" to file.name,
                    "sha256" to digest.joinToString("") { "%02x".format(it) }, "runtimeVerification" to "pending")
            }
            destinationDir.resolve("verification-manifest.json").writeText(
                JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("schemaVersion" to 1, "artifacts" to artifacts)))
            )
            logger.lifecycle("Collected $count individually verified target jars")
        }
    }
}
