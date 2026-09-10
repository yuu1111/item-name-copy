package com.github.yuu1111.itemnamecopy.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = KeyMapping.class, remap = MappingPolicy.REMAP)
public interface KeyMappingAccessor {
    //? if forge && >=1.16.1 && <1.16.2 {
    /*@Accessor("CATEGORY_ORDER")
    *///?} else {
    @Accessor("CATEGORY_SORT_ORDER")
    //?}
    static Map<String, Integer> itemnamecopy$getCategorySortOrder() {
        throw new AssertionError();
    }
}
