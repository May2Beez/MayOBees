package com.github.may2beez.mayobees;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import com.github.may2beez.mayobees.command.MainCommand;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.MotionUpdateEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.ModuleManager;
import com.github.may2beez.mayobees.module.impl.other.Dev;
import com.github.may2beez.mayobees.module.impl.other.Failsafes;
import com.github.may2beez.mayobees.pathfinder.FlyPathFinderExecutor;
import com.github.may2beez.mayobees.pathfinder.WorldCache;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.helper.AudioManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "mayobees", useMetadata=true)
public class MayOBees {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final MayOBeesConfig CONFIG = new MayOBeesConfig();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(RotationHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(AudioManager.getInstance());
        MinecraftForge.EVENT_BUS.register(Failsafes.getInstance());
        MinecraftForge.EVENT_BUS.register(FlyPathFinderExecutor.getInstance());
        MinecraftForge.EVENT_BUS.register(Dev.getInstance());
        MinecraftForge.EVENT_BUS.register(WorldCache.getInstance());
        CommandManager.register(new MainCommand());
        ModuleManager.getInstance().getModules().forEach(MinecraftForge.EVENT_BUS::register);
    }

    @SubscribeEvent
    public void onMotionUpdate(MotionUpdateEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (MayOBeesConfig.autoSprint) {
            if (mc.thePlayer.motionX == 0 && mc.thePlayer.motionZ == 0) return;
            if (mc.gameSettings.keyBindSprint.isKeyDown()) return;
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, true);
        }
    }
}
