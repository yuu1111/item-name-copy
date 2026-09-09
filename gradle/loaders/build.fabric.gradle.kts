plugins {
    id("dev.kikugie.loom-back-compat")
}

version = property("mod.version") as String
group = "com.github.yuu1111"
base.archivesName = "item-name-copy"

repositories {
    mavenCentral()
    maven("https://api.modrinth.com/maven")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.blamejared.com/") {
        content { includeGroup("mezz.jei") }
    }
    maven("https://repo.sleeping.town/") {
        content { includeGroup("dev.emi") }
    }
    maven("https://maven.shedaniel.me/") {
        content { includeGroup("me.shedaniel.cloth") }
    }
}

val legacyMappings = org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) <
        org.gradle.util.GradleVersion.version("1.14.4")
if (legacyMappings) apply(from = rootProject.file("gradle/legacy-fabric-mappings.gradle.kts"))

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    if (legacyMappings) mappings(project.extra["legacyFabricMappings"].toString())
    else loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    if (property("minecraft_version") == "1.21.1") {
        val recipeViewer = providers.gradleProperty("recipeViewerTest").orNull
        if (recipeViewer != null) {
            modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1")
        }
        when (recipeViewer) {
            "emi" -> {
                modImplementation("dev.emi:emi-fabric:1.1.24+1.21.1")
            }
            "rei" -> {
                modImplementation("maven.modrinth:rei:16.0.799+fabric")
                modImplementation("maven.modrinth:architectury-api:13.0.11+fabric")
                modImplementation("maven.modrinth:cloth-config:15.0.140+fabric")
                modImplementation("me.shedaniel.cloth:basic-math:0.6.1")
            }
            "jei" -> {
                modImplementation("mezz.jei:jei-1.21.1-fabric:19.53.0.426")
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
    java.exclude("**/NeoForgeClient.java", "**/ForgeClient.java", "**/LegacyForgeEvents.java")
    if (org.gradle.util.GradleVersion.version(property("minecraft_version").toString()) >=
        org.gradle.util.GradleVersion.version("1.21.9")) java.exclude("**/KeyMappingAccessor.java")
}

loomx.modJar.configure {
    archiveFileName = "item-name-copy-${project.version}+fabric-mc${project.property("minecraft_version")}.jar"
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE"))
}

tasks.processResources {
    val values = mapOf(
        "version" to project.version, "minecraft" to project.property("minecraft_version"),
        "java" to project.property("java_version"), "loader" to project.property("loader_version")
    )
    inputs.properties(values)
    filesMatching("fabric.mod.json") { expand(values) }
    exclude("META-INF/**")
}

apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
