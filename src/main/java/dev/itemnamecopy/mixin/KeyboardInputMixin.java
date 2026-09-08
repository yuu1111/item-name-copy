package dev.itemnamecopy.mixin;

import dev.itemnamecopy.client.ItemNameCopyClient;
import dev.itemnamecopy.client.MinecraftAccess;
import net.minecraft.client.KeyboardHandler;
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class)
abstract class KeyboardInputMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "keyPress", at = @At("HEAD"))
    private void itemnamecopy$begin(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != MinecraftAccess.windowHandle()) return;
        ItemNameCopyClient.beginKey(event.key(), action);
    }
    *///?} else {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void itemnamecopy$begin(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window != MinecraftAccess.windowHandle()) return;
        ItemNameCopyClient.beginKey(key, action);
    }
    //?}
}
