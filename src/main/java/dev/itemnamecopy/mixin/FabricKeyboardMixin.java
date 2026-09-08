package dev.itemnamecopy.mixin;

import dev.itemnamecopy.client.ItemNameCopyClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
//? if <1.17 {
/*import net.minecraft.client.gui.components.events.ContainerEventHandler;
 *///?}
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
 *///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, priority = 900)
abstract class FabricKeyboardMixin {
    //? if >=1.21.9 {
    /*@Inject(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"
            ),
            cancellable = true
    )
    private void itemnamecopy$beforeScreen(long window, int action, KeyEvent event, CallbackInfo ci) {
        boolean copied = ItemNameCopyClient.tryCopy(
                MinecraftAccess.currentScreen(), event.key(), event.modifiers(), action, false
        );
        if (copied) ci.cancel();
    }
    *///?}
    //? if >=1.21.2 && <1.21.9 {
    /*@Inject(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"
            ),
            cancellable = true
    )
    private void itemnamecopy$beforeScreen(long window, int key, int scanCode, int action, int modifiers,
                                          CallbackInfo ci) {
        boolean copied = ItemNameCopyClient.tryCopy(
                MinecraftAccess.currentScreen(), key, modifiers, action, false
        );
        if (copied) ci.cancel();
    }
    *///?}
    //? if >=1.17 && <1.21.2 {
    @Inject(
            method = "method_1454",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"
            ),
            cancellable = true
    )
    //? if >=1.19.3 {
    private static
    //?} else {
    /*private
     *///?}
    void itemnamecopy$beforeScreen(int action, Screen screen, boolean[] handled,
                                   int key, int scanCode, int modifiers, CallbackInfo ci) {
        boolean copied = ItemNameCopyClient.tryCopy(screen, key, modifiers, action, handled[0]);
        if (copied) {
            handled[0] = true;
            ci.cancel();
        }
    }
    //?}
    //? if <1.17 {
    /*@Inject(
            method = "method_1454",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/events/ContainerEventHandler;keyPressed(III)Z"
            ),
            cancellable = true
    )
    private void itemnamecopy$beforeScreen(int action, boolean[] handled, ContainerEventHandler listener,
                                          int key, int scanCode, int modifiers, CallbackInfo ci) {
        if (!(listener instanceof Screen)) return;

        boolean copied = ItemNameCopyClient.tryCopy((Screen) listener, key, modifiers, action, handled[0]);
        if (copied) {
            handled[0] = true;
            ci.cancel();
        }
    }
    *///?}
}
