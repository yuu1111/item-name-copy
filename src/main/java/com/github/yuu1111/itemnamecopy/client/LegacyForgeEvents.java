package com.github.yuu1111.itemnamecopy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "itemnamecopy", value = Dist.CLIENT)
public final class LegacyForgeEvents {
    private static boolean copyKeyDown;

    private LegacyForgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        if (!CopyKeyMapping.matches(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            return;
        }

        int action = copyKeyDown ? GLFW.GLFW_REPEAT : GLFW.GLFW_PRESS;
        updateCopyKey(event.getKeyCode(), event.getScanCode(), event.getModifiers(), action);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenRelease(GuiScreenEvent.KeyboardKeyReleasedEvent.Pre event) {
        if (!CopyKeyMapping.matches(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            return;
        }
        updateCopyKey(
                event.getKeyCode(), event.getScanCode(), event.getModifiers(), GLFW.GLFW_RELEASE
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterRawKey(InputEvent.KeyInputEvent event) {
        if (!CopyKeyMapping.matches(event.getKey(), event.getScanCode(), event.getModifiers())) {
            return;
        }
        updateCopyKey(event.getKey(), event.getScanCode(), event.getModifiers(), event.getAction());
    }

    private static void updateCopyKey(int key, int scanCode, int modifiers, int action) {
        copyKeyDown = action != GLFW.GLFW_RELEASE;
        ItemNameCopyClient.beginKey(key, scanCode, modifiers, action);
    }
}
