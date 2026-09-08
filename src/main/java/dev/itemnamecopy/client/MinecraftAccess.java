package dev.itemnamecopy.client;

import com.mojang.blaze3d.platform.ClipboardManager;
//? if !forge_without_mixins {
import dev.itemnamecopy.mixin.ContainerScreenAccessor;
import dev.itemnamecopy.mixin.RecipeBookAccessor;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
//? if <1.21.2 {
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
//? if <1.19 {
/*import net.minecraft.network.chat.TranslatableComponent;
 *///?}
//? if fabric && <1.19 {
/*import net.minecraft.network.chat.TextComponent;
 *///?}

public final class MinecraftAccess {
    private MinecraftAccess() {
    }

    public static Screen currentScreen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen();
         *///?} else {
        return Minecraft.getInstance().screen;
        //?}
    }

    public static long windowHandle() {
        //? if mcp_names && <1.15 {
        /*return Minecraft.getInstance().mainWindow.getHandle();
         *///?} elif mcp_names {
        /*return Minecraft.getInstance().getMainWindow().getHandle();
         *///?} elif <1.15 {
        /*return Minecraft.getInstance().window.getWindow();
         *///?} elif >=1.21.9 {
        /*return Minecraft.getInstance().getWindow().handle();
         *///?} else {
        return Minecraft.getInstance().getWindow().getWindow();
        //?}
    }

    public static RecipeBookComponent recipeBook(Screen screen) {
        //? if >=1.21.2 {
        /*if (screen instanceof RecipeScreenAccessor) {
            return ((RecipeScreenAccessor) screen).itemnamecopy$getRecipeBook();
        }
        return null;
        *///?} else {
        if (screen instanceof RecipeUpdateListener) {
            return ((RecipeUpdateListener) screen).getRecipeBookComponent();
        }
        return null;
        //?}
    }

    public static Slot hoveredSlot(Screen screen) {
        //? if forge_without_mixins {
        /*return ((AbstractContainerScreen<?>) screen).hoveredSlot;
         *///?} else {
        return ((ContainerScreenAccessor) screen).itemnamecopy$getHoveredSlot();
        //?}
    }

    public static boolean isRecipeSearchFocused(RecipeBookComponent recipeBook) {
        if (!recipeBook.isVisible()) return false;

        //? if forge_without_mixins {
        /*EditBox search = recipeBook.searchBox;
         *///?} else {
        EditBox search = ((RecipeBookAccessor) recipeBook).itemnamecopy$getSearchBox();
        //?}
        return search != null && search.isFocused();
    }

    public static void writeClipboard(ClipboardManager clipboard, String name) {
        //? if >=1.21.9 {
        /*clipboard.setClipboard(Minecraft.getInstance().getWindow(), name);
         *///?} else {
        clipboard.setClipboard(windowHandle(), name);
        //?}
    }

    public static void feedback(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        //? if >=1.19 {
        Component message = Component.translatable("itemnamecopy.copied", name);
        //?} else {
        /*Component message = new TranslatableComponent("itemnamecopy.copied", name);
         *///?}
        //? if fabric {
        if ("itemnamecopy.copied".equals(message.getString())) {
            String fallback = BundledFeedback.copied(minecraft.options.languageCode, name);
            //? if >=1.19 {
            message = Component.literal(fallback);
            //?} else {
            /*message = new TextComponent(fallback);
             *///?}
        }
        //?}
        //? if >=26.1 {
        /*minecraft.player.sendOverlayMessage(message);
         *///?} else {
        minecraft.player.displayClientMessage(message, true);
        //?}
    }
}
