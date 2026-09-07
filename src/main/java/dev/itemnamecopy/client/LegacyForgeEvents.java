package dev.itemnamecopy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "itemnamecopy", value = Dist.CLIENT)
public final class LegacyForgeEvents {
    private static boolean cDown;

    private LegacyForgeEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_C) return;
        ItemNameCopyClient.beginKey(GLFW.GLFW_KEY_C, cDown ? GLFW.GLFW_REPEAT : GLFW.GLFW_PRESS);
        cDown = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenRelease(GuiScreenEvent.KeyboardKeyReleasedEvent.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_C) return;
        cDown = false;
        ItemNameCopyClient.beginKey(GLFW.GLFW_KEY_C, GLFW.GLFW_RELEASE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterRawKey(InputEvent.KeyInputEvent event) {
        if (event.getKey() != GLFW.GLFW_KEY_C) return;
        cDown = event.getAction() != GLFW.GLFW_RELEASE;
        ItemNameCopyClient.beginKey(event.getKey(), event.getAction());
    }
}
