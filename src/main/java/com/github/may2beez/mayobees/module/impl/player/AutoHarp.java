package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.GuiClosedEvent;
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
import net.minecraftforge.client.event.GuiOpenEvent;
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
    private long lastPacketReceiveTime = 0;
    private final int lagSpikeThreshold = 100;
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
        enabled = true;
        hasGuiUpdated = false;
        slots = new ItemStack[7];
    }

    @Override
    public void onDisable() {
        enabled = false;
        hasGuiUpdated = false;
        slots = new ItemStack[7];
    }

    @SubscribeEvent
    public void onGuiOpenEvent(GuiScreenEvent.InitGuiEvent event) {
        if (!MayOBeesConfig.autoHarp || this.isRunning() || !(event.gui instanceof GuiChest)) return;
        String inventoryName = InventoryUtils.getInventoryName(((GuiChest) event.gui).inventorySlots);
        if (inventoryName != null && inventoryName.startsWith("Harp - ")) {
            this.onEnable();
            LogUtils.info("Harp Enabled");
        }
    }

    @SubscribeEvent
    public void onGuiClose(GuiClosedEvent event) {
        if (!this.isRunning()) return;
        this.onDisable();
        LogUtils.info("Harp Gui Closed");
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!isRunning())
            return;

        long currentTime = System.currentTimeMillis();
        boolean time = currentTime - lastPacketReceiveTime > lagSpikeThreshold;

        if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            int slotIndex = packet.func_149173_d();
            if (slotIndex > 43)
                return;
            hasGuiUpdated = false;
            if (slotIndex < 37)
                return;
            slots[slotIndex - 37] = packet.func_149174_e();
            return;
        }

        if (time && !hasGuiUpdated) {
            for (int i = 0; i < 7; i++) {
                if (slots[i] != null && Block.getBlockFromItem(slots[i].getItem()) == Blocks.quartz_block) {
                    InventoryUtils.clickContainerSlot(i + 37, InventoryUtils.ClickType.MIDDLE,
                            InventoryUtils.ClickMode.CLONE);
                    break;
                }
            }
        }

        lastPacketReceiveTime = currentTime;

        if (hasGuiUpdated)
            return;
        hasGuiUpdated = true;
        for (int i = 0; i < 7; i++) {
            if (slots[i] != null && Block.getBlockFromItem(slots[i].getItem()) == Blocks.quartz_block) {
                InventoryUtils.clickContainerSlot(i + 37, InventoryUtils.ClickType.MIDDLE,
                        InventoryUtils.ClickMode.CLONE);
                break;
            }
        }
    }
}
