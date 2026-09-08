import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.ow2.asm:asm-tree:9.9") }
}

val minecraftVersion = org.gradle.util.GradleVersion.version(property("minecraft_version").toString())
val keyEventApi = minecraftVersion >= org.gradle.util.GradleVersion.version("1.21.9")
val recipeScreenApi = minecraftVersion >= org.gradle.util.GradleVersion.version("1.21.2")
val fabricTarget = project.name.endsWith("-fabric")
val legacyForgeNames = if (project.name.endsWith("-forge") &&
    minecraftVersion < org.gradle.util.GradleVersion.version("1.17")) {
    java.util.Properties().apply {
        rootProject.file("gradle/legacy-forge-names.properties").inputStream().use(::load)
    }.entries.associate { (named, legacy) -> named.toString().replace('.', '/') to legacy.toString().replace('.', '/') }
} else emptyMap()
val legacyMemberNames = if (project.findProperty("mappings_channel") == "snapshot") {
    java.util.Properties().apply {
        rootProject.file("gradle/mcp-member-names.properties").inputStream().use(::load)
    }.entries.associate { (named, legacy) -> named.toString() to legacy.toString() }
} else emptyMap()
val minecraftClasspath = extensions.getByType<JavaPluginExtension>().sourceSets.named("main").map { it.compileClasspath }

val verifyMinecraftHooks = tasks.register("verifyMinecraftHooks") {
    group = "verification"
    dependsOn("compileJava")
    inputs.files(minecraftClasspath).withNormalizer(ClasspathNormalizer::class)
    doLast {
        val keyboardName = legacyForgeNames["net/minecraft/client/KeyboardHandler"] ?: "net/minecraft/client/KeyboardHandler"
        val minecraftJar = checkNotNull(minecraftClasspath.get().files.firstOrNull { file ->
            file.isFile && file.extension == "jar" && ZipFile(file).use { it.getEntry("$keyboardName.class") != null }
        }) { "Cannot locate named Minecraft client classes for ${project.name}" }

        ZipFile(minecraftJar).use { zip ->
            fun load(name: String): ClassNode {
                val mappedName = legacyForgeNames[name] ?: name
                val entry = checkNotNull(zip.getEntry("$mappedName.class")) { "Missing Minecraft class: $mappedName" }
                return ClassNode().also { node ->
                    zip.getInputStream(entry).use { ClassReader(it).accept(node, ClassReader.SKIP_DEBUG) }
                }
            }
            fun field(owner: String, name: String, descriptor: String) {
                val mappedDescriptor = legacyForgeNames.entries.fold(descriptor) { result, (named, legacy) ->
                    result.replace("L$named;", "L$legacy;")
                }
                val mappedField = legacyMemberNames[name] ?: name
                check(load(owner).fields.any { it.name == mappedField && it.desc == mappedDescriptor }) {
                    "Mixin field is missing or has changed type: $owner.$name $descriptor"
                }
            }

            field("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen", "hoveredSlot",
                "Lnet/minecraft/world/inventory/Slot;")
            field("net/minecraft/client/gui/screens/recipebook/RecipeBookComponent", "searchBox",
                "Lnet/minecraft/client/gui/components/EditBox;")
            if (project.name.endsWith("-forge") && minecraftVersion < org.gradle.util.GradleVersion.version("1.15.2")) {
                for ((owner, name) in listOf(
                    "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen" to "hoveredSlot",
                    "net/minecraft/client/gui/screens/recipebook/RecipeBookComponent" to "searchBox")) {
                    val mappedField = legacyMemberNames[name] ?: name
                    check((load(owner).fields.single { it.name == mappedField }.access and Opcodes.ACC_PUBLIC) != 0) {
                        "Legacy Forge access transformer did not expose $owner.$name"
                    }
                }
            }
            if (recipeScreenApi) {
                field("net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen", "recipeBookComponent",
                    "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;")
            }

            val keyboard = load(keyboardName)
            val keyEvent = "Lnet/minecraft/client/input/KeyEvent;"
            val keyPressDescriptor = if (keyEventApi) "(JI$keyEvent)V" else "(JIIII)V"
            val keyPressMethod = legacyMemberNames["keyPress"] ?: "keyPress"
            check(keyboard.methods.any { it.name == keyPressMethod && it.desc == keyPressDescriptor }) {
                "KeyboardInputMixin handler no longer matches keyPress$keyPressDescriptor"
            }
            if (fabricTarget) {
                val directKeyPress = minecraftVersion >= org.gradle.util.GradleVersion.version("1.21.2")
                val methodName = if (directKeyPress) "keyPress" else "method_1454"
                val legacyContainerCallback = minecraftVersion < org.gradle.util.GradleVersion.version("1.17")
                val descriptor = if (directKeyPress) keyPressDescriptor
                    else if (legacyContainerCallback) "(I[ZLnet/minecraft/client/gui/components/events/ContainerEventHandler;III)V"
                    else "(ILnet/minecraft/client/gui/screens/Screen;[ZIII)V"
                val method = checkNotNull(keyboard.methods.find { it.name == methodName && it.desc == descriptor }) {
                    "FabricKeyboardMixin handler no longer matches $methodName$descriptor"
                }
                val staticCallback = !directKeyPress &&
                    minecraftVersion >= org.gradle.util.GradleVersion.version("1.19.3")
                check(((method.access and Opcodes.ACC_STATIC) != 0) == staticCallback) {
                    "Fabric keyboard callback staticness changed"
                }
                val screenDescriptor = if (keyEventApi) "($keyEvent)Z" else "(III)Z"
                val screenOwner = if (legacyContainerCallback) "net/minecraft/client/gui/components/events/ContainerEventHandler"
                    else "net/minecraft/client/gui/screens/Screen"
                val calls = method.instructions.asSequence().filterIsInstance<MethodInsnNode>().count {
                    it.owner == screenOwner &&
                        it.name == "keyPressed" && it.desc == screenDescriptor
                }
                check(calls == 1) { "Expected one Screen.keyPressed invocation but found $calls" }
            }
        }
        logger.lifecycle("Verified Minecraft mixin targets and callback descriptors for ${project.name}")
    }
}

tasks.named("check") { dependsOn(verifyMinecraftHooks) }

if (plugins.hasPlugin("net.neoforged.moddev.legacyforge") || plugins.hasPlugin("net.minecraftforge.renamer")) {
    tasks.named("verifyArtifact") {
        doLast {
            val loader = project.name.substringAfter('-')
            val jar = layout.buildDirectory.file(
                "libs/item-name-copy-${project.version}+$loader-mc${project.property("minecraft_version")}.jar").get().asFile
            ZipFile(jar).use { zip ->
                val entry = checkNotNull(zip.getEntry("com/github/yuu1111/itemnamecopy/client/MinecraftAccess.class"))
                val adapter = ClassNode()
                zip.getInputStream(entry).use { ClassReader(it).accept(adapter, ClassReader.SKIP_DEBUG) }
                val instanceCalls = adapter.methods.asSequence().flatMap { it.instructions.asSequence() }
                    .filterIsInstance<MethodInsnNode>().filter {
                        it.owner == "net/minecraft/client/Minecraft" && it.desc == "()Lnet/minecraft/client/Minecraft;"
                    }.toList()
                check(instanceCalls.isNotEmpty() && instanceCalls.all { it.name.matches(Regex("m_\\d+_|func_\\d+_[a-zA-Z]+")) }) {
                    "Production jar still contains named Minecraft.getInstance calls instead of SRG calls"
                }
            }
        }
    }
}
