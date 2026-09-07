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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ItemNameCopy"
include("core")

stonecutter {
    create(rootProject) {
        version("1.20.1-fabric", "1.20.1").buildscript("build.fabric.gradle.kts")
        version("1.21.1-fabric", "1.21.1").buildscript("build.fabric.gradle.kts")
        version("1.21.1-neoforge", "1.21.1").buildscript("build.neoforge.gradle.kts")
        vcsVersion = "1.21.1-fabric"
    }
}
