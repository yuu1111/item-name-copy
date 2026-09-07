package dev.itemnamecopy.test.mixin;

import dev.itemnamecopy.test.ClientTests;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class ControlStateMixin {
    @Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
    private static void controlState(CallbackInfoReturnable<Boolean> cir) {
        if (ClientTests.modifiers != null) {
            cir.setReturnValue((ClientTests.modifiers & GLFW.GLFW_MOD_CONTROL) != 0);
        }
    }
}
