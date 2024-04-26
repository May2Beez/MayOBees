package com.github.may2beez.mayobees.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class InventoryUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int getSlotIdOfItemInContainer(String item) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static boolean isInventoryEmpty(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    public static Slot getSlotOfItemInContainer(String item) {
        return getSlotOfItemInContainer(item, false);
    }

    public static Slot getSlotOfItemInContainer(String item, boolean equals) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (equals) {
                    if (itemName.equalsIgnoreCase(item)) {
                        return slot;
                    }
                } else {
                    if (itemName.contains(item)) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    public static Slot getSlotOfItemInContainer(String item, int minimumStackSize) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item) && slot.getStack().stackSize >= minimumStackSize) {
                    return slot;
                }
            }
        }
        return null;
    }

    public static int getSlotIdOfItemInHotbar(String... items) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.stripControlCodes(slot.getDisplayName());
                if (Arrays.stream(items).anyMatch(itemName::contains)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static Slot getSlotOfItemInHotbar(String item) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.stripControlCodes(slot.getDisplayName());
                if (itemName.contains(item)) {
                    return mc.thePlayer.inventoryContainer.getSlot(i);
                }
            }
        }
        return null;
    }

    public static int getSlotIdOfItemInInventory(String item) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static Slot getSlotOfItemInInventory(String item) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot;
                }
            }
        }
        return null;
    }

    public static Slot getSlotOfItemFromInventoryInOpenContainer(String item, boolean equals) {
        final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot != null && slot.getHasStack() && slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (equals) {
                    if (itemName.equalsIgnoreCase(item)) {
                        return slot;
                    }
                } else {
                    if (itemName.contains(item)) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    public static String getInventoryName() {
        try {
            if (mc.currentScreen instanceof GuiChest) {
                final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) return null;
                final IInventory inv = chest.getLowerChestInventory();
                return inv.hasCustomName() ? inv.getName() : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasItemInInventory(String item) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasItemInHotbar(String... item) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.stripControlCodes(slot.getDisplayName());
                if (Arrays.stream(item).anyMatch(itemName::contains)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasItemInContainer(String item) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromInventory(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromContainer(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size(); i++) {
            Slot slot = mc.thePlayer.openContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static void clickSlotWithId(int id, ClickType mouseButton, ClickMode mode, int windowId) {
        mc.playerController.windowClick(windowId, id, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void clickContainerSlot(int slot, ClickType mouseButton, ClickMode mode) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void clickSlot(int slot, ClickType mouseButton, ClickMode mode) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void swapSlots(int slot, int hotbarSlot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hotbarSlot, 2, mc.thePlayer);
    }

    public static void openInventory() {
        KeyBinding.onTick(mc.gameSettings.keyBindInventory.getKeyCode());
    }

    public static void moveEveryItemToContainer(String item) {
        if (mc.currentScreen instanceof GuiChest) {
            final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            for (Slot slot : chest.inventorySlots) {
                if (slot != null && slot.getHasStack() && slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()) {
                    if (StringUtils.stripControlCodes(slot.getStack().getDisplayName()).equalsIgnoreCase(item)) {
                        clickContainerSlot(slot.slotNumber, ClickType.LEFT, ClickMode.QUICK_MOVE);
                    }
                }
            }
        }
    }

    public static Slot getSlotOfId(int id) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.slotNumber == id) {
                return slot;
            }
        }
        return null;
    }

    public static Slot getSlotOfIdInContainer(int id) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.slotNumber == id) {
                return slot;
            }
        }
        return null;
    }

    public static ArrayList<String> getItemLore(ItemStack itemStack) {
        NBTTagList loreTag = itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
        ArrayList<String> loreList = new ArrayList<>();
        for (int i = 0; i < loreTag.tagCount(); i++) {
            loreList.add(StringUtils.stripControlCodes(loreTag.getStringTagAt(i)));
        }
        return loreList;
    }

    public static List<String> getLoreOfItemInContainer(int slot) {
        if (slot == -1) return new ArrayList<>();
        ItemStack itemStack = mc.thePlayer.openContainer.getSlot(slot).getStack();
        if (itemStack == null) return new ArrayList<>();
        return getItemLore(itemStack);
    }

    public static int getAmountOfItemInInventory(String item) {
        int amount = 0;
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.equals(item)) {
                    amount += slot.getStack().stackSize;
                }
            }
        }
        return amount;
    }

    public static int getRancherBootSpeed() {
        final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(8).getStack();
        int speed = -1;
        if (stack != null && stack.hasTagCompound()) {
            final NBTTagCompound tag = stack.getTagCompound();
            final Pattern pattern = Pattern.compile("(Current Speed Cap: §a\\d+)", Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(tag.toString());
            while (matcher.find()) {
                if (matcher.group(0) != null) {
                    speed = parseInt((matcher.group(0).replaceAll("Current Speed Cap: §a", "")));
                }
            }
        }
        return speed;
    }

    public static boolean hasFreeSlots() {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (!slot.getHasStack()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFreeSlotsInContainer() {
        final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.slotNumber < chest.getLowerChestInventory().getSizeInventory()) {
                if (!slot.getHasStack()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Slot getFirstEmptySlotInInventory() {
        if (mc.thePlayer.openContainer != null) {
            final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                if (slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()) {
                    if (!slot.getHasStack()) {
                        return slot;
                    }
                }
            }
        } else {
            for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
                if (!slot.getHasStack()) {
                    return slot;
                }
            }
        }
        return null;
    }

    public static boolean closeScreen() {
        if (mc.thePlayer == null) return false;
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
            return true;
        }
        return false;
    }

    public static enum ClickType {
        LEFT,
        RIGHT
    }

    public static enum ClickMode {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE
    }
}
