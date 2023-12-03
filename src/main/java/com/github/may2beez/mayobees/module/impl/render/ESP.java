package com.github.may2beez.mayobees.module.impl.render;

import com.github.may2beez.mayobees.MayOBees;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEntityEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.RenderUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ESP implements IModule {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<BlockPos> clickedFairySouls = new ArrayList<>();
    private final File clickedFairySoulsFile = new File(mc.mcDataDir + "/config/mayobees/clickedFairySouls.json");
    private static ESP instance;

    private final String FAIRY_SOUL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjk2OTIzYWQyNDczMTAwMDdmNmFlNWQzMjZkODQ3YWQ1Mzg2NGNmMTZjMzU2NWExODFkYzhlNmIyMGJlMjM4NyJ9fX0=";

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
                    String json = MayOBees.GSON.toJson(new BlockPos[0]);
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
            BlockPos[] locations = MayOBees.GSON.fromJson(json, BlockPos[].class);
            clickedFairySouls.addAll(Arrays.asList(locations));
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
            String json = MayOBees.GSON.toJson(clickedFairySouls.toArray(new BlockPos[0]));
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

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.chestESP || MayOBeesConfig.fairySoulESP;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

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

        if (MayOBeesConfig.fairySoulESP) {
            AxisAlignedBB closestBb = null;
            for (EntityArmorStand entityArmorStand : mc.theWorld.loadedEntityList.stream().filter(entity -> entity instanceof EntityArmorStand).map(entity -> (EntityArmorStand) entity).collect(Collectors.toList())) {
                ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
                if (helmet == null || !helmet.hasTagCompound()) continue;
                if (helmet.getTagCompound().toString().contains(FAIRY_SOUL_TEXTURE)) {
                    if (clickedFairySouls.contains(entityArmorStand.getPosition())) continue;
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
                }
            }
            if (closestBb != null) {
                RenderUtils.drawBox(closestBb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ), MayOBeesConfig.fairySoulESPColor.toJavaColor());
                double x = closestBb.minX + (closestBb.maxX - closestBb.minX) / 2;
                double y = closestBb.minY + (closestBb.maxY - closestBb.minY) / 2;
                double z = closestBb.minZ + (closestBb.maxZ - closestBb.minZ) / 2;
                if (MayOBeesConfig.fairySoulESPTracers) {
                    RenderUtils.drawTracer(new Vec3(x, y, z), MayOBeesConfig.fairySoulESPColor.toJavaColor());
                }
                if (MayOBeesConfig.fairySoulESPShowDistance) {
                    double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(closestBb.minX + 0.5, closestBb.minY + 0.5, closestBb.minZ + 0.5)));
                    RenderUtils.drawText(String.format("%.2fm", distance), x, y, z, 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickEntity(ClickEntityEvent.Right event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        if (MayOBeesConfig.fairySoulESP) {
            if (event.entity instanceof EntityArmorStand) {
                ItemStack helmet = ((EntityArmorStand) event.entity).getEquipmentInSlot(4);
                if (helmet == null || !helmet.hasTagCompound()) return;
                if (helmet.getTagCompound().toString().contains(FAIRY_SOUL_TEXTURE)) {
                    clickedFairySouls.add(event.entity.getPosition());
                    saveClickedFairySouls();
                }
            }
        }
    }
}
