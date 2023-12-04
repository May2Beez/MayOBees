package com.github.may2beez.mayobees.util;

import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.util.helper.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntityUtils {
    private final static Minecraft mc = Minecraft.getMinecraft();
    public static boolean isPlayer(Entity entity, List<String> playerList) {
        if (!(entity instanceof EntityOtherPlayerMP)) {
            return false;
        }
        return playerList.stream().anyMatch(player -> player.toLowerCase().contains(entity.getName().toLowerCase()));
    }

    public static boolean isNPC(Entity entity) {
        if (!(entity instanceof EntityOtherPlayerMP)) {
            return false;
        }
        EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
        if (StringUtils.stripControlCodes(entityLivingBase.getCustomNameTag()).startsWith("[NPC]")) {
            return true;
        }
        return entity.getUniqueID().version() == 2 && entityLivingBase.getHealth() == 20 && entityLivingBase.getMaxHealth() == 20;
    }

    private static boolean isOnTeam(EntityPlayer player) {
        for (Score score : Minecraft.getMinecraft().thePlayer.getWorldScoreboard().getScores()) {
            if (score.getObjective().getName().equals("health") && score.getPlayerName().contains(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTeam(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.getDisplayName().getUnformattedText().length() < 4) {
            return false;
        }

        return isOnTeam((EntityPlayer) entity);
    }

    public static boolean isEntityInFOV(Entity entity, double fov) {
        Rotation rotation = RotationHandler.getInstance().getRotation(entity);
        Rotation neededRotation = RotationHandler.getInstance().getNeededChange(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), rotation);
        return Math.abs(neededRotation.getYaw()) <= fov / 2f;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, boolean armorStand) {
        List<Entity> possible = mc.theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(mc.thePlayer));
            boolean flag2 = armorStand == (a instanceof EntityArmorStand);
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag4 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            return flag1 && flag2 && flag3 && flag4;
        });
        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
        return null;
    }

    public static boolean isLookingAtEntity(Entity entity, double distance, Vec3 lookDirection) {
        List<Entity> entities = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().addCoord(lookDirection.xCoord * distance, lookDirection.yCoord * distance, lookDirection.zCoord * distance).expand(1, 1, 1), entity1 -> entity1 instanceof EntityLivingBase && entity1 != mc.thePlayer);
        if (entities.isEmpty()) {
            return false;
        }
        for (Entity entity1 : entities) {
            if (entity1.equals(entity)) {
                return true;
            }
        }
        return false;
    }
}
