plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

version = property("mod.version") as String
group = "dev.itemnamecopy"
base.archivesName = "item-name-copy"

repositories { mavenCentral() }

neoForge {
    version = project.property("loader_version") as String
    mods {
        register("itemnamecopy") { sourceSet(sourceSets.main.get()) }
    }
    runs {
        register("client") { client() }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/FabricKeyboardMixin.java", "**/ForgeClient.java")
}

tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+neoforge-mc${project.property("minecraft_version")}.jar"
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE"))
}

tasks.processResources {
    val values = mapOf("version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"), "loader" to project.property("loader_version"))
    inputs.properties(values)
    filesMatching("META-INF/neoforge.mods.toml") { expand(values) }
    filesMatching("itemnamecopy.mixins.json") {
        expand("keyboard_mixin" to "", "refmap" to "", "java" to project.property("java_version").toString())
    }
    exclude("fabric.mod.json", "META-INF/mods.toml")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
