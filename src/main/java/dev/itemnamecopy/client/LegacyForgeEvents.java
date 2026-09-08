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
    private static boolean copyKeyDown;

    private LegacyForgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        if (!isCopyKey(event.getKeyCode())) return;

        int action = copyKeyDown ? GLFW.GLFW_REPEAT : GLFW.GLFW_PRESS;
        updateCopyKey(action);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreenRelease(GuiScreenEvent.KeyboardKeyReleasedEvent.Pre event) {
        if (!isCopyKey(event.getKeyCode())) return;
        updateCopyKey(GLFW.GLFW_RELEASE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterRawKey(InputEvent.KeyInputEvent event) {
        if (!isCopyKey(event.getKey())) return;
        updateCopyKey(event.getAction());
    }

    private static boolean isCopyKey(int key) {
        return key == GLFW.GLFW_KEY_C;
    }

    private static void updateCopyKey(int action) {
        copyKeyDown = action != GLFW.GLFW_RELEASE;
        ItemNameCopyClient.beginKey(GLFW.GLFW_KEY_C, action);
    }
}
