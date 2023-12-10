package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.HeadUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GiftAura implements IModule {
    private static GiftAura instance;
    public static GiftAura getInstance() {
        if (instance == null) {
            instance = new GiftAura();
        }
        return instance;
    }
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<EntityArmorStand> openedGifts = new ArrayList<>();
    private final Clock delay = new Clock();

    @Override
    public boolean isRunning() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return false;
        if (!MayOBeesConfig.giftAura)
            return false;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)
            return true;
        return GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.JERRY_WORKSHOP
                && MayOBeesConfig.giftAuraOpenGiftsOutsideOfJerryWorkshop;
    }

    public void reset() {
        openedGifts.clear();
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (mc.theWorld == null) return;
        List<Entity> loadedEntities = mc.theWorld.loadedEntityList;
        for (Entity entity : loadedEntities) {
            if (entity instanceof EntityArmorStand && shouldOpenGift((EntityArmorStand) entity) && !openedGifts.contains(entity)) {
                RenderUtils.drawHeadBox(entity, MayOBeesConfig.giftAuraESPColor.toJavaColor());
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.currentScreen != null) return;
        if (RotationHandler.getInstance().isRotating()) return;
        if (delay.isScheduled() && !delay.passed()) return;

        List<Entity> loadedEntities = mc.theWorld.loadedEntityList;
        loadedEntities.stream()
                .filter(EntityArmorStand.class::isInstance)
                .map(EntityArmorStand.class::cast)
                .sorted(Comparator.comparingDouble(mc.thePlayer::getDistanceToEntity))
                .filter(entity -> shouldOpenGift(entity) && !openedGifts.contains(entity) && mc.thePlayer.getDistanceToEntity(entity) <= 3.5f)
                .findFirst()
                .ifPresent(entity -> {
                    if (mc.thePlayer.canEntityBeSeen(entity) || MayOBeesConfig.giftAuraDontCheckForVisibility) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity), 400L,
                                MayOBeesConfig.giftAuraRotationType ? RotationConfiguration.RotationType.CLIENT : RotationConfiguration.RotationType.SERVER,
                                () -> this.tryToOpenGift(entity)));
                    }
                });
        delay.schedule(MayOBeesConfig.giftAuraDelay);
    }

    private void tryToOpenGift(EntityArmorStand entity) {
        if (mc.thePlayer.canEntityBeSeen(entity) || MayOBeesConfig.giftAuraDontCheckForVisibility) {
            mc.playerController.interactWithEntitySendPacket(mc.thePlayer, entity);
            openedGifts.add(entity);
            LogUtils.debug("Gift opened!");
            if (!MayOBeesConfig.giftAuraRotationType)
                RotationHandler.getInstance().easeBackFromServerRotation();
            else
                RotationHandler.getInstance().reset();
        }
    }

    private boolean shouldOpenGift(EntityArmorStand entity) {
        return (MayOBeesConfig.giftAuraOpenDefaultGiftsAtJerryWorkshop && HeadUtils.isGift(entity))
                || (MayOBeesConfig.giftAuraOpenPlayerGifts && HeadUtils.isPersonalGift(entity));
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        openedGifts.clear();
    }
}
