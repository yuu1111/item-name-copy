package com.github.yuu1111.itemnamecopy.mixin;

import com.github.yuu1111.itemnamecopy.client.ItemNameCopyClient;
import com.github.yuu1111.itemnamecopy.client.MinecraftAccess;
import net.minecraft.client.KeyboardHandler;
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, remap = MappingPolicy.REMAP)
abstract class KeyboardInputMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "keyPress", at = @At("HEAD"))
    private void itemnamecopy$begin(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != MinecraftAccess.windowHandle()) return;
        ItemNameCopyClient.beginKey(event.key(), event.scancode(), event.modifiers(), action);
    }
    *///?} else {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void itemnamecopy$begin(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window != MinecraftAccess.windowHandle()) return;
        ItemNameCopyClient.beginKey(key, scanCode, modifiers, action);
    }
    //?}
}
