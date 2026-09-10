package com.github.yuu1111.itemnamecopy.mixin;

import com.github.yuu1111.itemnamecopy.client.CopyKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.io.File;

@Mixin(value = Options.class, remap = MappingPolicy.REMAP)
abstract class OptionsKeyMappingMixin {
    @Shadow
    @Final
    @Mutable
    //? if forge && >=1.16.1 && <1.16.2 {
    /*public KeyMapping[] keyBindings;
    *///?} else {
    public KeyMapping[] keyMappings;
    //?}

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target =
                    //? if forge && >=1.16.1 && <1.16.2 {
                    /*"Lnet/minecraft/client/Options;loadOptions()V"
                    *///?} else {
                    "Lnet/minecraft/client/Options;load()V"
                    //?}
            )
    )
    private void itemnamecopy$loadWithKeyMapping(Options options, Minecraft minecraft, File gameDirectory) {
        //? if <1.21.9 {
        KeyMappingAccessor.itemnamecopy$getCategorySortOrder().putIfAbsent(
                CopyKeyMapping.CATEGORY,
                KeyMappingAccessor.itemnamecopy$getCategorySortOrder().size()
        );
        //?}
        //? if forge && >=1.16.1 && <1.16.2 {
        /*this.keyBindings = Arrays.copyOf(this.keyBindings, this.keyBindings.length + 1);
        this.keyBindings[this.keyBindings.length - 1] = CopyKeyMapping.mapping();
        *///?} else {
        this.keyMappings = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 1);
        this.keyMappings[this.keyMappings.length - 1] = CopyKeyMapping.mapping();
        //?}
        CopyKeyMapping.beginOptionsLoad(gameDirectory);
        //? if forge && >=1.16.1 && <1.16.2 {
        /*options.loadOptions();
        *///?} else {
        options.load();
        //?}
        CopyKeyMapping.finishOptionsLoad();
    }
}
