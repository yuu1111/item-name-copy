package dev.itemnamecopy.client;

import com.mojang.blaze3d.platform.ClipboardManager;
import dev.itemnamecopy.core.CopyShortcutHandler;
import dev.itemnamecopy.mixin.ContainerScreenAccessor;
import dev.itemnamecopy.mixin.RecipeBookAccessor;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItemNameCopyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("itemnamecopy");
    private static final CopyShortcutHandler HANDLER = new CopyShortcutHandler();
    private static final ClipboardManager CLIPBOARD = new ClipboardManager();
    private static int currentAction;

    private ItemNameCopyClient() {}

    public static void beginKey(int key, int action) {
        currentAction = action;
        if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) HANDLER.reset();
        if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_RELEASE) {
            HANDLER.releaseC();
        }
    }

    public static int currentAction() {
        return currentAction;
    }

    public static boolean tryCopy(Screen screen, int key, int modifiers, int action, boolean handled) {
        if (key != GLFW.GLFW_KEY_C || action == GLFW.GLFW_RELEASE) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (MinecraftAccess.currentScreen() != screen || !(screen instanceof AbstractContainerScreen)) return false;
        // GLFWのModifierはmacOSでも左右の物理Ctrlを表す
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean other = (modifiers & (GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_SUPER)) != 0;
        return HANDLER.pressC(control, other, action != GLFW.GLFW_PRESS, handled, new CopyShortcutHandler.Target() {
            public boolean isTextInputFocused() {
                RecipeBookComponent recipeBook = MinecraftAccess.recipeBook(screen);
                if (recipeBook != null) {
                    EditBox search = ((RecipeBookAccessor) recipeBook).itemnamecopy$getSearchBox();
                    if (recipeBook.isVisible() && search != null && search.isFocused()) return true;
                }
                return hasFocusedTextInput(screen);
            }

            public Optional<String> hoveredItemName() {
                Slot slot = ((ContainerScreenAccessor) screen).itemnamecopy$getHoveredSlot();
                if (slot == null || !slot.hasItem()) return Optional.empty();
                return Optional.of(slot.getItem().getHoverName().getString());
            }

            public boolean writeClipboard(String name) {
                try {
                    // KeyboardHandlerは空文字を無視するため同じ内部APIを直接使う
                    MinecraftAccess.writeClipboard(CLIPBOARD, name);
                    boolean success = name.equals(minecraft.keyboardHandler.getClipboard());
                    if (!success) LOGGER.warn("Clipboard did not retain the item name");
                    return success;
                } catch (RuntimeException exception) {
                    LOGGER.warn("Could not copy the item name", exception);
                    return false;
                }
            }

            public void showFeedback(String name) {
                MinecraftAccess.feedback(name);
            }
        });
    }

    private static boolean hasFocusedTextInput(GuiEventListener listener) {
        if (listener instanceof EditBox && listener.isFocused()) return true;
        if (listener instanceof ContainerEventHandler) {
            ContainerEventHandler container = (ContainerEventHandler) listener;
            for (GuiEventListener child : container.children()) {
                if (hasFocusedTextInput(child)) return true;
            }
        }
        return false;
    }
}
