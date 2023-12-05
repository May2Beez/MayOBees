package com.github.may2beez.mayobees.util;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class LogUtils {
    private static final String prefix = "§4§ka§r§e[MayOBees]§4§ka§r ";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void info(String message) {
        if (mc.thePlayer == null) {
            System.out.println(prefix + "§3" + message);
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(prefix + "§3" + message));
    }

    public static void warn(String message) {
        if (mc.thePlayer == null) {
            System.out.println(prefix + "§c" + message);
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(prefix + "§c" + message));
    }

    public static void error(String message) {
        if (mc.thePlayer == null) {
            System.out.println(prefix + "§4" + message);
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(prefix + "§4" + message));
    }

    public static void debug(String message) {
        if (mc.thePlayer == null || !MayOBeesConfig.debugMode) {
            System.out.println(prefix + "§b" + message);
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(prefix + "§b" + message));
    }
}
