package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FillChestWithSaplingMacro implements IModuleActive {

    private static FillChestWithSaplingMacro instance;

    public static FillChestWithSaplingMacro getInstance() {
        if (instance == null) {
            instance = new FillChestWithSaplingMacro();
        }
        return instance;
    }

    private boolean enabled = false;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public String getName() {
        return "Fill Chest With Sapling Macro";
    }

    public enum States {
        IDLE,
        ABIPHONE,
        BUILDER,
        OPEN_CHEST,
        FILL_CHEST
    }

    private static States state = States.IDLE;
    private static final Clock waitTimer = new Clock();

    @Override
    public void onEnable() {
        state = States.IDLE;
        waitTimer.reset();
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!waitTimer.passed()) return;

        switch (state) {
            case IDLE:
                if (InventoryUtils.hasItemInInventory(getSaplingName())) {
                    state = States.OPEN_CHEST;
                    waitTimer.schedule(300);
                    return;
                }

                if (InventoryUtils.closeScreen()) {
                    return;
                }

                if (InventoryUtils.getSlotIdOfItemInHotbar("Abiphone") == -1) {
                    LogUtils.error("[" + getName() + "] You need an Abiphone to use this macro!");
                    onDisable();
                    return;
                }

                if (mc.thePlayer.inventory.getCurrentItem() == null || !mc.thePlayer.inventory.getCurrentItem().getDisplayName().contains("Abiphone")) {
                    mc.thePlayer.inventory.currentItem = InventoryUtils.getSlotIdOfItemInHotbar("Abiphone");
                } else {
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());
                    state = States.ABIPHONE;
                }
                waitTimer.schedule(300);
                break;
            case ABIPHONE:
                if (mc.currentScreen == null) return;

                Slot builder = InventoryUtils.getSlotOfItemInContainer("Builder");

                if (builder != null) {
                    InventoryUtils.clickContainerSlot(builder.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    state = States.BUILDER;
                    waitTimer.schedule(80);
                    return;
                }

                if (InventoryUtils.getInventoryName() != null && !InventoryUtils.getInventoryName().contains("Builder")) {
                    mc.thePlayer.closeScreen();
                    waitTimer.schedule(80);
                    LogUtils.error("[" + getName() + "] You need a Builder's contact to use this macro!");
                    onDisable();
                    return;
                }
                break;
            case BUILDER:
                if (mc.currentScreen == null) return;

                if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Builder")) {
                    Slot greenThumb = InventoryUtils.getSlotOfItemInContainer("Green Thumb");
                    if (greenThumb != null) {
                        InventoryUtils.clickContainerSlot(greenThumb.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        waitTimer.schedule(80);
                        return;
                    }
                    return;
                }

                if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Green Thumb")) {
                    Slot sapling = InventoryUtils.getSlotOfItemInContainer(getSaplingName());
                    if (sapling != null) {
                        InventoryUtils.clickContainerSlot(sapling.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        waitTimer.schedule(80);
                        return;
                    }
                    return;
                }

                if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Shop Trading Options")) {
                    if (InventoryUtils.hasFreeSlots()) {
                        Slot sapling = InventoryUtils.getSlotOfItemInContainer(getSaplingName(), 64);
                        if (sapling != null) {
                            InventoryUtils.clickContainerSlot(sapling.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                            waitTimer.schedule(300);
                            return;
                        }
                    } else {
                        InventoryUtils.closeScreen();
                        waitTimer.schedule(300);
                        state = States.OPEN_CHEST;
                        return;
                    }
                }
                break;
            case OPEN_CHEST:
                if (InventoryUtils.closeScreen()) {
                    waitTimer.schedule(300);
                    return;
                }

                MovingObjectPosition objectMouseOver = mc.objectMouseOver;

                if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().equals(Blocks.chest)) {
                    LogUtils.error("[" + getName() + "] You need to be looking at a chest to use this macro!");
                    onDisable();
                    return;
                }

                KeyBindUtils.rightClick();
                state = States.FILL_CHEST;
                waitTimer.reset();
                break;
            case FILL_CHEST:
                if (mc.currentScreen == null) return;

                if (InventoryUtils.hasItemInInventory(getSaplingName()) && InventoryUtils.hasFreeSlotsInContainer()) {
                    InventoryUtils.moveEveryItemToContainer(getSaplingName());
                    waitTimer.reset();
                } else if (InventoryUtils.hasItemInInventory(getSaplingName()) && !InventoryUtils.hasFreeSlotsInContainer()) {
                    mc.thePlayer.closeScreen();
                    onDisable();
                    LogUtils.warn("[" + getName() + "] The chest is full!");
                } else {
                    mc.thePlayer.closeScreen();
                    waitTimer.reset();
                    LogUtils.info("[" + getName() + "] Every sapling has been put into the chest");
                    state = States.IDLE;
                }
                break;
        }
    }

    private static String getSaplingName() {
        switch (MayOBeesConfig.fillChestSaplingType) {
            case 0:
                return "Spruce Sapling";
            case 1:
                return "Jungle Sapling";
            case 2:
                return "Dark Oak Sapling";
            default:
                throw new IllegalStateException("Unexpected value: " + MayOBeesConfig.fillChestSaplingType);
        }
    }
}
