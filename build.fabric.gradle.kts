plugins {
    id("fabric-loom") version "1.17.20"
}

version = property("mod.version") as String
group = "dev.itemnamecopy"
base.archivesName = "item-name-copy"

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/NeoForgeClient.java", "**/ForgeClient.java")
}

tasks.remapJar {
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
    filesMatching("itemnamecopy.mixins.json") { expand("keyboard_mixin" to ", \"FabricKeyboardMixin\"") }
    exclude("META-INF/**")
}
