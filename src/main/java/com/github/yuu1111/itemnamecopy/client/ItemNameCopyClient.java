package com.github.yuu1111.itemnamecopy.client;

import com.github.yuu1111.itemnamecopy.core.CopyShortcutHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public final class ItemNameCopyClient {

    private static final CopyShortcutHandler HANDLER = new CopyShortcutHandler();
    private static int currentAction;

    private ItemNameCopyClient() {
    }

    public static void beginKey(int key, int scanCode, int modifiers, int action) {
        currentAction = action;
        if (!CopyKeyMapping.matches(key, scanCode, modifiers)) {
            return;
        }

        if (action == GLFW.GLFW_PRESS) {
            HANDLER.reset();
        } else if (action == GLFW.GLFW_RELEASE) {
            HANDLER.releaseKey();
        }
    }

    public static int currentAction() {
        return currentAction;
    }

    public static boolean tryCopy(Screen screen, int key, int scanCode, int modifiers,
                                  int action, boolean alreadyHandled) {
        if (!CopyKeyMapping.matches(key, scanCode, modifiers) || action == GLFW.GLFW_RELEASE) {
            return false;
        }

        if (MinecraftAccess.currentScreen() != screen
                || (!(screen instanceof AbstractContainerScreen) && !RecipeViewerAccess.supports(screen))) {
            return false;
        }

        boolean repeat = action != GLFW.GLFW_PRESS;
        return HANDLER.pressKey(repeat, alreadyHandled, new ContainerCopyTarget(screen));
    }
}
