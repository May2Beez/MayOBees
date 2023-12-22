package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.SpawnParticleEvent;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.module.impl.combat.MobKiller;
import com.github.may2beez.mayobees.util.*;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Fishing implements IModuleActive {
    private static Fishing instance;
    public static Fishing getInstance() {
        if (instance == null) {
            instance = new Fishing();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final List<String> fishingMobs = JsonUtils.getListFromUrl("https://raw.githubusercontent.com/May2Beez/May2BeezQoL/master/sea_creatures_list.json", "mobs");

    private final Timer throwTimer = new Timer();
    private final Timer inWaterTimer = new Timer();

    private final Clock attackDelay = new Clock();

    private final Timer antiAfkTimer = new Timer();

    private double oldBobberPosY = 0.0D;
    private Rotation startRotation = null;

    private static final CopyOnWriteArrayList<ParticleEntry> particles = new CopyOnWriteArrayList<>();

    private int rodSlot = 0;

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.fishing;
    }

    private enum AutoFishState {
        THROWING,
        IN_WATER,
        FISH_BITE
    }

    private enum AntiAfkState {
        AWAY,
        BACK
    }

    private AntiAfkState antiAfkState = AntiAfkState.AWAY;

    private AutoFishState currentState = AutoFishState.THROWING;

    @Override
    public void onEnable() {
        currentState = AutoFishState.THROWING;
        throwTimer.reset();
        inWaterTimer.reset();
        attackDelay.reset();
        antiAfkTimer.reset();
        if (MayOBeesConfig.mouseUngrab)
            UngrabUtils.ungrabMouse();
        oldBobberPosY = 0.0D;
        particles.clear();
        rodSlot = InventoryUtils.getSlotIdOfItemInHotbar("Rod");
        if (rodSlot == -1) {
            LogUtils.error("No rod found in hotbar!");
            onDisable();
            return;
        }
        startRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), MayOBeesConfig.sneakWhileFishing);
        MobKiller.getInstance().start();
        MobKiller.getInstance().setTarget(fishingMobs.stream().filter(name -> !name.toLowerCase().contains("squid")).toArray(String[]::new));
    }

    @Override
    public void onDisable() {
        if (MayOBeesConfig.sneakWhileFishing) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
        KeyBindUtils.stopMovement();
        RotationHandler.getInstance().reset();
        MobKiller.getInstance().stop();
        UngrabUtils.regrabMouse();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !isRunning() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;
        ItemStack heldItem;

        particles.removeIf(p -> (System.currentTimeMillis() - p.timeAdded) > 1000);

        if (MobKiller.hasTarget) {
            return;
        }

        if (MayOBeesConfig.antiAfkWhileFishing && antiAfkTimer.hasPassed(3000 + new Random().nextInt(1500))) {
            antiAfkTimer.reset();

            if (RotationHandler.getInstance().isRotating()) {
                switch (antiAfkState) {
                    case AWAY: {
                        Rotation randomRotation = new Rotation(startRotation.getYaw() + (-2 + new Random().nextInt(4)), startRotation.getPitch() + (-2 + new Random().nextInt(4)));
                        RotationHandler.getInstance().easeTo(
                                new RotationConfiguration(randomRotation, 160, RotationConfiguration.RotationType.CLIENT, null));
                        antiAfkState = AntiAfkState.BACK;
                        break;
                    }
                    case BACK: {
                        RotationHandler.getInstance().easeTo(
                                new RotationConfiguration(startRotation, 180, RotationConfiguration.RotationType.CLIENT, null));
                        antiAfkState = AntiAfkState.AWAY;
                        break;
                    }
                }
            }
        }


        if (MayOBeesConfig.sneakWhileFishing) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        }

        switch (currentState) {
            case THROWING: {
                if (mc.thePlayer.fishEntity == null && throwTimer.hasPassed(250) && RotationHandler.getInstance().isRotating()) {
                    mc.thePlayer.inventory.currentItem = rodSlot;
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                    throwTimer.reset();
                    inWaterTimer.reset();
                    currentState = AutoFishState.IN_WATER;
                    break;
                }
                if (throwTimer.hasPassed(2500) && mc.thePlayer.fishEntity != null) {
                    currentState = AutoFishState.FISH_BITE;
                }
                break;
            }
            case IN_WATER: {
                heldItem = mc.thePlayer.getHeldItem();
                if (heldItem != null && heldItem.getItem() == Items.fishing_rod) {
                    if (throwTimer.hasPassed(500) && mc.thePlayer.fishEntity != null) {
                        if (mc.thePlayer.fishEntity.isInWater() || mc.thePlayer.fishEntity.isInLava()) {
                            EntityFishHook bobber = mc.thePlayer.fishEntity;
                            if (inWaterTimer.hasPassed(2500) && Math.abs(bobber.motionX) < 0.01 && Math.abs(bobber.motionZ) < 0.01) {
                                double movement = bobber.posY - oldBobberPosY;
                                oldBobberPosY = bobber.posY;
                                if ((movement < -0.04 && isBobberNearParticles(bobber)) || bobber.caughtEntity != null) {
                                    currentState = AutoFishState.FISH_BITE;
                                }
                            }
                            break;
                        }
                        if (inWaterTimer.hasPassed(2500)) {
                            currentState = AutoFishState.FISH_BITE;
                        }
                        break;
                    }
                    if (throwTimer.hasPassed(1000) && mc.thePlayer.fishEntity == null) {
                        throwTimer.reset();
                        currentState = AutoFishState.THROWING;
                    }
                    break;
                }
                mc.thePlayer.inventory.currentItem = rodSlot;
                break;
            }
            case FISH_BITE: {
                mc.thePlayer.inventory.currentItem = rodSlot;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                RotationHandler.getInstance().easeTo(
                        new RotationConfiguration(startRotation, 45L, RotationConfiguration.RotationType.CLIENT, null));
                throwTimer.reset();
                currentState = AutoFishState.THROWING;
                break;
            }
        }
    }

    @SubscribeEvent
    public void handleParticles(SpawnParticleEvent packet) {
        if (packet.getParticleTypes() == EnumParticleTypes.WATER_WAKE || packet.getParticleTypes() == EnumParticleTypes.SMOKE_NORMAL || packet.getParticleTypes() == EnumParticleTypes.FLAME) {
            particles.add(new ParticleEntry(new Vec3(packet.getXCoord(), packet.getYCoord(), packet.getZCoord()), System.currentTimeMillis()));
        }
    }

    public double getHorizontalDistance(Vec3 vec1, Vec3 vec2) {
        double d0 = vec1.xCoord - vec2.xCoord;
        double d2 = vec1.zCoord - vec2.zCoord;
        return MathHelper.sqrt_double(d0 * d0 + d2 * d2);
    }

    private boolean isBobberNearParticles(EntityFishHook bobber) {
        return particles.stream().anyMatch(v -> (getHorizontalDistance(bobber.getPositionVector(), v.position) < 0.2D));
    }

    private static class ParticleEntry {
        public Vec3 position;

        public long timeAdded;

        public ParticleEntry(Vec3 position, long timeAdded) {
            this.position = position;
            this.timeAdded = timeAdded;
        }
    }
}
