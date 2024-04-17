package com.github.may2beez.mayobees.mixin.network;

import com.github.may2beez.mayobees.event.SpawnParticleEvent;
import com.github.may2beez.mayobees.event.UpdateTablistEvent;
import com.github.may2beez.mayobees.util.TablistUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.github.may2beez.mayobees.util.TablistUtils.playerOrdering;

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

    @Unique
    private final List<String> farmHelperV2$previousTablist = new ArrayList<>();
    @Unique
    private final List<String> farmHelperV2$previousFooter = new ArrayList<>();

    @Inject(method = "handlePlayerListItem", at = @At(value = "RETURN"))
    public void handlePlayerListItem(S38PacketPlayerListItem packetIn, CallbackInfo ci) {
        List<String> tablist = new ArrayList<>();
        List<NetworkPlayerInfo> players =
                playerOrdering.sortedCopy(Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap());

        GuiPlayerTabOverlay tabOverlay = Minecraft.getMinecraft().ingameGUI.getTabList();

        for (NetworkPlayerInfo info : players) {
            tablist.add(StringUtils.stripControlCodes(tabOverlay.getPlayerName(info)));
        }
        if (tablist.equals(farmHelperV2$previousTablist)) return;
        farmHelperV2$previousTablist.clear();
        farmHelperV2$previousTablist.addAll(tablist);
        TablistUtils.setCachedTablist(tablist);
        MinecraftForge.EVENT_BUS.post(new UpdateTablistEvent(tablist, System.currentTimeMillis()));
    }
}
