package com.github.yuu1111.itemnamecopy.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractContainerScreen.class, remap = MappingPolicy.REMAP)
public interface ContainerScreenAccessor {
    @Accessor("hoveredSlot")
    Slot itemnamecopy$getHoveredSlot();
}
