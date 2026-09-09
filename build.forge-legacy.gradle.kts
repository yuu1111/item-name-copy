plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

version = property("mod.version") as String
group = "com.github.yuu1111"
base.archivesName = "item-name-copy"

val targetLoader = property("target_loader").toString()

repositories { mavenCentral() }

legacyForge {
    enable {
        if (targetLoader == "neoforge") {
            neoForgeVersion = "${project.property("minecraft_version")}-${project.property("loader_version")}"
        } else {
            forgeVersion = "${project.property("minecraft_version")}-${project.property("loader_version")}"
        }
        setDisableRecompilation(true)
    }
    mods {
        register("itemnamecopy") { sourceSet(sourceSets.main.get()) }
    }
    runs {
        register("client") {
            client()
            if (org.gradle.util.GradleVersion.version(project.property("minecraft_version").toString()) <
                org.gradle.util.GradleVersion.version("1.18.2")) {
                jvmArgument("--add-opens=java.base/java.lang.invoke=cpw.mods.securejarhandler")
            }
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/FabricKeyboardMixin.java", "**/NeoForgeClient.java", "**/LegacyForgeEvents.java")
}

dependencies { annotationProcessor("org.spongepowered:mixin:0.8.5:processor") }

mixin {
    add(sourceSets.main.get(), "itemnamecopy.refmap.json")
    config("itemnamecopy.mixins.json")
}

tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+$targetLoader-mc${project.property("minecraft_version")}.jar"
    manifest.attributes("MixinConfigs" to "itemnamecopy.mixins.json")
}

tasks.named<Jar>("reobfJar") {
    archiveFileName = "item-name-copy-${project.version}+$targetLoader-mc${project.property("minecraft_version")}.jar"
}

tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) }

tasks.processResources {
    val values = mapOf(
        "version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"),
        "java_feature" to if (targetLoader == "forge" && project.property("loader_version").toString().substringBefore('.').toInt() < 47) "java_version" else "javaVersion",
        "loader" to project.property("loader_version"),
        "fml" to project.property("loader_version").toString().substringBefore('.')
    )
    inputs.properties(values)
    filesMatching("META-INF/mods.toml") { expand(values) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
