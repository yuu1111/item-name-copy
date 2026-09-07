val minecraftVersion = org.gradle.util.GradleVersion.version(property("minecraft_version").toString())
val legacyMetadata = minecraftVersion < org.gradle.util.GradleVersion.version("1.20.5")
val fmlVersion = when {
    minecraftVersion < org.gradle.util.GradleVersion.version("1.20.3") -> 1
    legacyMetadata -> 2
    minecraftVersion < org.gradle.util.GradleVersion.version("1.21") -> 3
    else -> 4
}

tasks.named<ProcessResources>("processResources") {
    val values = mapOf("version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"), "loader" to project.property("loader_version"),
        "fml" to fmlVersion, "dependency_kind" to if (fmlVersion == 1) "mandatory=true" else "type=\"required\"",
        "display_test" to if (legacyMetadata) "displayTest=\"IGNORE_ALL_VERSION\"" else "")
    inputs.properties(values)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(values)
        if (legacyMetadata) path = "META-INF/mods.toml"
    }
    exclude("fabric.mod.json", "META-INF/mods.toml")
}
