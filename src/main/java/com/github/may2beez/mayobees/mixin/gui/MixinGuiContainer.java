package com.github.may2beez.mayobees.mixin.gui;

import com.github.may2beez.mayobees.event.DrawScreenAfterEvent;
import com.github.may2beez.mayobees.event.GuiClosedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen_after(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new DrawScreenAfterEvent(Minecraft.getMinecraft().currentScreen));
    }

    @Inject(method = "onGuiClosed", at = @At("RETURN"))
    public void onGuiClosed(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new GuiClosedEvent());
    }
}
