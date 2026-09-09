plugins {
    id("net.neoforged.gradle.userdev") version "7.1.38"
}

version = property("mod.version") as String
group = "com.github.yuu1111"
base.archivesName = "item-name-copy"

repositories { mavenCentral() }

java {
    toolchain.languageVersion = JavaLanguageVersion.of(property("java_version").toString().toInt())
    withSourcesJar()
}

dependencies {
    implementation("net.neoforged:neoforge:${property("loader_version")}")
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    java.exclude("**/FabricKeyboardMixin.java", "**/ForgeClient.java", "**/LegacyForgeEvents.java")
}

runs {
    configureEach { modSource(sourceSets.main.get()) }
}

tasks.jar {
    archiveFileName = "item-name-copy-${project.version}+neoforge-mc${project.property("minecraft_version")}.jar"
}
tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

apply(from = rootProject.file("gradle/neoforge-resources.gradle.kts"))
apply(from = rootProject.file("gradle/verify-artifact.gradle.kts"))
