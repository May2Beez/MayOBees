package com.github.may2beez.mayobees.mixin.client;

import com.github.may2beez.mayobees.event.ClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "clickMouse", at = @At("HEAD"))
    public void clickMouse(CallbackInfo ci) {
        MovingObjectPosition objectMouseOver = Minecraft.getMinecraft().objectMouseOver;
        if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Left(objectMouseOver.entityHit));
        } else if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Left(objectMouseOver.getBlockPos()));
        } else {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Left());
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"))
    public void rightClickMouse(CallbackInfo ci) {
        MovingObjectPosition objectMouseOver = Minecraft.getMinecraft().objectMouseOver;
        if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Right(objectMouseOver.entityHit));
        } else if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Right(objectMouseOver.getBlockPos()));
        } else {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Right());
        }
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"))
    public void middleClickMouse(CallbackInfo ci) {
        MovingObjectPosition objectMouseOver = Minecraft.getMinecraft().objectMouseOver;
        if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Middle(objectMouseOver.entityHit));
        } else if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Middle(objectMouseOver.getBlockPos()));
        } else {
            MinecraftForge.EVENT_BUS.post(new ClickEvent.Middle());
        }
    }
}
