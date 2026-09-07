import groovy.json.JsonOutput
import java.security.MessageDigest

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-fabric"

stonecutter parameters {
    constants {
        match(current.project.substringAfter('-'), "fabric", "forge", "neoforge")
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
