package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.UngrabUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

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
        SACK,
        ABIPHONE,
        BUILDER,
        GET_FREE_HAND,
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
        if (MayOBeesConfig.mouseUngrab)
            UngrabUtils.ungrabMouse();
    }

    @Override
    public void onDisable() {
        enabled = false;
        UngrabUtils.regrabMouse();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!waitTimer.passed()) return;

        switch (state) {
            case IDLE:
                if (InventoryUtils.hasItemInInventory(MayOBeesConfig.getSaplingName())) {
                    state = States.GET_FREE_HAND;
                    waitTimer.schedule(300);
                    return;
                }

                if (InventoryUtils.closeScreen()) {
                    return;
                }

                if (InventoryUtils.getSlotIdOfItemInHotbar("Abiphone") != -1) {
                    if (mc.thePlayer.inventory.getCurrentItem() == null || !mc.thePlayer.inventory.getCurrentItem().getDisplayName().contains("Abiphone")) {
                        mc.thePlayer.inventory.currentItem = InventoryUtils.getSlotIdOfItemInHotbar("Abiphone");
                    } else {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());
                        state = States.ABIPHONE;
                    }
                    waitTimer.schedule(300);
                    break;
                }

                if (InventoryUtils.getSlotIdOfItemInHotbar("Foraging Sack") != -1) {
                    if (mc.thePlayer.inventory.getCurrentItem() == null || !mc.thePlayer.inventory.getCurrentItem().getDisplayName().contains("Foraging Sack")) {
                        mc.thePlayer.inventory.currentItem = InventoryUtils.getSlotIdOfItemInHotbar("Foraging Sack");
                    } else {
                        KeyBindUtils.rightClick();
                        state = States.SACK;
                    }
                    waitTimer.schedule(800);
                    break;
                }

                LogUtils.error("[" + getName() + "] You need an Abiphone or a Foraging Sack in your hotbar to use this macro!");
                onDisable();
                break;
            case SACK:
                String invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Foraging Sack")) {
                    Slot takeOut = InventoryUtils.getSlotOfItemInContainer(MayOBeesConfig.getSaplingName());
                    if (takeOut != null) {
                        List<String> lore = InventoryUtils.getItemLore(takeOut.getStack());
                        if (lore.stream().anyMatch(l -> l.contains("Empty sack!"))) {
                            LogUtils.error("[" + getName() + "] You need to have a full Foraging Sack to use this macro!");
                            onDisable();
                            return;
                        }
                        InventoryUtils.clickContainerSlot(takeOut.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        state = States.GET_FREE_HAND;
                        waitTimer.schedule(800);
                        return;
                    }
                }
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
                    Slot sapling = InventoryUtils.getSlotOfItemInContainer(MayOBeesConfig.getSaplingName());
                    if (sapling != null) {
                        InventoryUtils.clickContainerSlot(sapling.slotNumber, InventoryUtils.ClickType.RIGHT, InventoryUtils.ClickMode.PICKUP);
                        waitTimer.schedule(80);
                        return;
                    }
                    return;
                }

                if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Shop Trading Options")) {
                    if (InventoryUtils.getFirstEmptySlotInInventory() == null) {
                        state = States.GET_FREE_HAND;
                        waitTimer.schedule(500);
                        break;
                    }
                    Slot saplingStack = InventoryUtils.getSlotOfItemInContainer(MayOBeesConfig.getSaplingName(), 64);
                    if (saplingStack == null) break;
                    InventoryUtils.clickContainerSlot(saplingStack.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    waitTimer.schedule(150 + Math.random() * 75);
                }
                break;
            case GET_FREE_HAND:
                if (InventoryUtils.closeScreen()) {
                    waitTimer.schedule(300);
                    return;
                }
                getFreeHand();
                state = States.OPEN_CHEST;
                waitTimer.schedule(300);
                break;
            case OPEN_CHEST:
                MovingObjectPosition objectMouseOver = mc.objectMouseOver;

                if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().equals(Blocks.chest)) {
                    LogUtils.error("[" + getName() + "] You need to be looking at a chest to use this macro!");
                    onDisable();
                    return;
                }

                KeyBindUtils.rightClick();
                state = States.FILL_CHEST;
                waitTimer.schedule(500);
                break;
            case FILL_CHEST:
                invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (!invName.contains("Chest")) return;

                if (InventoryUtils.hasItemInInventory(MayOBeesConfig.getSaplingName()) && InventoryUtils.hasFreeSlotsInContainer()) {
                    InventoryUtils.moveEveryItemToContainer(MayOBeesConfig.getSaplingName());
                    waitTimer.schedule(500);
                } else if (InventoryUtils.hasItemInInventory(MayOBeesConfig.getSaplingName()) && !InventoryUtils.hasFreeSlotsInContainer()) {
                    mc.thePlayer.closeScreen();
                    onDisable();
                    LogUtils.warn("[" + getName() + "] The chest is full!");
                } else {
                    mc.thePlayer.closeScreen();
                    waitTimer.schedule(500);
                    LogUtils.info("[" + getName() + "] Every sapling has been put into the chest");
                    state = States.IDLE;
                }
                break;
        }
    }

    private void getFreeHand() {
        if (mc.thePlayer.inventory.getCurrentItem() == null) return;
        int freeSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            Item sapling = Item.getItemFromBlock(Blocks.sapling);
            if (itemStack == null || itemStack.getItem().equals(sapling)) {
                freeSlot = i;
                break;
            }
        }
        if (freeSlot == -1) {
            LogUtils.error("[" + getName() + "] You don't have any free slots in your hotbar!");
            onDisable();
            return;
        }
        mc.thePlayer.inventory.currentItem = freeSlot;
    }
}
