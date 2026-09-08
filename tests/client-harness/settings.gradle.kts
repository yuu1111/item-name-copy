pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
rootProject.name = "ItemNameCopyClientHarness"
includeBuild("../../minecraft-client-testkit")
