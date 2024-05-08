package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.UpdateTablistEvent;
import com.github.may2beez.mayobees.module.IModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NickHider implements IModule {
    private static NickHider instance;

    public static NickHider getInstance() {
        if (instance == null) instance = new NickHider();
        return instance;
    }

    private final HashMap<String, String> cachedNames = new HashMap<>();
    private final CopyOnWriteArrayList<String> currentPlayers = new CopyOnWriteArrayList<>();

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.nickHider;
    }

    @Override
    public String getName() {
        return "NickHider";
    }

    public String apply(final String text) {
        StringBuilder nameBuilder = new StringBuilder(text);
        for (String player : currentPlayers) {
            int index = nameBuilder.indexOf(player);
            if (index != -1) {
                nameBuilder.replace(index, index + player.length(), this.cachedNames.getOrDefault(player, "NoNickFound"));
            }
        }
        return nameBuilder.toString();
    }

    @SubscribeEvent
    public void onPlayerListUpdate(UpdateTablistEvent event) {
        this.currentPlayers.clear();
        for (NetworkPlayerInfo npi : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
            String name = npi.getGameProfile().getName();
            String nick = name.equalsIgnoreCase(Minecraft.getMinecraft().thePlayer.getGameProfile().getName())
                    ? MayOBeesConfig.nickHiderUserName
                    : RandomStringUtils.randomAlphanumeric(16);
            this.cachedNames.putIfAbsent(name, nick);
            this.currentPlayers.add(name);
        }
    }
}
