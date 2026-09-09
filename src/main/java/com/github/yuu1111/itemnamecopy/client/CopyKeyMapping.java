package com.github.yuu1111.itemnamecopy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if forge {
/*import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
*///?} elif neoforge {
/*import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
*///?}
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
*///?}
import org.lwjgl.glfw.GLFW;

import java.io.File;

public final class CopyKeyMapping {
    public static final String CATEGORY = "key.categories.itemnamecopy";

    //? if >=1.21.9 {
    /*private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            //? if >=26.1 {
            Identifier.fromNamespaceAndPath("itemnamecopy", "main")
            //?} else {
            ResourceLocation.fromNamespaceAndPath("itemnamecopy", "main")
            //?}
    );
    *///?}
    private static final KeyMapping COPY =
            //? if fabric {
            new FabricChordKeyMapping(
            "key.itemnamecopy.copy",
            GLFW.GLFW_KEY_C,
            //? if >=1.21.9 {
            /*KEY_CATEGORY
            *///?} else {
            CATEGORY
            //?}
            );
            //?} else {
            /*new KeyMapping(
                    "key.itemnamecopy.copy",
                    KeyConflictContext.UNIVERSAL,
                    KeyModifier.CONTROL,
                    //? if forge && >=26.1 {
                    /^InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C),
                    KEY_CATEGORY,
                    0
                    ^///?} else {
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    //? if >=1.21.9 {
                    /^KEY_CATEGORY
                    ^///?} else {
                    CATEGORY
                    //?}
                    //?}
            );
            *///?}

    private CopyKeyMapping() {
    }

    public static KeyMapping mapping() {
        return COPY;
    }

    public static boolean matches(int key, int scanCode, int modifiers) {
        //? if fabric {
        return ((FabricChordKeyMapping) COPY).matchesShortcut(key, scanCode, modifiers);
        //?} else {
        /*//? if >=1.21.9 {
        return COPY.isActiveAndMatches(InputConstants.getKey(new KeyEvent(key, scanCode, modifiers)));
        //?} else {
        return COPY.isActiveAndMatches(InputConstants.getKey(key, scanCode));
        //?}
        *///?}
    }

    public static void beginOptionsLoad(File gameDirectory) {
        //? if fabric {
        ((FabricChordKeyMapping) COPY).beginOptionsLoad(gameDirectory);
        //?}
    }

    public static void finishOptionsLoad() {
        //? if fabric {
        ((FabricChordKeyMapping) COPY).finishOptionsLoad();
        //?}
    }

    public static void prepareRebind(int modifiers) {
        //? if fabric {
        ((FabricChordKeyMapping) COPY).prepareRebind(modifiers);
        //?}
    }
}
