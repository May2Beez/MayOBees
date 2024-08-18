package com.github.may2beez.mayobees.module.impl.combat;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.*;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class MobAura implements IModuleActive {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static MobAura instance;

    public static MobAura getInstance() {
        if (instance == null) {
            instance = new MobAura();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Mob Aura";
    }

    @Getter
    private boolean enabled = false;

    private Optional<EntityLivingBase> currentTarget = Optional.empty();
    private final CopyOnWriteArrayList<EntityLivingBase> possibleTargets = new CopyOnWriteArrayList<>();

    private final List<Tuple<Entity, Long>> hitTargets = new ArrayList<>();

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public void onEnable() {
        nextRotationSpeed = MayOBeesConfig.getRandomizedMobAuraRotationSpeed();
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
        currentTarget = Optional.empty();
        hitTargets.clear();
        possibleTargets.clear();
        lastHit = 0;
        currentRandomDelay = MayOBeesConfig.getRandomizedMobAuraCooldown();
        if (!MayOBeesConfig.mobAuraRotationType)
            RotationHandler.getInstance().easeBackFromServerRotation();
        else
            RotationHandler.getInstance().reset();
        if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, false);
        }
    }

    private long lastHit = 0;
    private long currentRandomDelay = MayOBeesConfig.getRandomizedMobAuraCooldown();
    private long nextRotationSpeed = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (!enabled) return;
        hitTargets.removeIf(tuple -> System.currentTimeMillis() - tuple.getSecond() > 1000);
        if (currentTarget.isPresent() && currentTarget.get().isDead) {
            currentTarget = Optional.empty();
            resetAttack();
            return;
        }
        List<String> playerOnTab = TablistUtils.getTabListPlayersSkyblock();
        List<EntityLivingBase> tempPossibleTarget = mc.theWorld.getEntities(EntityLivingBase.class, entity ->
                entity != mc.thePlayer &&
                        (MayOBeesConfig.mobAuraUnlimitedRange || mc.thePlayer.getDistanceToEntity(entity) < MayOBeesConfig.mobAuraRange) &&
                        (MayOBeesConfig.mobAuraUnlimitedRange || Math.abs(mc.thePlayer.getPositionVector().yCoord - entity.getPositionVector().yCoord) <= MayOBeesConfig.mobAuraYDifference) &&
                        EntityUtils.isEntityInFOV(entity, MayOBeesConfig.mobAuraFOV) &&
                        this.isValidTarget(entity) &&
                        !EntityUtils.isNPC(entity) &&
                        !EntityUtils.isPlayer(entity, playerOnTab) &&
                        hitTargets.stream().noneMatch(tuple -> tuple.getFirst() == entity));
        possibleTargets.clear();
        possibleTargets.addAll(tempPossibleTarget);

        Rotation startRotation;
        if (!MayOBeesConfig.mobAuraRotationType && RotationHandler.getInstance().isRotating()) {
            startRotation = new Rotation(RotationHandler.getInstance().getServerSideYaw(), RotationHandler.getInstance().getServerSidePitch());
        } else {
            startRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        if (!currentTarget.isPresent()) {
            if (possibleTargets.size() == 1) {
                currentTarget = Optional.of(possibleTargets.get(0));
            } else if (possibleTargets.size() > 1) {
                currentTarget = possibleTargets.stream().min((entity1, entity2) -> {
                    Rotation rot1 = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(entity1));
                    Rotation rot2 = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(entity2));
                    return Float.compare(Math.abs(rot1.getYaw()) + Math.abs(rot1.getPitch()), Math.abs(rot2.getYaw()) + Math.abs(rot2.getPitch()));
                });
            }
            if (!currentTarget.isPresent()) {
                if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, false);
                }
                return;
            }
        }

        if (currentTarget.get().isDead || (!MayOBeesConfig.mobAuraUnlimitedRange && currentTarget.get().getDistanceToEntity(mc.thePlayer) > MayOBeesConfig.mobAuraRange) || currentTarget.get().getHealth() <= 0 || !mc.thePlayer.canEntityBeSeen(currentTarget.get())) {
            currentTarget = Optional.empty();
            resetAttack();
            return;
        }

        int slot = InventoryUtils.getSlotIdOfItemInHotbar(MayOBeesConfig.mobAuraItemName);

        if (MayOBeesConfig.mobAuraItemName.isEmpty() || slot == -1 || slot != mc.thePlayer.inventory.currentItem) {
            currentTarget = Optional.empty();
            resetAttack();
            return;
        }

        if (System.currentTimeMillis() < lastHit + currentRandomDelay - nextRotationSpeed + 75) {
            return;
        }

        if (RotationHandler.getInstance().isRotating() && !RotationHandler.getInstance().getConfiguration().goingBackToClientSide()) {
            return;
        }
        Rotation neededChange = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(currentTarget.get()));
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(currentTarget.get()),
                        nextRotationSpeed,
                        MayOBeesConfig.mobAuraRotationType ? RotationConfiguration.RotationType.CLIENT : RotationConfiguration.RotationType.SERVER,
                        this::hitEntity
                ).bowRotation(!MayOBeesConfig.mobAuraRotationMode).easeOutBack(Math.abs(neededChange.getYaw()) > 60).followTarget(true)
        );
    }

    private void resetAttack() {
        if (RotationHandler.getInstance().isRotating() && !RotationHandler.getInstance().getConfiguration().goingBackToClientSide()) {
            RotationHandler.getInstance().reset();
        }
        if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, false);
        }
    }

    private void hitEntity() {
        int slot = InventoryUtils.getSlotIdOfItemInHotbar(MayOBeesConfig.mobAuraItemName);
        if (slot == -1) {
            return;
        }
        if (MayOBeesConfig.mobAuraMouseButton == 0) {
            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                KeyBindUtils.leftClick();
            } else {
                if (currentTarget.isPresent() && mc.thePlayer.getDistanceToEntity(currentTarget.get()) <= 3) {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, currentTarget.get());
                }
            }
        } else if (MayOBeesConfig.mobAuraMouseButton == 1) {
            KeyBindUtils.rightClick();
        } else if (MayOBeesConfig.mobAuraMouseButton == 2) {
            if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem);
            }
        } else {
            throw new IllegalStateException("Invalid mouse button!");
        }
        if (!MayOBeesConfig.mobAuraAttackUntilDead) {
            currentTarget.ifPresent(entityLivingBase -> hitTargets.add(new Tuple<>(entityLivingBase, System.currentTimeMillis())));
            currentTarget = Optional.empty();
        }
        lastHit = System.currentTimeMillis();
        currentRandomDelay = MayOBeesConfig.getRandomizedMobAuraCooldown();
        nextRotationSpeed = MayOBeesConfig.getRandomizedMobAuraRotationSpeed();
        if (!MayOBeesConfig.mobAuraRotationType)
            RotationHandler.getInstance().easeBackFromServerRotation();
        else
            RotationHandler.getInstance().reset();
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity.isInvisible()) {
            return false;
        }
        if (entity instanceof EntityArmorStand) {
            return false;
        }
        if (!mc.thePlayer.canEntityBeSeen(entity)) {
            return false;
        }

        if (entity.getHealth() <= 0.0f) {
            return false;
        }

        if (!MayOBeesConfig.mobAuraAttackMobs && (entity instanceof EntityMob || entity instanceof EntityAmbientCreature || entity instanceof EntityWaterMob || entity instanceof EntityAnimal || entity instanceof EntitySlime)) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            return !EntityUtils.isTeam(entity);
        }

        if (entity.isDead) {
            return false;
        }

        return !(entity instanceof EntityVillager);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!enabled) return;
        if (currentTarget.isPresent()) {
            AxisAlignedBB bb = currentTarget.get().getEntityBoundingBox();
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.mobAuraCurrentTargetColor.toJavaColor());
        }

        for (Entity entity : possibleTargets) {
            if (currentTarget.isPresent() && entity == currentTarget.get()) continue;
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.mobAuraPossibleTargetColor.toJavaColor());
        }

        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 playerLeftLooking = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw - MayOBeesConfig.mobAuraFOV / 2f);
        Vec3 playerRightLooking = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw + MayOBeesConfig.mobAuraFOV / 2f);
        Vec3 playerLeftLookingEnd = playerPos.addVector(playerLeftLooking.xCoord * MayOBeesConfig.mobAuraRange, playerLeftLooking.yCoord * MayOBeesConfig.mobAuraRange, playerLeftLooking.zCoord * MayOBeesConfig.mobAuraRange);
        Vec3 playerRightLookingEnd = playerPos.addVector(playerRightLooking.xCoord * MayOBeesConfig.mobAuraRange, playerRightLooking.yCoord * MayOBeesConfig.mobAuraRange, playerRightLooking.zCoord * MayOBeesConfig.mobAuraRange);
        RenderUtils.drawTracer(new Vec3(0, 0, 0), playerLeftLookingEnd.addVector(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ), Color.WHITE);
        RenderUtils.drawTracer(new Vec3(0, 0, 0), playerRightLookingEnd.addVector(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ), Color.WHITE);
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        currentTarget = Optional.empty();
        possibleTargets.clear();
        if (enabled)
            onDisable();
    }
}
