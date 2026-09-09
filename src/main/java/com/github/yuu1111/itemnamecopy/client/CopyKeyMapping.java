package com.github.yuu1111.itemnamecopy.client;

import net.minecraft.client.KeyMapping;
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
*///?}
import org.lwjgl.glfw.GLFW;

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
    private static final KeyMapping COPY = new KeyMapping(
            "key.itemnamecopy.copy",
            GLFW.GLFW_KEY_C,
            //? if >=1.21.9 {
            /*KEY_CATEGORY
            *///?} else {
            CATEGORY
            //?}
    );

    private CopyKeyMapping() {
    }

    public static KeyMapping mapping() {
        return COPY;
    }

    public static boolean matches(int key, int scanCode, int modifiers) {
        //? if >=1.21.9 {
        /*return COPY.matches(new KeyEvent(key, scanCode, modifiers));
        *///?} else {
        return COPY.matches(key, scanCode);
        //?}
    }
}
