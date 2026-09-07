import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.tree.MemoryMappingTree

buildscript {
    repositories { maven("https://maven.fabricmc.net/") }
    dependencies { classpath("net.fabricmc:mapping-io:0.8.0") }
}

val cacheKey = "itemnamecopyLegacyFabricMappings"
val rootExtras = rootProject.extensions.extraProperties
if (!rootExtras.has(cacheKey)) {
    val directory = rootProject.file(".gradle/legacy-fabric-mappings").apply { mkdirs() }

    fun checksum(file: File): String = MessageDigest.getInstance("SHA-1")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    fun download(name: String, address: String, sha1: String): File {
        val file = directory.resolve(name)
        if (file.isFile && checksum(file) == sha1) return file
        val temporary = directory.resolve("$name.part")
        val connection = URI.create(address).toURL().openConnection().apply {
            connectTimeout = 30000
            readTimeout = 60000
        }
        connection.getInputStream().use { input -> temporary.outputStream().use(input::copyTo) }
        check(checksum(temporary) == sha1) { "Mapping checksum mismatch: $name" }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return file
    }

    val intermediaryFile = download("intermediary-1.14.4.jar",
        "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14.4/intermediary-1.14.4-v2.jar",
        "ba7ae696906d5b076ec03bbc75c490bf164d16dc")
    val clientFile = download("client-1.14.4.txt",
        "https://piston-data.mojang.com/v1/objects/6073e4ba6949217eb708c4512be2ccc1850a603f/client.txt",
        "6073e4ba6949217eb708c4512be2ccc1850a603f")

    val intermediary = MemoryMappingTree(true)
    ZipFile(intermediaryFile).use { zip ->
        zip.getInputStream(checkNotNull(zip.getEntry("mappings/mappings.tiny"))).bufferedReader().use {
            MappingReader.read(it, MappingFormat.TINY_2_FILE, intermediary)
        }
    }
    val named = MemoryMappingTree(true)
    clientFile.bufferedReader().use { ProGuardFileReader.read(it, "named", "official", named) }

    // Mojang mappingsがない版では、安定したIntermediary名を介してクライアント側の名前を引き継ぐ
    val bridge = MemoryMappingTree()
    bridge.visitNamespaces("intermediary", listOf("named"))
    for (type in intermediary.classes) {
        val namedType = named.getClass(type.srcName, 0) ?: continue
        val stableName = type.getDstName(0) ?: continue
        bridge.visitClass(stableName)
        bridge.visitDstName(MappedElementKind.CLASS, 0, namedType.srcName)
        bridge.visitElementContent(MappedElementKind.CLASS)
        for (field in type.fields) {
            val namedField = namedType.getField(field.srcName, field.srcDesc, 0) ?: continue
            val stableField = field.getDstName(0) ?: continue
            bridge.visitField(stableField, field.getDstDesc(0))
            bridge.visitDstName(MappedElementKind.FIELD, 0, namedField.srcName)
            bridge.visitElementContent(MappedElementKind.FIELD)
        }
        for (method in type.methods) {
            val namedMethod = namedType.getMethod(method.srcName, method.srcDesc, 0) ?: continue
            if (namedMethod.srcName.startsWith("lambda$") || namedMethod.srcName.startsWith("access$")) continue
            val stableMethod = method.getDstName(0) ?: continue
            bridge.visitMethod(stableMethod, method.getDstDesc(0))
            bridge.visitDstName(MappedElementKind.METHOD, 0, namedMethod.srcName)
            bridge.visitElementContent(MappedElementKind.METHOD)
        }
    }
    bridge.visitEnd()
    check(bridge.classes.any { it.getDstName(0) == "net/minecraft/client/KeyboardHandler" }) {
        "Legacy mapping bridge does not contain the Minecraft client"
    }
    val tinyFile = directory.resolve("mappings.tiny")
    MappingWriter.create(tinyFile.toPath(), MappingFormat.TINY_2_FILE).use { bridge.accept(it) }
    val mappingVersion = "1.14.4-${checksum(tinyFile)}"
    val artifactDirectory = directory.resolve("repo/dev/itemnamecopy/mappings/legacy-client/$mappingVersion")
        .apply { mkdirs() }
    val artifact = artifactDirectory.resolve("legacy-client-$mappingVersion-v2.jar")
    ZipOutputStream(artifact.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("mappings/mappings.tiny").apply { time = 0L })
        tinyFile.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
    rootExtras[cacheKey] = "dev.itemnamecopy.mappings:legacy-client:$mappingVersion:v2"
}
repositories.maven {
    url = uri(rootProject.file(".gradle/legacy-fabric-mappings/repo"))
    metadataSources { artifact() }
    content { includeModule("dev.itemnamecopy.mappings", "legacy-client") }
}
extensions.extraProperties["legacyFabricMappings"] = rootExtras[cacheKey]
