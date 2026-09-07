import net.minecraftforge.renamer.gradle.RenamerExtension

plugins {
    java
    id("net.minecraftforge.gradle") version "7.0.36"
    id("net.minecraftforge.renamer") version "1.0.14" apply false
}

version = property("mod.version") as String
group = "dev.itemnamecopy"
base.archivesName = "item-name-copy"
val obfuscatedRuntime = org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) <
    org.gradle.util.GradleVersion.version("1.20.5")
if (obfuscatedRuntime) apply(plugin = "net.minecraftforge.renamer")

minecraft {
    if (project.property("minecraft_version").toString().substringBefore('.').toInt() < 26) {
        mappings("official", project.property("minecraft_version").toString())
    }
    runs {
        configureEach {
            workingDir.convention(layout.projectDirectory.dir("run"))
            args("--mixin.config=itemnamecopy.mixins.json")
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

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${property("minecraft_version")}-${property("loader_version")}"))
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/FabricKeyboardMixin.java", "**/NeoForgeClient.java")
}

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+forge-mc${project.property("minecraft_version")}.jar"
    if (obfuscatedRuntime) destinationDirectory = layout.buildDirectory.dir("devlibs")
    manifest.attributes("MixinConfigs" to "itemnamecopy.mixins.json")
}

if (obfuscatedRuntime) {
    val renamer = extensions.getByType<RenamerExtension>()
    renamer.enableMixinRefmaps {
        config("itemnamecopy.mixins.json")
        refMap.set("itemnamecopy.refmap.json")
        source(sourceSets.main.get()).refMap.set("itemnamecopy.refmap.json")
        jar(tasks.jar)
    }
    renamer.mappings(minecraft.dependency.toSrg)
    val reobfuscatedJar = renamer.classes(tasks.jar) {
        mappings(renamer.mixin.generatedMappings)
        output.set(layout.buildDirectory.file(
            "libs/item-name-copy-${project.version}+forge-mc${project.property("minecraft_version")}.jar"))
    }
    tasks.assemble { dependsOn(reobfuscatedJar) }
}

tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.processResources {
    val values = mapOf("version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"), "loader" to project.property("loader_version"),
        "fml" to project.property("loader_version").toString().substringBefore('.'))
    inputs.properties(values)
    filesMatching("META-INF/mods.toml") { expand(values) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
