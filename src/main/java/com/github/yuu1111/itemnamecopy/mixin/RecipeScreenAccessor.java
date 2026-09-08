package com.github.yuu1111.itemnamecopy.mixin;

//? if >=1.21.2 {
/*import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractRecipeBookScreen.class, remap = MappingPolicy.REMAP)
public interface RecipeScreenAccessor {
    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> itemnamecopy$getRecipeBook();
}
*///?} else {
public interface RecipeScreenAccessor {
}
//?}
