package com.github.may2beez.mayobees.util;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;

public class LogUtils {
    private static final String prefix = "§4§ka§r§eMayOBees§4§ka§r§l ";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void info(String message) {
        if (mc.thePlayer == null) {
            System.out.println(StringUtils.stripControlCodes(prefix) + message);
            return;
        }
        sendMsg(prefix + "§f" + message);
    }

    public static void warn(String message) {
        if (mc.thePlayer == null) {
            System.out.println(StringUtils.stripControlCodes(prefix) + message);
            return;
        }
        sendMsg(prefix + "§6" + message);
    }

    public static void error(String message) {
        if (mc.thePlayer == null) {
            System.out.println(StringUtils.stripControlCodes(prefix) + message);
            return;
        }
        sendMsg(prefix + "§c" + message);
    }

    public static void debug(String message) {
        if (mc.thePlayer == null || !MayOBeesConfig.debugMode) {
            System.out.println(StringUtils.stripControlCodes(prefix) + message);
            return;
        }
        sendMsg(prefix + "§7" + message);
    }

    private static void sendMsg(String message) {
        try {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        } catch (Exception e) {
            System.out.println(message);
        }
    }
}
