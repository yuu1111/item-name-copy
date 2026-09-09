import net.minecraftforge.renamer.gradle.RenamerExtension
import java.io.File

plugins {
    java
    id("net.minecraftforge.gradle") version "7.0.36"
    id("net.minecraftforge.renamer") version "1.0.14" apply false
}

version = property("mod.version") as String
group = "com.github.yuu1111"
base.archivesName = "item-name-copy"
val obfuscatedRuntime = org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) <
        org.gradle.util.GradleVersion.version("1.20.5")
val mixinRuntime = org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) >=
        org.gradle.util.GradleVersion.version("1.15.2")
if (obfuscatedRuntime) apply(plugin = "net.minecraftforge.renamer")

minecraft {
    if (!mixinRuntime) {
        val accessFile = rootProject.file(".gradle/legacy-forge-access/${project.name}.cfg")
        val searchField = if (project.findProperty("mappings_channel") == "snapshot") "searchBar" else "searchBox"
        val accessRules = rootProject.file("src/legacy-forge/resources/META-INF/accesstransformer.cfg").readText()
            .replace("field_147006_u", "hoveredSlot").replace("field_193962_q", searchField)
        accessFile.parentFile.mkdirs()
        if (!accessFile.isFile || accessFile.readText() != accessRules) accessFile.writeText(accessRules)
        accessTransformers.from(accessFile)
    }
    if (project.property("minecraft_version").toString().substringBefore('.').toInt() < 26) {
        mappings(
            project.findProperty("mappings_channel")?.toString() ?: "official",
            project.findProperty("mappings_version")?.toString() ?: project.property("minecraft_version").toString()
        )
    }
    runs {
        configureEach {
            workingDir.convention(layout.projectDirectory.dir("run"))
            if (mixinRuntime) args("--mixin.config=itemnamecopy.mixins.json")
        }
        register("client")
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (project.property("minecraft_version").toString() in listOf("1.16.3", "1.16.4") &&
            requested.group == "cpw.mods" && requested.name == "modlauncher") {
            useVersion("8.1.3")
            because("Supports both ManifestEntryVerifier constructors used by Java 8 updates")
        }
        if (project.property("minecraft_version").toString() == "1.21" &&
            requested.group == "net.sf.jopt-simple" && requested.name == "jopt-simple") {
            useVersion("5.0.4")
            because("Forge 51.0.33 requires the jopt.simple module name")
        }
    }
}

dependencies {
    implementation(
        minecraft.dependency(
            project.name,
            "net.minecraftforge:forge:${property("minecraft_version")}-${property("loader_version")}"
        )
    )
    if (mixinRuntime) annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
    if (org.gradle.util.GradleVersion.version(project.property("minecraft_version").toString()) <
        org.gradle.util.GradleVersion.version("1.17")
    ) {
        compileOnly("org.lwjgl:lwjgl-glfw:3.2.2")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/FabricKeyboardMixin.java", "**/NeoForgeClient.java")
    if (mixinRuntime) java.exclude("**/LegacyForgeEvents.java")
    else {
        java.exclude("**/mixin/**")
        resources.srcDir(rootProject.file("src/legacy-forge/resources"))
    }
}

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+forge-mc${project.property("minecraft_version")}.jar"
    if (obfuscatedRuntime) destinationDirectory = layout.buildDirectory.dir("devlibs")
    if (mixinRuntime) manifest.attributes("MixinConfigs" to "itemnamecopy.mixins.json")
}

if (obfuscatedRuntime) {
    tasks.withType<JavaExec>().matching { it.name == "runClient" }.configureEach {
        doFirst {
            environment("MOD_CLASSES", sourceSets.main.get().output.files.joinToString(File.pathSeparator) {
                "itemnamecopy%%${it.absolutePath}"
            })
        }
    }
    val renamer = extensions.getByType<RenamerExtension>()
    if (mixinRuntime) {
        renamer.enableMixinRefmaps {
            config("itemnamecopy.mixins.json")
            refMap.set("itemnamecopy.refmap.json")
            source(sourceSets.main.get()).refMap.set("itemnamecopy.refmap.json")
            jar(tasks.jar)
        }
    }
    renamer.mappings(minecraft.getDependency(project.name).toSrg)
    val reobfuscatedJar = renamer.classes(tasks.jar) {
        if (mixinRuntime) mappings(renamer.mixin.generatedMappings)
        output.set(
            layout.buildDirectory.file(
                "libs/item-name-copy-${project.version}+forge-mc${project.property("minecraft_version")}.jar"
            )
        )
    }
    tasks.assemble { dependsOn(reobfuscatedJar) }
}

tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.processResources {
    val values = mapOf(
        "version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"),
        "java_feature" to if (project.property("loader_version").toString().substringBefore('.').toInt() < 47) "java_version" else "javaVersion",
        "loader" to project.property("loader_version"),
        "fml" to project.property("loader_version").toString().substringBefore('.')
    )
    inputs.properties(values)
    filesMatching("META-INF/mods.toml") { expand(values) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
    if (!mixinRuntime) exclude("itemnamecopy.mixins.json")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
