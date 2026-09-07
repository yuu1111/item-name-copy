package dev.itemnamecopy.mixin;

import dev.itemnamecopy.client.ItemNameCopyClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, remap = MappingPolicy.REMAP)
abstract class KeyboardInputMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void itemnamecopy$begin(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            ItemNameCopyClient.beginKey(key, action);
        }
    }
}
