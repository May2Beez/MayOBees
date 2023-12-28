package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.EntityUtils;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FillForagingSackMacro implements IModuleActive {
    private static FillForagingSackMacro instance;

    public static FillForagingSackMacro getInstance() {
        if (instance == null) {
            instance = new FillForagingSackMacro();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public String getName() {
        return "Fill Foraging Sack Macro";
    }

    @Override
    public void onEnable() {
        enabled = true;
        state = States.IDLE;
        waitTimer.reset();
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    enum States {
        IDLE,
        OPEN_BUILDER,
        OPEN_GREEN_THUMB_PAGE,
        CHOOSE_SAPLING,
        BUY_SAPLING,
        CLOSE_BUILDER,
        OPEN_SACK,
        FILL_SACK,
        CHECK_SACK,
        END
    }

    private static States state = States.IDLE;
    private static final Clock waitTimer = new Clock();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!waitTimer.passed()) return;

        switch (state) {
            case IDLE:
                Slot sackSlot = InventoryUtils.getSlotOfItemInHotbar("Foraging Sack");
                if (sackSlot == null) {
                    LogUtils.error("You don't have any foraging sack in your hotbar!");
                    onDisable();
                    return;
                }
                mc.thePlayer.inventory.currentItem = sackSlot.slotNumber;
                state = States.OPEN_BUILDER;
                waitTimer.schedule(500);
                break;
            case OPEN_BUILDER:
                MovingObjectPosition objectMouseOver = mc.objectMouseOver;
                if (objectMouseOver == null) {
                    LogUtils.error("You need to be looking at the builder to use this macro!");
                    onDisable();
                    return;
                }
                if (objectMouseOver.entityHit == null) {
                    LogUtils.error("You need to be looking at the builder to use this macro!");
                    onDisable();
                    return;
                }
                Entity entity = objectMouseOver.entityHit;
                Entity entity2 = EntityUtils.getEntityCuttingOtherEntity(entity, true);
                if (entity.getDisplayName().getUnformattedText().contains("Builder") || entity2 != null && entity2.getDisplayName().getUnformattedText().contains("Builder")) {
                    KeyBindUtils.leftClick();
                    state = States.OPEN_GREEN_THUMB_PAGE;
                    waitTimer.schedule(500);
                } else {
                    LogUtils.error("You need to be looking at the builder to use this macro!");
                    onDisable();
                    return;
                }
                break;
            case OPEN_GREEN_THUMB_PAGE:
                String invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Builder")) {
                    Slot greenThumb = InventoryUtils.getSlotOfItemInContainer("Green Thumb");
                    if (greenThumb == null) break;
                    InventoryUtils.clickContainerSlot(greenThumb.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    state = States.CHOOSE_SAPLING;
                    waitTimer.schedule(500);
                }
                break;
            case CHOOSE_SAPLING:
                invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Green Thumb")) {
                    Slot sapling = InventoryUtils.getSlotOfItemInContainer(MayOBeesConfig.getSaplingName());
                    if (sapling == null) break;
                    InventoryUtils.clickContainerSlot(sapling.slotNumber, InventoryUtils.ClickType.RIGHT, InventoryUtils.ClickMode.PICKUP);
                    state = States.BUY_SAPLING;
                    waitTimer.schedule(500);
                }
                break;
            case BUY_SAPLING:
                invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Shop Trading Options")) {
                    if (InventoryUtils.getFirstEmptySlotInInventory() == null) {
                        state = States.CLOSE_BUILDER;
                        waitTimer.schedule(500);
                        break;
                    }
                    Slot saplingStack = InventoryUtils.getSlotOfItemInContainer(MayOBeesConfig.getSaplingName(), 64);
                    if (saplingStack == null) break;
                    InventoryUtils.clickContainerSlot(saplingStack.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    waitTimer.schedule(150 + Math.random() * 75);
                }
                break;
            case CLOSE_BUILDER:
                if (InventoryUtils.closeScreen()) {
                    waitTimer.schedule(500);
                }
                state = States.OPEN_SACK;
                break;
            case OPEN_SACK:
                if (InventoryUtils.closeScreen()) {
                    waitTimer.schedule(500);
                    break;
                }
                Slot sack = InventoryUtils.getSlotOfItemInHotbar("Foraging Sack");
                if (sack == null) {
                    LogUtils.error("You don't have any foraging sack in your hotbar!");
                    onDisable();
                    return;
                }
                if (mc.thePlayer.inventory.currentItem != sack.slotNumber) {
                    mc.thePlayer.inventory.currentItem = sack.slotNumber;
                    waitTimer.schedule(500);
                    break;
                }
                KeyBindUtils.rightClick();
                state = States.FILL_SACK;
                break;
            case FILL_SACK:
                invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Foraging Sack")) {
                    Slot insertInventory = InventoryUtils.getSlotOfItemInContainer("Insert inventory");
                    if (insertInventory == null) break;
                    InventoryUtils.clickContainerSlot(insertInventory.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    state = States.CHECK_SACK;
                    waitTimer.schedule(500);
                }
                break;
            case CHECK_SACK:
                invName = InventoryUtils.getInventoryName();
                if (invName == null) return;
                if (invName.contains("Foraging Sack")) {
                    Slot insertInventory = InventoryUtils.getSlotOfItemInContainer("Insert inventory");
                    if (insertInventory == null) {
                        state = States.OPEN_BUILDER;
                        waitTimer.schedule(500);
                        InventoryUtils.closeScreen();
                        break;
                    } else {
                        state = States.END;
                        return;
                    }
                }
                break;
            case END:
                if (InventoryUtils.closeScreen()) {
                    waitTimer.schedule(500);
                    break;
                }
                LogUtils.info("[" + getName() + "] Sack is full!");
                onDisable();
                break;
        }
    }

}
