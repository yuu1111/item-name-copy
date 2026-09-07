package dev.itemnamecopy.test.mixin;

import dev.itemnamecopy.test.ClientTests;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class ClientTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void runTests(CallbackInfo ci) {
        if (Boolean.getBoolean("itemnamecopy.clientTest")) ClientTests.tick();
    }
}
