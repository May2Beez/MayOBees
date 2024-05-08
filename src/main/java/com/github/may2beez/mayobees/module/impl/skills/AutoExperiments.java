package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.GuiClosedEvent;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class AutoExperiments implements IModuleActive {
    private static AutoExperiments instance;

    public static AutoExperiments getInstance() {
        if (instance == null) instance = new AutoExperiments();
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Clock clickDelay = new Clock();
    private final Deque<Integer> clickQueue = new ArrayDeque<>();
    private boolean enabled = false;
    private boolean solving = false;
    private int experiment = -1;
    private String lastItemName = "";
    private boolean shouldCloseGui = false;

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        this.enabled = false;
        this.solving = false;
        this.experiment = -1;
        this.lastItemName = "";
        this.shouldCloseGui = false;
        this.clickQueue.clear();
    }

    @Override
    public boolean isRunning() {
        return this.enabled && MayOBeesConfig.autoExperiments;
    }

    @Override
    public String getName() {
        return "AutoExperiments";
    }

    // 0 = Chronomatron ; 1 = Ultrasequencer
    private void enable(final int experiment) {
        this.experiment = experiment;
        this.enabled = true;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent event) {
        if (!MayOBeesConfig.autoExperiments || this.isRunning() || !(event.gui instanceof GuiChest)) return;

        final String inventoryName = InventoryUtils.getInventoryName(((GuiChest) event.gui).inventorySlots);
        if (inventoryName.startsWith("Chronomatron (")) {
            this.enable(0);
        } else if (inventoryName.startsWith("Ultrasequencer (")) {
            this.enable(1);
        }
    }

    @SubscribeEvent
    public void guiCloseEvent(GuiClosedEvent event) {
        if (!this.isRunning()) return;
        this.onDisable();
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!this.isRunning()) return;
        if (!(event.packet instanceof S2FPacketSetSlot)) return;

        final S2FPacketSetSlot slot = (S2FPacketSetSlot) event.packet;
        final int slotIndex = slot.func_149173_d();
        final ItemStack slotStack = slot.func_149174_e();

        if (slotStack == null || !slotStack.hasDisplayName()) return;
        if (slotIndex > mc.thePlayer.openContainer.inventorySlots.size() - 37) return;

        final String slotName = StringUtils.stripControlCodes(slotStack.getDisplayName());

        if (slotName.equalsIgnoreCase("Remember the pattern!") && this.solving) {
            this.clickQueue.clear();
            this.solving = false;
            this.lastItemName = "";
            if (this.shouldCloseGui) {
                InventoryUtils.closeScreen();
            }
            return;
        }

        if (slotName.startsWith("Timer: ") && !this.solving) {
            this.solving = true;
            this.lastItemName = "";
            this.clickDelay.schedule(this.getRandomClickTime());
            this.shouldCloseGui = this.clickQueue.size() >= MayOBeesConfig.autoExperimentsMaxRounds;
            return;
        }

        if (this.experiment != 0 || this.solving) return;

        if (slotName.equals(this.lastItemName) && !slotStack.isItemEnchanted()) {
            this.lastItemName = "";
            return;
        }

        if (this.lastItemName.isEmpty() && slotStack.isItemEnchanted()) {
            this.lastItemName = slotName;
            this.clickQueue.add(slotIndex);
        }
    }

    @SubscribeEvent
    public void onTickUltrasequencer(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.isRunning() || this.solving || (this.experiment == 1 && !this.clickQueue.isEmpty()))
            return;
        final TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int i = 0; i <= mc.thePlayer.openContainer.inventorySlots.size() - 37; i++) {
            final Slot slot = mc.thePlayer.openContainer.getSlot(i);
            if (!slot.getHasStack()) continue;
            final String name = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
            if (name.matches("\\d+")) {
                map.put(Integer.parseInt(name), i);
            }
        }
        this.clickQueue.addAll(map.values());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.isRunning() || !this.solving) return;
        if (this.clickDelay.isScheduled() && !this.clickDelay.passed()) return;
        if (this.clickQueue.isEmpty()) return;

        InventoryUtils.clickContainerSlot(this.clickQueue.poll(), InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.CLONE);
        this.clickDelay.schedule(this.getRandomClickTime());
    }

    private int getRandomClickTime() {
        return MayOBeesConfig.autoExperimentsClickDelay + (int) (new Random().nextFloat() * MayOBeesConfig.autoExperimentsClickDelayRandomization);
    }
}
