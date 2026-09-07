import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.zip.ZipFile
import net.minecraftforge.renamer.gradle.RenamerExtension

plugins {
    java
    id("net.minecraftforge.gradle") version "7.0.36"
    id("net.minecraftforge.renamer") version "1.0.14"
}

version = property("mod.version") as String
group = "dev.itemnamecopy"
base.archivesName = "item-name-copy"

minecraft {
    mappings("stable", "39-1.12")
    runs {
        configureEach { workingDir.convention(layout.projectDirectory.dir("run")) }
        register("client")
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    implementation(minecraft.dependency(project.name,
        "net.minecraftforge:forge:${property("minecraft_version")}-${property("loader_version")}"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(8)
    withSourcesJar()
}

sourceSets.main {
    java.setSrcDirs(listOf(rootProject.file("src/legacy112/java"), rootProject.file("core/src/main/java")))
    resources.setSrcDirs(emptyList<String>())
}

val generatedResources = layout.buildDirectory.dir("generated/legacy112-resources")
val generateLegacyResources = tasks.register("generateLegacyResources") {
    inputs.property("version", project.version)
    inputs.property("minecraft", project.property("minecraft_version"))
    inputs.property("loader", project.property("loader_version"))
    inputs.dir(rootProject.file("src/main/resources/assets/itemnamecopy"))
    outputs.dir(generatedResources)
    doLast {
        val output = generatedResources.get().asFile
        output.mkdirs()
        output.resolve("version.properties").writeText("itemnamecopy.version=${project.version}\n")
        output.resolve("pack.mcmeta").writeText(JsonOutput.toJson(mapOf("pack" to mapOf(
            "pack_format" to 3, "description" to "ItemNameCopy translations"))))
        output.resolve("mcmod.info").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(listOf(mapOf(
            "modid" to "itemnamecopy", "name" to "ItemNameCopy", "version" to project.version.toString(),
            "mcversion" to project.property("minecraft_version"), "description" to "Copy hovered item names with Ctrl+C",
            "logoFile" to "assets/itemnamecopy/icon.png", "clientSideOnly" to true,
            "useDependencyInformation" to true,
            "dependencies" to listOf("required-after:forge@[${project.property("loader_version")},)"),
            "authorList" to listOf("yuu1111"))))))
        val assets = output.resolve("assets/itemnamecopy")
        assets.resolve("lang").mkdirs()
        rootProject.file("src/main/resources/assets/itemnamecopy/icon.png").copyTo(assets.resolve("icon.png"), true)
        for (language in listOf("en_us", "ja_jp")) {
            val translations = JsonSlurper().parse(rootProject.file(
                "src/main/resources/assets/itemnamecopy/lang/$language.json")) as Map<*, *>
            assets.resolve("lang/$language.lang").writeText(translations.entries.joinToString("\n", postfix = "\n") {
                "${it.key}=${it.value}"
            })
        }
    }
}
sourceSets.main { resources.srcDir(generateLegacyResources) }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
val artifactName = "item-name-copy-${project.version}+forge-mc${property("minecraft_version")}.jar"
tasks.jar {
    archiveFileName = artifactName
    destinationDirectory = layout.buildDirectory.dir("devlibs")
}
val renamer = extensions.getByType<RenamerExtension>()
renamer.mappings(minecraft.getDependency(project.name).toSrg)
val productionJar = renamer.classes(tasks.jar) {
    output.set(layout.buildDirectory.file("libs/$artifactName"))
}
tasks.assemble { dependsOn(productionJar) }

val verifyArtifact = tasks.register("verifyArtifact") {
    group = "verification"
    dependsOn(productionJar)
    inputs.file(layout.buildDirectory.file("libs/$artifactName"))
    doLast {
        ZipFile(layout.buildDirectory.file("libs/$artifactName").get().asFile).use { zip ->
            fun read(name: String): String = zip.getInputStream(checkNotNull(zip.getEntry(name))).bufferedReader().use { it.readText() }
            val metadata = (JsonSlurper().parseText(read("mcmod.info")) as List<*>).single() as Map<*, *>
            check(metadata["modid"] == "itemnamecopy" && metadata["clientSideOnly"] == true)
            check(metadata["version"] == project.version && metadata["mcversion"] == project.property("minecraft_version"))
            check(read("version.properties").trim() == "itemnamecopy.version=${project.version}")
            check(metadata["dependencies"] == listOf("required-after:forge@[${project.property("loader_version")},)"))
            check(read("LICENSE").startsWith("MIT License"))
            check(zip.getEntry("dev/itemnamecopy/legacy/Forge112Client.class") != null)
            check(zip.getEntry("dev/itemnamecopy/core/CopyShortcutHandler.class") != null)
            check(zip.getEntry("itemnamecopy.mixins.json") == null && zip.getEntry("META-INF/mods.toml") == null)
            val icon = zip.getInputStream(checkNotNull(zip.getEntry("assets/itemnamecopy/icon.png"))).use { javax.imageio.ImageIO.read(it) }
            check(icon.width == 256 && icon.height == 256)
            for (language in listOf("en_us", "ja_jp")) {
                check(read("assets/itemnamecopy/lang/$language.lang").contains("itemnamecopy.copied="))
            }
            zip.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val header = input.readNBytes(8)
                    val major = (header[6].toInt() and 255) * 256 + (header[7].toInt() and 255)
                    check(major <= 52) { "${entry.name} requires newer Java than 8" }
                }
            }
        }
    }
}
tasks.named("check") { dependsOn(verifyArtifact) }
