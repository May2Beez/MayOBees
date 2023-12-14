package com.github.may2beez.mayobees.module.impl.render;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.github.may2beez.mayobees.MayOBees;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.HeadUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ESP implements IModule {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final HashMap<String, List<Location>> clickedFairySouls = new HashMap<>();
    private final CopyOnWriteArrayList<EntityArmorStand> visibleFairySouls = new CopyOnWriteArrayList<>();
    private final List<BlockPos> clickedGifts = new ArrayList<>();
    private final File clickedFairySoulsFile = new File(mc.mcDataDir + "/config/mayobees/clickedFairySouls.json");
    private static ESP instance;

    public static ESP getInstance() {
        if (instance == null) {
            instance = new ESP();
        }
        return instance;
    }

    public ESP() {
        try {
            if (!clickedFairySoulsFile.getParentFile().exists()) {
                clickedFairySoulsFile.getParentFile().mkdirs();
            }
            if (!clickedFairySoulsFile.exists()) {
                clickedFairySoulsFile.createNewFile();
                // fill it with empty array
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(clickedFairySoulsFile);
                    String json = MayOBees.GSON.toJson(clickedFairySouls);
                    fileOutputStream.write(json.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (fileOutputStream != null) try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(clickedFairySoulsFile);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            String json = new String(bytes);
            if (json.isEmpty()) return;
            Type type = new TypeToken<HashMap<String, List<Location>>>() {
            }.getType();
            try {
                HashMap<String, List<Location>> locations = MayOBees.GSON.fromJson(json, type);
                if (locations == null) return;
                clickedFairySouls.putAll(locations);
            } catch (Exception e) {
                e.printStackTrace();
                saveClickedFairySouls();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileInputStream != null) try {
                fileInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveClickedFairySouls() {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(clickedFairySoulsFile);
            String json = MayOBees.GSON.toJson(clickedFairySouls);
            fileOutputStream.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileOutputStream != null) try {
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void resetClickedFairySouls() {
        clickedFairySouls.clear();
        saveClickedFairySouls();
    }

    public void resetClickedFairySoulsOnlyCurrentIsland() {
        clickedFairySouls.remove(GameStateHandler.getInstance().getLocation().getName());
        saveClickedFairySouls();
    }

    public void addAllVisibleFairySoulsToClickedList() {
        List<Location> thisLocationFairySouls = clickedFairySouls.computeIfAbsent(GameStateHandler.getInstance().getLocation().getName(), k -> new ArrayList<>());
        for (EntityArmorStand entityArmorStand : visibleFairySouls) {
            if (listHasElement(thisLocationFairySouls, Location.of(entityArmorStand.getPosition()))) continue;
            thisLocationFairySouls.add(Location.of(entityArmorStand.getPosition()));
        }
        clickedFairySouls.put(GameStateHandler.getInstance().getLocation().getName(), thisLocationFairySouls);
        saveClickedFairySouls();
    }

    public void resetClickedGifts() {
        clickedGifts.clear();
    }

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.chestESP || MayOBeesConfig.fairySoulESP || MayOBeesConfig.giftESP;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        if (MayOBeesConfig.chestESP) {
            for (TileEntity tileEntityChest : mc.theWorld.loadedTileEntityList.stream().filter(tileEntity -> tileEntity instanceof TileEntityChest).collect(Collectors.toList())) {
                Block block = mc.theWorld.getBlockState(tileEntityChest.getPos()).getBlock();
                block.setBlockBoundsBasedOnState(mc.theWorld, tileEntityChest.getPos());
                AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, tileEntityChest.getPos()).expand(0.002, 0.002, 0.002).offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
                RenderUtils.drawBox(bb, MayOBeesConfig.chestESPColor.toJavaColor());
                if (MayOBeesConfig.chestESPTracers) {
                    RenderUtils.drawTracer(new Vec3(tileEntityChest.getPos().getX() + 0.5, tileEntityChest.getPos().getY() + 0.5, tileEntityChest.getPos().getZ() + 0.5), MayOBeesConfig.chestESPColor.toJavaColor());
                }
            }
        }

        AxisAlignedBB closestFairySoulBb = null;

        List<EntityArmorStand> visibleFairySouls = new ArrayList<>();
        for (EntityArmorStand entityArmorStand : mc.theWorld.loadedEntityList.stream().filter(entity -> entity instanceof EntityArmorStand).map(entity -> (EntityArmorStand) entity).collect(Collectors.toList())) {
            ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
            if (helmet == null || !helmet.hasTagCompound()) continue;

            if (MayOBeesConfig.fairySoulESP) {
                closestFairySoulBb = fairySoulEntityCheck(entityArmorStand, closestFairySoulBb, visibleFairySouls);
            }

            if (MayOBeesConfig.giftESP && (!MayOBeesConfig.giftESPShowOnlyOnJerryWorkshop || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)) {
                giftEntityCheck(entityArmorStand);
            }
        }
        this.visibleFairySouls.clear();
        this.visibleFairySouls.addAll(visibleFairySouls);

        drawClosest(closestFairySoulBb, MayOBeesConfig.fairySoulESPColor, MayOBeesConfig.fairySoulESPTracers, MayOBeesConfig.fairySoulESPShowDistance);
    }

    private void drawClosest(AxisAlignedBB closestFairySoulBb, OneColor fairySoulESPColor, boolean fairySoulESPTracers, boolean fairySoulESPShowDistance) {
        if (closestFairySoulBb != null) {
            RenderUtils.drawBox(closestFairySoulBb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ), fairySoulESPColor.toJavaColor());
            double x = closestFairySoulBb.minX + (closestFairySoulBb.maxX - closestFairySoulBb.minX) / 2;
            double y = closestFairySoulBb.minY + (closestFairySoulBb.maxY - closestFairySoulBb.minY) / 2;
            double z = closestFairySoulBb.minZ + (closestFairySoulBb.maxZ - closestFairySoulBb.minZ) / 2;
            if (fairySoulESPTracers) {
                RenderUtils.drawTracer(new Vec3(x, y, z), fairySoulESPColor.toJavaColor());
            }
            if (fairySoulESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(closestFairySoulBb.minX + 0.5, closestFairySoulBb.minY + 0.5, closestFairySoulBb.minZ + 0.5)));
                RenderUtils.drawText(String.format("%.2fm", distance), x, y, z, 1);
            }
        }
    }

    private AxisAlignedBB fairySoulEntityCheck(EntityArmorStand entityArmorStand, AxisAlignedBB closestBb, List<EntityArmorStand> visibleFairySouls) {
        ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return closestBb;
        if (!helmet.getTagCompound().toString().contains(HeadUtils.FAIRY_SOUL_TEXTURE)) return closestBb;

        List<Location> fairySoulsFromThisLocation = clickedFairySouls.get(GameStateHandler.getInstance().getLocation().getName());

        if (fairySoulsFromThisLocation != null && listHasElement(fairySoulsFromThisLocation, Location.of(entityArmorStand.getPosition()))) return closestBb;

        visibleFairySouls.add(entityArmorStand);

        AxisAlignedBB bb = new AxisAlignedBB(entityArmorStand.posX - 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() - 0.5, entityArmorStand.posZ - 0.5, entityArmorStand.posX + 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ + 0.5).expand(0.002, 0.002, 0.002);
        if (MayOBeesConfig.fairySoulESPShowOnlyClosest) {
            if (closestBb == null) {
                closestBb = bb;
            } else {
                if (mc.thePlayer.getDistanceSqToCenter(entityArmorStand.getPosition()) < mc.thePlayer.getDistanceSqToCenter(new BlockPos(closestBb.minX + 0.5, closestBb.minY + 0.5, closestBb.minZ + 0.5))) {
                    closestBb = bb;
                }
            }
        } else {
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.fairySoulESPColor.toJavaColor());
            if (MayOBeesConfig.fairySoulESPTracers) {
                RenderUtils.drawTracer(new Vec3(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ), MayOBeesConfig.fairySoulESPColor.toJavaColor());
            }
            if (MayOBeesConfig.fairySoulESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ)));
                RenderUtils.drawText(String.format("%.2fm", distance), entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ, 1);
            }
        }

        return closestBb;
    }

    private void giftEntityCheck(EntityArmorStand entityArmorStand) {
        ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return;
        if (helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[0]) || helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[1]) || helmet.getTagCompound().toString().contains(HeadUtils.GIFT_TEXTURES[2])) {
            if (clickedGifts.contains(entityArmorStand.getPosition())) return;
            RenderUtils.drawHeadBox(entityArmorStand, MayOBeesConfig.giftESPColor.toJavaColor());
            if (MayOBeesConfig.giftESPTracers) {
                RenderUtils.drawTracer(new Vec3(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ), MayOBeesConfig.giftESPColor.toJavaColor());
            }
            if (MayOBeesConfig.giftESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ)));
                RenderUtils.drawText(String.format("%.2fm", distance), entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ, 1);
            }
        }
    }

    @SubscribeEvent
    public void onLeftClick(ClickEvent.Left event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entity == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        fairySoulClick(event);
    }

    @SubscribeEvent
    public void onRightClick(ClickEvent.Right event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entity == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        fairySoulClick(event);
        giftClick(event);
    }


    private void fairySoulClick(ClickEvent event) {
        if (!MayOBeesConfig.fairySoulESP) return;
        if (!(event.entity instanceof EntityArmorStand)) return;
        ItemStack helmet = ((EntityArmorStand) event.entity).getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return;
        if (helmet.getTagCompound().toString().contains(HeadUtils.FAIRY_SOUL_TEXTURE)) {
            List<Location> thisLocationFairySouls = clickedFairySouls.get(GameStateHandler.getInstance().getLocation().getName());
            if (thisLocationFairySouls == null || thisLocationFairySouls.isEmpty()) {
                clickedFairySouls.put(GameStateHandler.getInstance().getLocation().getName(), new ArrayList<>(Collections.singletonList(Location.of(event.entity.getPosition()))));
                saveClickedFairySouls();
                return;
            }
            if (listHasElement(thisLocationFairySouls, Location.of(event.entity.getPosition()))) return;
            clickedFairySouls.get(GameStateHandler.getInstance().getLocation().getName()).add(Location.of(event.entity.getPosition()));
            saveClickedFairySouls();
        }
    }

    private void giftClick(ClickEvent event) {
        if (!MayOBeesConfig.giftESP) return;
        if (!HeadUtils.isGift(event.entity)) return;
        if (clickedGifts.contains(event.entity.getPosition())) return;
        clickedGifts.add(event.entity.getPosition());
    }

    private boolean listHasElement(List<Location> list, Location location) {
        for (Location location1 : list) {
            if (location1.x == location.x && location1.y == location.y && location1.z == location.z) {
                return true;
            }
        }
        return false;
    }


    private static class Location {
        public final int x;
        public final int y;
        public final int z;

        public Location(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static Location of(BlockPos blockPos) {
            return new Location(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }
}
