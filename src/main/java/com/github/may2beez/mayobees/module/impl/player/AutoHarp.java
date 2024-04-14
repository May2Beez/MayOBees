package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoHarp implements IModuleActive {
    private static AutoHarp instance;

    public static AutoHarp getInstance() {
        if (instance == null) {
            instance = new AutoHarp();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean hasGuiUpdated = false;
    private ItemStack[] slots = new ItemStack[7];

    @Override
    public boolean isRunning() {
        return enabled && mc.currentScreen instanceof GuiChest;
    }

    @Override
    public String getName() {
        return "AutoHarp";
    }

    @Override
    public void onEnable() {
        hasGuiUpdated = false;
        slots = new ItemStack[7];
    }

    @Override
    public void onDisable() {
        onEnable();
    }

    @SubscribeEvent
    void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!MayOBeesConfig.autoHarpEnable) return;
        String inventoryName = InventoryUtils.getInventoryName();
        if (inventoryName != null && inventoryName.startsWith("Harp - ")) {
            enabled = true;
            onEnable();
            LogUtils.debug("Harp Enabled");
        }else{
            enabled = false;
        }
    }

    @SubscribeEvent
    void onPacketReceive(PacketEvent.Receive event) {
        if (!isRunning()) return;
        if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            int slotIndex = packet.func_149173_d();
            if (slotIndex > 43) return;
            hasGuiUpdated = false;
            if (slotIndex < 37) return;
            slots[slotIndex - 37] = packet.func_149174_e();
            return;
        }
        if (hasGuiUpdated) return;
        hasGuiUpdated = true;
        for (int i = 0; i < 7; i++) {
            if (slots[i] != null && Block.getBlockFromItem(slots[i].getItem()) == Blocks.quartz_block) {
                InventoryUtils.clickContainerSlot(i + 37, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.CLONE);
                break;
            }
        }
    }
}
