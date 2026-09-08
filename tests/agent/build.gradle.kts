plugins { java }

repositories { mavenCentral() }

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}

val transformer = sourceSets.create("transformer")
val testkit = sourceSets.create("testkit")
val runtime = sourceSets.create("runtime")
runtime.compileClasspath += testkit.output
runtime.runtimeClasspath += testkit.output
dependencies { add(transformer.implementationConfigurationName, "org.ow2.asm:asm:9.9.1") }

tasks.jar {
    archiveFileName = "client-test-agent.jar"
    manifest.attributes("Premain-Class" to "dev.itemnamecopy.test.agent.Agent")
}
val transformerJar = tasks.register<Jar>("transformerJar") {
    archiveFileName = "client-test-transformer.jar"
    from(transformer.output)
    from(configurations[transformer.runtimeClasspathConfigurationName].map { zipTree(it) })
    exclude("META-INF/MANIFEST.MF", "module-info.class", "META-INF/versions/**")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
val runtimeJar = tasks.register<Jar>("runtimeJar") {
    archiveFileName = "client-test-runtime.jar"
    from(testkit.output)
    from(runtime.output)
}
tasks.assemble { dependsOn(transformerJar, runtimeJar) }
