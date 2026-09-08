plugins {
    id("dev.kikugie.loom-back-compat")
}

version = property("mod.version") as String
group = "dev.itemnamecopy"
base.archivesName = "item-name-copy"

repositories {
    mavenCentral()
}

val legacyMappings = org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) <
    org.gradle.util.GradleVersion.version("1.14.4")
if (legacyMappings) apply(from = rootProject.file("gradle/legacy-fabric-mappings.gradle.kts"))

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    if (legacyMappings) mappings(project.extra["legacyFabricMappings"].toString())
    else loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/NeoForgeClient.java", "**/ForgeClient.java", "**/LegacyForgeEvents.java")
}

loomx.modJar.configure {
    archiveFileName = "item-name-copy-${project.version}+fabric-mc${project.property("minecraft_version")}.jar"
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE"))
}

tasks.processResources {
    val values = mapOf("version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"), "loader" to project.property("loader_version"))
    inputs.properties(values)
    filesMatching("fabric.mod.json") { expand(values) }
    exclude("META-INF/**")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
