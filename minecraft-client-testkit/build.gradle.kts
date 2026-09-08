plugins { `java-library` }

group = "dev.itemnamecopy.test"
version = "0.0.0"

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}
