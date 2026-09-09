package com.github.yuu1111.itemnamecopy.client;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.github.yuu1111.itemnamecopy.core.CopyShortcutHandler;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
//? if >=1.17 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

final class ContainerCopyTarget implements CopyShortcutHandler.Target {
    //? if >=1.17 {
    private static final Logger LOGGER = LoggerFactory.getLogger("itemnamecopy");
    //?} else {
    /*private static final Logger LOGGER = LogManager.getLogger("itemnamecopy");
    *///?}
    private static final ClipboardManager CLIPBOARD = new ClipboardManager();

    private final Minecraft minecraft;
    private final Screen containerScreen;

    ContainerCopyTarget(Screen screen) {
        this.minecraft = Minecraft.getInstance();
        this.containerScreen = screen;
    }

    @Override
    public boolean isTextInputFocused() {
        RecipeBookComponent recipeBook = MinecraftAccess.recipeBook(containerScreen);
        if (recipeBook != null && MinecraftAccess.isRecipeSearchFocused(recipeBook)) return true;
        return hasFocusedTextInput(containerScreen) || RecipeViewerAccess.isSearchFocused();
    }

    @Override
    public Optional<String> hoveredItemName() {
        if (containerScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
            Slot slot = MinecraftAccess.hoveredSlot(containerScreen);
            if (slot != null && slot.hasItem()) {
                return Optional.of(slot.getItem().getHoverName().getString());
            }
        }
        return RecipeViewerAccess.hoveredItemName(containerScreen);
    }

    @Override
    public boolean writeClipboard(String name) {
        try {
            MinecraftAccess.writeClipboard(CLIPBOARD, name);
            boolean success = name.equals(minecraft.keyboardHandler.getClipboard());
            if (!success) LOGGER.warn("Clipboard did not retain the item name");
            return success;
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not copy the item name", exception);
            return false;
        }
    }

    @Override
    public void showFeedback(String name) {
        MinecraftAccess.feedback(name);
    }

    private static boolean hasFocusedTextInput(GuiEventListener listener) {
        if (listener instanceof EditBox && ((EditBox) listener).isFocused()) return true;
        if (!(listener instanceof ContainerEventHandler)) return false;

        ContainerEventHandler container = (ContainerEventHandler) listener;
        for (GuiEventListener child : container.children()) {
            if (hasFocusedTextInput(child)) return true;
        }
        return false;
    }
}
