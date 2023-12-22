package com.github.may2beez.mayobees;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import com.github.may2beez.mayobees.command.MainCommand;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.ModuleManager;
import com.github.may2beez.mayobees.module.impl.other.Failsafes;
import com.github.may2beez.mayobees.util.helper.AudioManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "mayobees", useMetadata=true)
public class MayOBees {
    public static final MayOBeesConfig CONFIG = new MayOBeesConfig();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(RotationHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(AudioManager.getInstance());
        MinecraftForge.EVENT_BUS.register(Failsafes.getInstance());
        CommandManager.register(new MainCommand());
        ModuleManager.getInstance().getModules().forEach(MinecraftForge.EVENT_BUS::register);
    }
}
