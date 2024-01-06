package com.github.may2beez.mayobees.module.impl.combat;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoClicker implements IModuleActive {
    private static AutoClicker instance;

    public static AutoClicker getInstance() {
        if (instance == null) {
            instance = new AutoClicker();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    private long nextClick = System.currentTimeMillis();
    private boolean enabled = false;

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public String getName() {
        return "AutoClicker";
    }

    @Override
    public void onEnable() {
        nextClick = System.currentTimeMillis();
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning() && MayOBeesConfig.autoClickerMode != 1) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;
        if (mc.thePlayer.isUsingItem()) return;

        long nowMillis = System.currentTimeMillis();
        if (nowMillis < nextClick) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (!MayOBeesConfig.autoClickerType && (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || mop.entityHit.isDead)) {
            // If the player is not looking at an entity, don't click
            return;
        }

        switch (MayOBeesConfig.autoClickerMode) {
            case 0: {
                if (!MayOBeesConfig.autoClickerType && mc.gameSettings.keyBindAttack.isKeyDown()) { //left click
                    KeyBindUtils.leftClick();
                }
                if (MayOBeesConfig.autoClickerType && mc.gameSettings.keyBindUseItem.isKeyDown()) { //right click
                    KeyBindUtils.rightClick();
                }
                nextClickDelay();
                break;
            }
            case 1: {
                if (!MayOBeesConfig.autoClickerKeybind.isActive()) break;
                if (!MayOBeesConfig.autoClickerType) { //left click
                    KeyBindUtils.leftClick();
                }
                if (MayOBeesConfig.autoClickerType) { //right click
                    KeyBindUtils.rightClick();
                }
                nextClickDelay();
                break;
            }
            case 2: {
                if (!MayOBeesConfig.autoClickerType) { //left click
                    KeyBindUtils.leftClick();
                }
                if (MayOBeesConfig.autoClickerType) { //right click
                    KeyBindUtils.rightClick();
                }
                nextClickDelay();
                break;
            }
        }
    }

    private void nextClickDelay() {
        long nowMillis = System.currentTimeMillis();
        int maxCpsMillis = (int) (1000.0 / MayOBeesConfig.autoClickerMinCPS);
        int minCpsMillis = (int) (1000.0 / MayOBeesConfig.autoClickerMaxCPS);
        if (maxCpsMillis >= minCpsMillis) {
            maxCpsMillis = minCpsMillis - 1;
        }
        nextClick = (long) (nowMillis + maxCpsMillis + (Math.random() * (maxCpsMillis - minCpsMillis)));
    }
}
