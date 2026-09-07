package dev.itemnamecopy.client;

import com.mojang.blaze3d.platform.ClipboardManager;
import dev.itemnamecopy.mixin.RecipeScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
//? if <1.21.2 {
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
//?}
import net.minecraft.network.chat.Component;

public final class MinecraftAccess {
    private MinecraftAccess() {}

    public static Screen currentScreen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen();
        *///?} else {
        return Minecraft.getInstance().screen;
        //?}
    }

    public static long windowHandle() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().getWindow().handle();
        *///?} else {
        return Minecraft.getInstance().getWindow().getWindow();
        //?}
    }

    public static RecipeBookComponent recipeBook(Screen screen) {
        //? if >=1.21.2 {
        /*return screen instanceof RecipeScreenAccessor
                ? ((RecipeScreenAccessor) screen).itemnamecopy$getRecipeBook() : null;
        *///?} else {
        return screen instanceof RecipeUpdateListener
                ? ((RecipeUpdateListener) screen).getRecipeBookComponent() : null;
        //?}
    }

    public static void writeClipboard(ClipboardManager clipboard, String name) {
        //? if >=26.2 {
        /*clipboard.setClipboard(Minecraft.getInstance().getWindow(), name);
        *///?} else {
        clipboard.setClipboard(windowHandle(), name);
        //?}
    }

    public static void feedback(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Component message = Component.translatable("itemnamecopy.copied", name);
        //? if >=26.2 {
        /*minecraft.player.sendOverlayMessage(message);
        *///?} else {
        minecraft.player.displayClientMessage(message, true);
        //?}
    }
}
