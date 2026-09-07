package dev.itemnamecopy.mixin;

import dev.itemnamecopy.client.ItemNameCopyClient;
import dev.itemnamecopy.client.MinecraftAccess;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
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
    /*@Inject(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"),
            cancellable = true)
    private void itemnamecopy$beforeScreen(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (ItemNameCopyClient.tryCopy(MinecraftAccess.currentScreen(), event.key(), event.modifiers(), action, false)) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "method_1454", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"), cancellable = true)
    private static void itemnamecopy$beforeScreen(int action, Screen screen, boolean[] handled,
                                                int key, int scanCode, int modifiers, CallbackInfo ci) {
        if (ItemNameCopyClient.tryCopy(screen, key, modifiers, action, handled[0])) {
            handled[0] = true;
            ci.cancel();
        }
    }
    //?}
}
