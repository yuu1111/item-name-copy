plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

version = property("mod.version") as String
group = "com.github.yuu1111"
base.archivesName = "item-name-copy"

repositories { mavenCentral() }

neoForge {
    enable {
        version = project.property("loader_version") as String
        setDisableRecompilation(true)
    }
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
    java.exclude(
        "**/FabricKeyboardMixin.java", "**/FabricKeyBindsScreenMixin.java",
        "**/FabricChordKeyMapping.java", "**/ForgeClient.java", "**/LegacyForgeEvents.java"
    )
    if (org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) >=
        org.gradle.util.GradleVersion.version("1.21.9")) java.exclude("**/KeyMappingAccessor.java")
}

tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+neoforge-mc${project.property("minecraft_version")}.jar"
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE"))
}

apply(from = rootProject.file("gradle/neoforge-resources.gradle.kts"))
apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
