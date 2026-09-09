package com.github.yuu1111.itemnamecopy.mixin;

import com.github.yuu1111.itemnamecopy.client.CopyKeyMapping;
import net.minecraft.client.KeyMapping;
//? if <1.18 {
/*import net.minecraft.client.gui.screens.controls.ControlsScreen;
*///?} elif <1.21 {
/*import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
*///?} else {
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
//?}
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        //? if <1.18 {
        /*ControlsScreen.class
        *///?} else {
        KeyBindsScreen.class
        //?}
)
abstract class FabricKeyBindsScreenMixin {
    @Shadow
    public KeyMapping selectedKey;

    //? if >=1.21.9 {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void itemnamecopy$captureChord(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        capture(event.key(), event.modifiers(), cir);
    }
    *///?} else {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void itemnamecopy$captureChord(int key, int scanCode, int modifiers,
                                           CallbackInfoReturnable<Boolean> cir) {
        capture(key, modifiers, cir);
    }
    //?}

    private void capture(int key, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (selectedKey != CopyKeyMapping.mapping()) return;
        if (isModifier(key)) {
            cir.setReturnValue(true);
            return;
        }
        CopyKeyMapping.prepareRebind(modifiers);
    }

    private static boolean isModifier(int key) {
        return key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }
}
