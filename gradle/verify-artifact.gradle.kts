import groovy.json.JsonSlurper
import java.util.zip.ZipFile

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val minecraftTarget = project.property("minecraft_version").toString()
val loaderTarget = project.name.substringAfter('-')
val requiredJava = project.property("java_version").toString().toInt()
val artifactName = "item-name-copy-${project.version}+$loaderTarget-mc$minecraftTarget.jar"
val artifact = layout.buildDirectory.file("libs/$artifactName")
val legacyForge = plugins.hasPlugin("net.neoforged.moddev.legacyforge")
val renamedForge = plugins.hasPlugin("net.minecraftforge.renamer")
val requiresRefmap = legacyForge || renamedForge
val hasRecipeScreen = org.gradle.util.GradleVersion.version(minecraftTarget) >= org.gradle.util.GradleVersion.version("1.21.2")
tasks.named<ProcessResources>("processResources") {
    val mixinValues = mapOf("java" to requiredJava.toString(),
        "keyboard_mixin" to if (loaderTarget == "fabric") ", \"FabricKeyboardMixin\"" else "",
        "recipe_screen_mixin" to if (hasRecipeScreen) ", \"RecipeScreenAccessor\"" else "",
        "refmap" to if (requiresRefmap) "\"refmap\": \"itemnamecopy.refmap.json\"," else "")
    inputs.properties(mixinValues)
    filesMatching("itemnamecopy.mixins.json") { expand(mixinValues) }
}
val artifactTask = when {
    loaderTarget == "fabric" && minecraftTarget.substringBefore('.').toInt() < 26 -> "remapJar"
    legacyForge -> "reobfJar"
    renamedForge -> "renameJar"
    else -> "jar"
}

val verifyArtifact = tasks.register("verifyArtifact") {
    group = "verification"
    dependsOn(artifactTask)
    inputs.file(artifact)
    doLast {
        ZipFile(artifact.get().asFile).use { zip ->
            fun read(path: String): String {
                val entry = checkNotNull(zip.getEntry(path)) { "$artifactName is missing $path" }
                return zip.getInputStream(entry).bufferedReader().use { it.readText() }
            }
            check(read("LICENSE").startsWith("MIT License")) { "Missing MIT license" }
            val iconPath = "assets/itemnamecopy/icon.png"
            val iconEntry = checkNotNull(zip.getEntry(iconPath)) { "Missing mod icon" }
            zip.getInputStream(iconEntry).use { input ->
                val icon = checkNotNull(javax.imageio.ImageIO.read(input)) { "Unreadable mod icon" }
                check(icon.width == 256 && icon.height == 256) { "Mod icon must be 256 x 256" }
            }
            val mixins = JsonSlurper().parseText(read("itemnamecopy.mixins.json")) as Map<*, *>
            check(mixins["required"] == true)
            check(mixins["mixins"] == null) { "Client mixins must not load on a dedicated server" }
            val clientMixins = mixins["client"] as List<*>
            check(clientMixins.containsAll(listOf("ContainerScreenAccessor", "RecipeBookAccessor", "KeyboardInputMixin")))
            check(clientMixins.contains("FabricKeyboardMixin") == (loaderTarget == "fabric"))
            check(clientMixins.contains("RecipeScreenAccessor") == hasRecipeScreen)
            clientMixins.forEach {
                check(zip.getEntry("dev/itemnamecopy/mixin/$it.class") != null) { "Missing mixin class $it" }
            }
            if (requiresRefmap) {
                check(mixins["refmap"] == "itemnamecopy.refmap.json")
                check((JsonSlurper().parseText(read("itemnamecopy.refmap.json")) as Map<*, *>).isNotEmpty())
                check(read("META-INF/MANIFEST.MF").contains("MixinConfigs: itemnamecopy.mixins.json"))
            }
            if (loaderTarget == "fabric") {
                val metadata = JsonSlurper().parseText(read("fabric.mod.json")) as Map<*, *>
                check(metadata["id"] == "itemnamecopy" && metadata["environment"] == "client")
                check(metadata["version"] == project.version.toString() && metadata["license"] == "MIT")
                check(metadata["icon"] == iconPath)
                val depends = metadata["depends"] as Map<*, *>
                check(depends["minecraft"] == "=$minecraftTarget")
                check(depends["java"] == ">=$requiredJava")
                check(depends["fabricloader"] == ">=${project.property("loader_version")}")
                check(zip.getEntry("META-INF/mods.toml") == null && zip.getEntry("META-INF/neoforge.mods.toml") == null)
            } else {
                val oldNeoMetadata = loaderTarget == "neoforge" &&
                    org.gradle.util.GradleVersion.version(minecraftTarget) < org.gradle.util.GradleVersion.version("1.20.5")
                val metadataPath = if (legacyForge || loaderTarget == "forge" || oldNeoMetadata)
                    "META-INF/mods.toml" else "META-INF/neoforge.mods.toml"
                val metadata = read(metadataPath)
                check(!metadata.contains("\${")) { "Unexpanded metadata" }
                check(metadata.contains("modId=\"itemnamecopy\""))
                check(metadata.contains("logoFile=\"$iconPath\""))
                check(metadata.contains("version=\"${project.version}\""))
                check(metadata.contains("versionRange=\"[$minecraftTarget]\""))
                check(metadata.contains("versionRange=\"[${project.property("loader_version")},)\""))
                check(metadata.contains("javaVersion=\"[$requiredJava,)\""))
                check(zip.getEntry("fabric.mod.json") == null)
                val otherMetadata = if (metadataPath == "META-INF/mods.toml") "META-INF/neoforge.mods.toml" else "META-INF/mods.toml"
                check(zip.getEntry(otherMetadata) == null) { "Jar contains metadata for another loader generation" }
                if (legacyForge || loaderTarget == "forge") {
                    check(metadata.contains("displayTest=\"IGNORE_ALL_VERSION\""))
                    check(read("META-INF/MANIFEST.MF").contains("MixinConfigs: itemnamecopy.mixins.json"))
                }
            }
            for (language in listOf("en_us", "ja_jp")) {
                val translations = JsonSlurper().parseText(read("assets/itemnamecopy/lang/$language.json")) as Map<*, *>
                check(translations["itemnamecopy.copied"].toString().contains("%s"))
            }
            zip.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val header = input.readNBytes(8)
                    val major = (header[6].toInt() and 255) * 256 + (header[7].toInt() and 255)
                    check(major <= requiredJava + 44) { "${entry.name} requires a newer Java than $requiredJava" }
                }
            }
        }
        logger.lifecycle("Verified $artifactName: metadata, client mixins, translations, license and Java $requiredJava")
    }
}

tasks.named("check") { dependsOn(verifyArtifact) }

apply(from = rootProject.file("gradle/verify-minecraft-hooks.gradle.kts"))
