package com.github.may2beez.mayobees.mixin.network;

import com.github.may2beez.mayobees.event.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"))
    private void sendPacketPre(Packet<?> packet, CallbackInfo callbackInfo) {
        MinecraftForge.EVENT_BUS.post(new PacketEvent.Send(packet));
    }

    @Inject(method = "channelRead0*", at = @At("HEAD"))
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        if (packet.getClass().getSimpleName().startsWith("S")) {
            MinecraftForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
        } else if (packet.getClass().getSimpleName().startsWith("C")) {
            MinecraftForge.EVENT_BUS.post(new PacketEvent.Send(packet));
        }
    }
}
