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

    public static void beginKey(int key, int action) {
        currentAction = action;
        if (key != GLFW.GLFW_KEY_C) {
            return;
        }

        if (action == GLFW.GLFW_PRESS) {
            HANDLER.reset();
        } else if (action == GLFW.GLFW_RELEASE) {
            HANDLER.releaseC();
        }
    }

    public static int currentAction() {
        return currentAction;
    }

    public static boolean tryCopy(Screen screen, int key, int modifiers, int action, boolean alreadyHandled) {
        if (key != GLFW.GLFW_KEY_C || action == GLFW.GLFW_RELEASE) {
            return false;
        }

        if (MinecraftAccess.currentScreen() != screen || !(screen instanceof AbstractContainerScreen)) {
            return false;
        }

        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean otherModifier = hasOtherModifier(modifiers);
        boolean repeat = action != GLFW.GLFW_PRESS;
        return HANDLER.pressC(
                control,
                otherModifier,
                repeat,
                alreadyHandled,
                new ContainerCopyTarget(screen)
        );
    }

    private static boolean hasOtherModifier(int modifiers) {
        int otherModifiers = GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_SUPER;
        return (modifiers & otherModifiers) != 0;
    }
}
