package com.github.may2beez.mayobees.module.impl.dungeon;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class AutoTrapDefuser implements IModule {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoTrapDefuser instance;

    public static AutoTrapDefuser getInstance() {
        if (instance == null) {
            instance = new AutoTrapDefuser();
        }
        return instance;
    }

    private final Clock delay = new Clock();

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.autoTrapDefuser;
    }

    @Override
    public String getName() {
        return "Auto Trap Defuser";
    }

    private int previousSlot = -1;
    private boolean shouldDefuse = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!delay.passed()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.DUNGEON) return;
        ItemStack currentStack = mc.thePlayer.getHeldItem();
        if (shouldDefuse) {
            if (currentStack == null || !currentStack.getItem().equals(Items.shears)) {
                previousSlot = -1;
                shouldDefuse = false;
            }
            return;
        }
        MovingObjectPosition objectMouseOver = mc.objectMouseOver;

        if (currentStack != null && currentStack.getItem().equals(Items.shears)) return;
        if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        Block block = mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock();
        if (!block.equals(Blocks.tripwire)) return;

        boolean isPlacedOnFloor = mc.theWorld.getBlockState(objectMouseOver.getBlockPos().down()).getBlock().isBlockSolid(mc.theWorld, objectMouseOver.getBlockPos().down(), null);
        if (!isPlacedOnFloor) return;

        if (previousSlot == -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
            int defuseKitSlot = InventoryUtils.getSlotIdOfItemInHotbar("Defuse Kit");
            if (defuseKitSlot != -1) {
                mc.thePlayer.inventory.currentItem = defuseKitSlot;
            } else {
                LogUtils.error("[Auto Trap Defuser] No defuse kit found in hotbar!");
                MayOBeesConfig.autoTrapDefuser = false;
                previousSlot = -1;
            }
            shouldDefuse = true;
            if (MayOBeesConfig.autoTrapDefuserAutoDefuse) {
                Multithreading.schedule(() -> {
                    KeyBindUtils.leftClick();
                    Multithreading.schedule(() -> {
                        if (previousSlot != -1) {
                            mc.thePlayer.inventory.currentItem = previousSlot;
                            previousSlot = -1;
                            delay.schedule(2_000);
                        }
                    }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
                }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
            }
        }
    }

    @SubscribeEvent
    public void onLeftClick(ClickEvent.Left event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (MayOBeesConfig.autoTrapDefuserAutoDefuse) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.DUNGEON) return;
        if (!shouldDefuse) return;
        ItemStack currentStack = mc.thePlayer.getHeldItem();
        if (currentStack != null && currentStack.getItem().equals(Items.shears)) {
            Multithreading.schedule(() -> {
                if (previousSlot != -1) {
                    mc.thePlayer.inventory.currentItem = previousSlot;
                    previousSlot = -1;
                    delay.schedule(2_000);
                }
            }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
        }
    }
}