package com.github.may2beez.mayobees.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public class MouseUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static int getX(){
        return Mouse.getX() / getGuiScale();
    }

    public static int getY(){
        return (mc.displayHeight - Mouse.getY()) / getGuiScale();
    }
    public static int getGuiScale(){
        return Math.max(mc.gameSettings.guiScale, 1);
    }

    public static int getScaledWidth(){
        return mc.displayWidth / getGuiScale();
    }

    public static int getScaledHeight(){
        return mc.displayHeight / getGuiScale();
    }
}
