package com.github.may2beez.mayobees.module.impl.utils;

import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.module.impl.utils.helper.BazaarConfig;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.SignUtils;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBazaar implements IModuleActive {
    private static AutoBazaar instance;

    public static AutoBazaar getInstance() {
        if (instance == null) {
            instance = new AutoBazaar();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean hasSucceeded = false;
    private boolean hasFailed = false;
    private FailReason failReason = FailReason.NONE;
    private final Clock timer = new Clock();
    private final Clock timeoutTimer = new Clock();
    private final Pattern instabuyAmountPattern = Pattern.compile("Amount:\\s(\\d+)x");
    private final Pattern totalCostPattern = Pattern.compile("Price:\\s(\\d+(.\\d+)?)\\scoins");
    private final Predicate<Slot> instaBuyButtonPredicate = slot -> {
        if (slot.getHasStack()) {
            String name = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
            return name.startsWith("Buy") || name.equals("Fill my inventory!");
        }
        return false;
    };

    private BazaarConfig config;

    private String buyButtonText = null;
    private String itemToBuy = null;
    private int buyAmount = 0;
    private int spendThreshold = 0;
    private BuyState buyState = BuyState.STARTING;

    @Override
    public boolean isRunning() {
        return !(mc.thePlayer == null || mc.theWorld == null || !enabled);
    }

    @Override
    public String getName() {
        return "AutoBazaar";
    }

    @Override
    public void onEnable() {
        enabled = true;
        hasSucceeded = false;
        hasFailed = false;
        failReason = FailReason.NONE;

        resetState();
    }

    @Override
    public void onDisable() {
        enabled = false;

        resetState();
    }

    public void buy(BazaarConfig config) {
        if (isRunning()) return;

        this.config = config;
        if (config.verifyConfig()) {
            LogUtils.info("Something is wrong with BazaarConfig. Contact developer.");
            LogUtils.info(config.toString());
            return;
        }

        onEnable();
        this.itemToBuy = config.itemToBuy;
        this.buyAmount = config.buyAmount;
        this.spendThreshold = config.spendThreshold;

        LogUtils.debug("Enabling");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;

        switch (buyState) {
            case STARTING:
                changeState(BuyState.OPENING_BZ, 0, false);
                break;
            case OPENING_BZ:
                mc.thePlayer.sendChatMessage("/bz " + itemToBuy);
                changeState(BuyState.WAITING_FOR_GUI, 2000, false);
                break;
            case WAITING_FOR_GUI:
                hasClickTimerEnded();
                if (!(mc.currentScreen instanceof GuiChest)) return;

                changeState(BuyState.NAVIGATING_GUI);
                break;
            case NAVIGATING_GUI:
                if (!hasClickTimerEnded()) return;
                if (!(mc.currentScreen instanceof GuiChest)) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }

                String inventoryName = InventoryUtils.getInventoryName();

                String button = getButtonNameToClick(inventoryName, itemToBuy);
                if (button == null) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }

                String slotToClick;

                if (button.equals("Custom Amount")) {
                    Pair<String, Boolean> customBtn = getCustomAmountButton(inventoryName, itemToBuy);
                    if (customBtn.getFirst() == null) {
                        LogUtils.debug("Could not find button. Waiting.");
                        changeState(BuyState.NAVIGATING_GUI, 200, false);
                        return;
                    }
                    if (customBtn.getSecond()) {
                        if (isManipulated(customBtn.getFirst(), spendThreshold)) {
                            disable(false, FailReason.MANIPULATED);
                            return;
                        }

                        buyButtonText = customBtn.getFirst();
                        changeState(BuyState.BUYING_ITEM);
                        return;
                    }
                    changeState(BuyState.EDITING_SIGN);
                    slotToClick = customBtn.getFirst();
                } else {
                    changeState(BuyState.WAITING_FOR_GUI, 2000, false);
                    slotToClick = button;
                }

                Slot itemSlot = InventoryUtils.getSlotOfItemInContainer(slotToClick, true);
                if (itemSlot == null || itemSlot.slotNumber > mc.thePlayer.openContainer.inventorySlots.size() - 37) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }
                InventoryUtils.clickContainerSlot(itemSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                break;
            case EDITING_SIGN:
                if (!hasClickTimerEnded()) return;
                if (!(mc.currentScreen instanceof GuiEditSign)) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }

                SignUtils.setTextToWriteOnString(String.valueOf(buyAmount));
                changeState(BuyState.CONFIRMING_SIGN);
                break;
            case CONFIRMING_SIGN:
                if (!hasClickTimerEnded()) return;

                SignUtils.confirmSign();
                changeState(BuyState.WAITING_FOR_GUI);
                break;
            case BUYING_ITEM:
                if (!hasClickTimerEnded()) return;
                if (buyButtonText == null) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }

                int slotIndex = InventoryUtils.getSlotIdOfItemInContainer(buyButtonText);
                if (slotIndex == -1 || slotIndex > mc.thePlayer.openContainer.inventorySlots.size() - 37) {
                    disable(false, FailReason.UNUSABLE);
                    return;
                }

                InventoryUtils.clickContainerSlot(slotIndex, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                changeState(BuyState.VERIFYING_PURCHASE, 2000, false);
                break;
            case VERIFYING_PURCHASE:
                if (timer.isScheduled() && timer.passed()) {
                    disable(false, FailReason.UNUSABLE);
                }
                break;
            case DISABLING:
                if (timer.isScheduled() && !timer.passed()) return;
                InventoryUtils.closeScreen();
                disable(true, FailReason.NONE);
                break;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0 || !isRunning()) return;
        if (event.message == null) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String boughtMessage = String.format("[Bazaar] Bought %dx %s for", this.buyAmount, this.itemToBuy);

        if (message.startsWith(boughtMessage) && buyState == BuyState.VERIFYING_PURCHASE) {
            changeState(BuyState.DISABLING);
        }
    }


    private void changeState(BuyState state) {
        changeState(state, getRandomGUIClickDelay(), true);
    }

    private void changeState(BuyState state, long time, boolean scheduleTimeout) {
        timer.reset();
        timeoutTimer.reset();
        timer.schedule(time);
        buyState = state;
        if (scheduleTimeout) {
            timeoutTimer.schedule(config.guiWaitTimeout);
        }
    }

    private boolean hasClickTimerEnded() {
        if (timeoutTimer.isScheduled() && timeoutTimer.passed()) {
            disable(false, FailReason.UNUSABLE);
            return false;
        }
        return timer.isScheduled() && timer.passed();
    }

    private void disable(boolean succeeded, FailReason reason) {
        hasSucceeded = succeeded;
        hasFailed = !hasSucceeded;
        failReason = reason;
        LogUtils.debug("Disabling. Succeeded: " + succeeded);
        if (failReason != FailReason.NONE) {
            LogUtils.error(reason.failReason);
        }

        onDisable();
    }

    private void resetState() {
        timer.reset();
        timeoutTimer.reset();
        itemToBuy = null;
        buyAmount = 0;
        spendThreshold = 0;
        buyState = BuyState.STARTING;
        buyButtonText = null;
    }

    enum BuyState {
        STARTING,
        OPENING_BZ,
        WAITING_FOR_GUI,
        NAVIGATING_GUI,
        EDITING_SIGN,
        CONFIRMING_SIGN,
        BUYING_ITEM,
        VERIFYING_PURCHASE,
        DISABLING
    }

    // Todo: Add More Specific Fail Reasons to Make debugging easier.
    enum FailReason {
        MANIPULATED("Item Price Exceeded the max spend threshold. Increase it or check if item was manipulated or not."),
        UNUSABLE("Could not find item / open gui. Pleas Report to the developer if this is a bug."),
        NONE("AutoBazaar bought the item successfully.");
        private final String failReason;

        FailReason(String failReason) {
            this.failReason = failReason;
        }
    }


    private String getButtonNameToClick(String inventoryName, String itemName) {
        if (("Bazaar ➜ \"" + itemName + "\"").startsWith(inventoryName)) {
            return itemName;
        }
        if ((itemName + " ➜ " + "Instant Buy").startsWith(inventoryName) ||
                inventoryName.equals("Confirm Instant Buy")) {
            return "Custom Amount";
        }
        String[] invNameSplit = inventoryName.split(" ➜ ");
        if (invNameSplit.length > 1 && itemName.contains(invNameSplit[1])) {
            return "Buy Instantly";
        }
        return null;
    }

    private Pair<String, Boolean> getCustomAmountButton(String inventoryName, String itemName) {
        Pair<String, Boolean> defaultBtn = new Pair<>(null, false);
        if ((itemName + " ➜ " + "Instant Buy").startsWith(inventoryName)) {
            List<Slot> buySlots = InventoryUtils.getIndexesOfItemsFromContainer(instaBuyButtonPredicate);
            if (buySlots.isEmpty()) return defaultBtn;
            for (Slot slot : buySlots) {
                String lore = String.join(" ", InventoryUtils.getItemLore(slot.getStack())).replace(",", "");
                Matcher matcher = instabuyAmountPattern.matcher(lore);

                if (matcher.find() && Integer.parseInt(matcher.group(1)) == this.buyAmount) {
                    return new Pair<>(StringUtils.stripControlCodes(slot.getStack().getDisplayName()), true);
                }
            }
            return new Pair<>("Custom Amount", false);
        }

        if (inventoryName.equals("Confirm Instant Buy")) {
            return new Pair<>("Custom Amount", true);
        }
        return defaultBtn;
    }

    private boolean isManipulated(String buttonName, int spendThreshold) {
        if (spendThreshold == 0) return false;
        Slot buySlot = InventoryUtils.getSlotOfItemInContainer(buttonName, true);
        if (buySlot == null) return true; // this should never happen

        String lore = String.join(" ", InventoryUtils.getItemLore(buySlot.getStack())).replace(",", "");
        Matcher matcher = totalCostPattern.matcher(lore);

        if (!matcher.find()) return true;

        float amount = Float.parseFloat(matcher.group(1));
        LogUtils.debug("Price: " + amount);
        LogUtils.debug("Spend Threshold: " + spendThreshold);
        return amount > spendThreshold;
    }

    // Todo: Use a global one
    public long getRandomGUIClickDelay() {
        return (long) (config.guiClickDelay + (float) Math.random() * config.guiClickDelayRandomness);
    }
}

