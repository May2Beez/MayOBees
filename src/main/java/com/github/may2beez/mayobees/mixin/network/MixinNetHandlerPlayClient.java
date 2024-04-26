package com.github.may2beez.mayobees.mixin.network;

import com.github.may2beez.mayobees.event.SpawnParticleEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
    @Inject(method = "handleParticles", at = @At(value = "HEAD"))
    public void handleParticles(S2APacketParticles packetIn, CallbackInfo ci) {
        SpawnParticleEvent event = new SpawnParticleEvent(
                packetIn.getParticleType(),
                packetIn.isLongDistance(),
                packetIn.getXCoordinate(),
                packetIn.getYCoordinate(),
                packetIn.getZCoordinate(),
                packetIn.getXOffset(),
                packetIn.getYOffset(),
                packetIn.getZOffset(),
                packetIn.getParticleArgs()
        );
        MinecraftForge.EVENT_BUS.post(event);
    }
}
