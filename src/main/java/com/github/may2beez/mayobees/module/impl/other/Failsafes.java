package com.github.may2beez.mayobees.module.impl.other;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.module.ModuleManager;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.helper.AudioManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Failsafes {

    private static Failsafes instance;

    public static Failsafes getInstance() {
        if (instance == null) {
            instance = new Failsafes();
        }
        return instance;
    }
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] teleportItems = new String[] {"Void", "Hyperion", "Aspect"};

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (!MayOBeesConfig.stopMacrosOnWorldChange) return;

        List<IModuleActive> activeModules = ModuleManager.getInstance().getModules().stream().filter(mod -> mod instanceof IModuleActive && mod.isRunning()).map(mod -> (IModuleActive) mod).collect(Collectors.toList());

        if (!activeModules.isEmpty()) {
            LogUtils.warn("[Failsafes] Detected World Change, Stopping All Macros");
        }
        for (IModuleActive m : activeModules) {
            m.onDisable();
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (!MayOBeesConfig.stopMacrosOnRotationTeleportCheck) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.packet instanceof S08PacketPlayerPosLook)) return;
        if (mc.thePlayer.getHeldItem() != null && Arrays.stream(teleportItems).anyMatch(i -> mc.thePlayer.getHeldItem().getDisplayName().contains(i))) return;
        List<IModuleActive> activeModules = ModuleManager.getInstance().getModules().stream().filter(mod -> mod instanceof IModuleActive && mod.isRunning()).map(mod -> (IModuleActive) mod).collect(Collectors.toList());
        if (activeModules.isEmpty()) return;

        activeModules.forEach(m -> {
            m.onDisable();
            LogUtils.warn("[Failsafes] Stopping " + m.getName() + " due to rotation check or teleport check!");
        });

        LogUtils.warn("[Failsafes] Rotation check or teleport check!");
        AudioManager.getInstance().playSound();
    }
}
