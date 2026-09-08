import java.util.Properties

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ItemNameCopy"
include("core")
includeBuild("tests/agent")

val vcsNode = "1.21.1-fabric"
val selectedTargets = providers.gradleProperty("target").orNull?.split(',')?.map { it.trim() }?.toSet()
val nodeDirectories = file("versions").listFiles().orEmpty()
    .filter { it.resolve("gradle.properties").isFile }.sortedBy { it.name }
val unknownTargets = selectedTargets.orEmpty() - nodeDirectories.map { it.name }.toSet()
check(unknownTargets.isEmpty()) { "Unknown targets: ${unknownTargets.joinToString()}" }

stonecutter {
    create(rootProject) {
        for (directory in nodeDirectories) {
            if (selectedTargets != null && directory.name != vcsNode && directory.name !in selectedTargets) continue
            val properties = Properties().apply { directory.resolve("gradle.properties").inputStream().use(::load) }
            val minecraft = properties.getProperty("minecraft_version")
            val script =
                checkNotNull(properties.getProperty("build_script")) { "Missing build_script in ${directory.name}" }
            check(directory.name.substringBefore('-') == minecraft) { "Node/version mismatch: ${directory.name}" }
            version(directory.name, minecraft).buildscript(script)
        }
        vcsVersion = vcsNode
    }
}
