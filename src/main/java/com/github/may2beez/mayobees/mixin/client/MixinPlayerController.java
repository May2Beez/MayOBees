package com.github.may2beez.mayobees.mixin.client;

import com.github.may2beez.mayobees.event.ClickEntityEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerController {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    public void attackEntityHead(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        System.out.println("attackEntityHead: " + targetEntity);
        MinecraftForge.EVENT_BUS.post(new ClickEntityEvent.Left(targetEntity));
    }

    @Inject(method = "isPlayerRightClickingOnEntity", at = @At("HEAD"))
    public void interactWithEntitySendPacketHead(EntityPlayer player, Entity entityIn, MovingObjectPosition movingObject, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("interactWithEntitySendPacketHead: " + entityIn);
        MinecraftForge.EVENT_BUS.post(new ClickEntityEvent.Right(entityIn));
    }
}
