package com.github.may2beez.mayobees.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

public class HeadUtils {
    public static final String FAIRY_SOUL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjk2OTIzYWQyNDczMTAwMDdmNmFlNWQzMjZkODQ3YWQ1Mzg2NGNmMTZjMzU2NWExODFkYzhlNmIyMGJlMjM4NyJ9fX0=";
    public static final String[] GIFT_TEXTURES = new String[]{
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBmNTM5ODUxMGIxYTA1YWZjNWIyMDFlYWQ4YmZjNTgzZTU3ZDcyMDJmNTE5M2IwYjc2MWZjYmQwYWUyIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ5N2Y0ZjQ0ZTc5NmY3OWNhNDMw0TdmYWE3YjRmZTkxYzQ0NWM3NmU1YzI2YTVhZDc5NGY1ZTQ3OTgzNyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjczYTIxMTQxMzZiOGVlNDkyNmNhYTUxNzg1NDE0MD2M2YTJiNzZlNGYxNjY4Y2I4OWQ5OTcxNmM0MjEifX19"
    };

    public static boolean isArmorStandWithSkull(Entity entity) {
        if (!(entity instanceof EntityArmorStand))
            return false;
        return isArmorStandWithSkull((EntityArmorStand) entity);
    }

    public static boolean isArmorStandWithSkull(EntityArmorStand armorStand) {
        ItemStack helmet = armorStand.getEquipmentInSlot(4);
        return helmet != null && helmet.getTagCompound() != null;
    }

    public static boolean isGift(EntityArmorStand armorStand) {
        if (!isArmorStandWithSkull(armorStand)) return false;
        ItemStack helmet = armorStand.getEquipmentInSlot(4);
        return !armorStand.hasCustomName()
                && Math.abs(armorStand.lastTickPosY - armorStand.posY) < 0.1
                && (helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[0])
                || helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[1])
                || helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[2]));
    }

    public static boolean isPersonalGift(EntityArmorStand armorStand) {
        return armorStand.hasCustomName() &&
              StringUtils.stripControlCodes(armorStand.getCustomNameTag()).contains("CLICK TO OPEN");
    }
}
