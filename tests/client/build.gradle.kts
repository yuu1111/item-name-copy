plugins { java }

repositories { mavenCentral() }

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}

val transformer = sourceSets.create("transformer")
val runtime = sourceSets.create("runtime")
val runtimeBundle = configurations.create("runtimeBundle")
dependencies {
    add(transformer.implementationConfigurationName, "org.ow2.asm:asm:9.9.1")
    add(runtime.implementationConfigurationName, "com.github.yuu1111:minecraft-client-testkit")
    add(runtimeBundle.name, "com.github.yuu1111:minecraft-client-testkit")
}

tasks.jar {
    archiveFileName = "client-test-bootstrap.jar"
    manifest.attributes("Premain-Class" to "com.github.yuu1111.itemnamecopy.test.instrumentation.ClientTestBootstrap")
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
    from(runtime.output)
    from(runtimeBundle.map { files -> files.map(::zipTree) })
    exclude("META-INF/MANIFEST.MF")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.assemble { dependsOn(transformerJar, runtimeJar) }
