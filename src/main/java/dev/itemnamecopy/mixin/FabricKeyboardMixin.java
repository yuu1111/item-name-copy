package dev.itemnamecopy.mixin;

import dev.itemnamecopy.client.ItemNameCopyClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, priority = 900)
abstract class FabricKeyboardMixin {
    @Inject(method = "method_1454", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"), cancellable = true)
    private static void itemnamecopy$beforeScreen(int action, Screen screen, boolean[] handled,
                                                int key, int scanCode, int modifiers, CallbackInfo ci) {
        if (ItemNameCopyClient.tryCopy(screen, key, modifiers, action, handled[0])) {
            handled[0] = true;
            ci.cancel();
        }
    }
}
