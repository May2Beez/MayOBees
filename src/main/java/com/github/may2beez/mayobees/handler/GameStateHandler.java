package com.github.may2beez.mayobees.handler;

import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.ScoreboardUtils;
import com.github.may2beez.mayobees.util.TablistUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameStateHandler {
    private static GameStateHandler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");
    @Getter
    private Location lastLocation = Location.TELEPORTING;
    @Getter
    private Location location = Location.TELEPORTING;
    @Getter
    private String serverIP;
    public static GameStateHandler getInstance() {
        if (instance == null) {
            instance = new GameStateHandler();
        }
        return instance;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        lastLocation = location;
        location = Location.TELEPORTING;
    }


    @SubscribeEvent
    public void onTickCheckLocation(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }

        if (TablistUtils.getTabList().size() == 1 && ScoreboardUtils.getScoreboardLines().isEmpty() && InventoryUtils.isInventoryEmpty(mc.thePlayer)) {
            lastLocation = location;
            location = Location.LIMBO;
            return;
        }

        for (String line : TablistUtils.getTabList()) {
            if (StringUtils.stripControlCodes(line).startsWith("Dungeon:")) {
                lastLocation = location;
                location = Location.DUNGEON;
                return;
            }
            if(!line.startsWith("Area: ")) continue;
            Matcher matcher = areaPattern.matcher(line);
            if (matcher.find()) {
                String area = matcher.group(1);
                for (Location island : Location.values()) {
                    if (area.equals(island.getName())) {
                        lastLocation = location;
                        location = island;
                        return;
                    }
                }
            }
        }

        if (!ScoreboardUtils.getScoreboardTitle().contains("SKYBLOCK") && !ScoreboardUtils.getScoreboardLines().isEmpty() && ScoreboardUtils.cleanSB(ScoreboardUtils.getScoreboardLines().get(0)).contains("www.hypixel.net")) {
            lastLocation = location;
            location = Location.LOBBY;
            return;
        }
        if (location != Location.TELEPORTING) {
            lastLocation = location;
        }
        location = Location.TELEPORTING;
    }

    @Getter
    public enum Location {
        PRIVATE_ISLAND("Private Island"),
        HUB("Hub"),
        THE_PARK("The Park"),
        THE_FARMING_ISLANDS("The Farming Islands"),
        SPIDER_DEN("Spider's Den"),
        THE_END("The End"),
        CRIMSON_ISLE("Crimson Isle"),
        GOLD_MINE("Gold Mine"),
        DEEP_CAVERNS("Deep Caverns"),
        DWARVEN_MINES("Dwarven Mines"),
        CRYSTAL_HOLLOWS("Crystal Hollows"),
        JERRY_WORKSHOP("Jerry's Workshop"),
        DUNGEON_HUB("Dungeon Hub"),
        LIMBO("UNKNOWN"),
        LOBBY("PROTOTYPE"),
        GARDEN("Garden"),
        DUNGEON("Dungeon"),
        TELEPORTING("Teleporting");

        private final String name;

        Location(String name) {
            this.name = name;
        }
    }
}
