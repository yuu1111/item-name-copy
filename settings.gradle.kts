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

val vcsNode = "1.21.1-fabric"
val selectedTarget = providers.gradleProperty("target").orNull
val nodeDirectories = file("versions").listFiles().orEmpty()
    .filter { it.resolve("gradle.properties").isFile }.sortedBy { it.name }
check(selectedTarget == null || nodeDirectories.any { it.name == selectedTarget }) { "Unknown target: $selectedTarget" }

stonecutter {
    create(rootProject) {
        for (directory in nodeDirectories) {
            if (selectedTarget != null && directory.name !in listOf(vcsNode, selectedTarget)) continue
            val properties = Properties().apply { directory.resolve("gradle.properties").inputStream().use(::load) }
            val minecraft = properties.getProperty("minecraft_version")
            val script = checkNotNull(properties.getProperty("build_script")) { "Missing build_script in ${directory.name}" }
            check(directory.name.substringBefore('-') == minecraft) { "Node/version mismatch: ${directory.name}" }
            version(directory.name, minecraft).buildscript(script)
        }
        vcsVersion = vcsNode
    }
}
