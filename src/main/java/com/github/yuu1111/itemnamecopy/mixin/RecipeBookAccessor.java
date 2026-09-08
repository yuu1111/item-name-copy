package com.github.yuu1111.itemnamecopy.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RecipeBookComponent.class, remap = MappingPolicy.REMAP)
public interface RecipeBookAccessor {
    @Accessor("searchBox")
    EditBox itemnamecopy$getSearchBox();
}
