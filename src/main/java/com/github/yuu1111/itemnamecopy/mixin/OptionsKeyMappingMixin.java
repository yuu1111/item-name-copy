package com.github.yuu1111.itemnamecopy.mixin;

import com.github.yuu1111.itemnamecopy.client.CopyKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(value = Options.class, remap = MappingPolicy.REMAP)
abstract class OptionsKeyMappingMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V")
    )
    private void itemnamecopy$registerKeyMapping(CallbackInfo ci) {
        //? if <1.21.9 {
        KeyMappingAccessor.itemnamecopy$getCategorySortOrder().putIfAbsent(
                CopyKeyMapping.CATEGORY,
                KeyMappingAccessor.itemnamecopy$getCategorySortOrder().size()
        );
        //?}
        this.keyMappings = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 1);
        this.keyMappings[this.keyMappings.length - 1] = CopyKeyMapping.mapping();
    }
}
