package com.github.may2beez.mayobees.command;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommandGroup;
import com.github.may2beez.mayobees.MayOBees;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.module.impl.player.Brush;
import com.github.may2beez.mayobees.module.impl.utils.AutoBazaar;
import com.github.may2beez.mayobees.module.impl.utils.helper.BazaarConfig;
import com.github.may2beez.mayobees.util.LogUtils;

@Command(value = "m2b", description = "MayOBees Command", aliases = {"mayobees"})
public class MainCommand {
    @Main
    private void main() {
        MayOBees.CONFIG.openGui();
    }

    @SubCommandGroup(value = "brush")
    private static class BrushCommand {
        @SubCommand
        private void setList(int list) {
            if (list < 0) {
                list = 0;
            }
            if (list >= Brush.getInstance().getWaypoints().size()) {
                list = Brush.getInstance().getWaypoints().size() - 1;
            }
            Brush.getInstance().setCurrentWaypointList(list);
            LogUtils.info("Set current waypoint list to " + list);
        }

        @SubCommand
        private void offset(int waypoint, int xOffset, int yOffset, int zOffset) {
            if (waypoint < 0) {
                waypoint = 0;
            }
            int currentList = Brush.getInstance().getCurrentWaypointList();
            if (waypoint >= Brush.getInstance().getWaypoints().get(currentList).getWaypoints().size()) {
                waypoint = Brush.getInstance().getWaypoints().get(currentList).getWaypoints().size() - 1;
            }
            Brush.getInstance().getWaypoints().get(currentList).getWaypoints().set(waypoint, Brush.getInstance().getWaypoints().get(currentList).getWaypoints().get(waypoint).add(xOffset, yOffset, zOffset));
            LogUtils.info("Offset waypoint " + waypoint + " by " + xOffset + " " + yOffset + " " + zOffset);
        }
    }

    @SubCommand
    private void bz(String itemName, int amount) {
        LogUtils.info("Item: " + itemName);
        BazaarConfig config = new BazaarConfig(
                itemName.replace("_", " "),
                amount,
                MayOBeesConfig.visitorHelperSpendThreshold * 1000,
                MayOBeesConfig.visitorHelperGuiDelay,
                MayOBeesConfig.visitorHelperGuiDelayRandomness,
                MayOBeesConfig.visitorHelperGuiTimeoutTime);
        AutoBazaar.getInstance().buy(config);
    }
}
