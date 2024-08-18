package com.github.may2beez.mayobees.mixin.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {
    @Accessor
    int getXSize();

    @Accessor
    int getYSize();

    @Accessor(value = "theSlot")
    Slot getTheSlot();
}
