package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.GuiClosedEvent;
import com.github.may2beez.mayobees.module.impl.utils.helper.BazaarConfig;
import com.github.may2beez.mayobees.module.impl.utils.AutoBazaar;
import com.github.may2beez.mayobees.mixin.gui.GuiContainerAccessor;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.MouseUtils;
import kotlin.Pair;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisitorHelper implements IModuleActive {
    private static VisitorHelper instance;

    public static VisitorHelper getInstance() {
        if (instance == null) {
            instance = new VisitorHelper();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    // Credits: Farmhelper
    private final Pattern requiredPattern = Pattern.compile("^(.*?)(?:\\sx(\\d+))?$");
    private BazaarConfig config;
    private GuiButton button;
    private Pair<String, Integer> itemToBuy;
    private final Predicate<Slot> visitorGuiButtonPredicate = slot -> {
        if (slot.getHasStack()) {
            String displayName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
            return slot.slotNumber < 54 && Block.getBlockFromItem(slot.getStack().getItem()) == Blocks.stained_hardened_clay && (displayName.equals("Accept Offer") || displayName.equals("Refuse Offer"));
        }
        return false;
    };

    private boolean enabled = false;
    private boolean shouldCheck = true;

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.visitorHelper && enabled;
    }

    @Override
    public String getName() {
        return "VisitorHelper";
    }

    @Override
    public void onEnable() {
        enabled = true;
        config = new BazaarConfig(itemToBuy.getFirst(),
                itemToBuy.getSecond(),
                MayOBeesConfig.visitorHelperSpendThreshold * 1000,
                MayOBeesConfig.visitorHelperGuiDelay,
                MayOBeesConfig.visitorHelperGuiDelayRandomness,
                MayOBeesConfig.visitorHelperGuiTimeoutTime);
    }

    @Override
    public void onDisable() {
        enabled = false;
        shouldCheck = true;
    }

    @SubscribeEvent
    public void detectVisitorGui(GuiScreenEvent event) {
        if (!MayOBeesConfig.visitorHelper || !(event.gui instanceof GuiChest) || isRunning() || !shouldCheck) return;
        Slot lastSlot = InventoryUtils.getSlotOfIdInContainer(((GuiChest) event.gui).inventorySlots.inventorySlots.size() - 37);
        if(lastSlot == null || !lastSlot.getHasStack()) return;
        shouldCheck = false;
        // Todo: Better Detection Method
        List<Slot> visitorGuiButtons = InventoryUtils.getIndexesOfItemsFromContainer(visitorGuiButtonPredicate);

        if (visitorGuiButtons.size() == 2) {
            LogUtils.debug("Visitors Gui Opened.");

            GuiContainerAccessor gui = ((GuiContainerAccessor) event.gui);
            int width = gui.getXSize();
            int height = gui.getYSize();
            button = new GuiButton(100, (MouseUtils.getScaledWidth() / 2) + (int) (width * 0.6), (MouseUtils.getScaledHeight() / 2) - (int) (height * 0.2), 100, 20, "Buy Items");

            Slot acceptSlot = visitorGuiButtons.stream().filter(slot -> StringUtils.stripControlCodes(slot.getStack().getDisplayName()).equals("Accept Offer")).findFirst().orElse(null);
            if (acceptSlot == null) {
                LogUtils.info("Could not find Accept Offer Slot."); // should never happen
                onDisable();
                return;
            }

            List<String> acceptSlotLore = InventoryUtils.getItemLore(acceptSlot.getStack());
            for (String lore : acceptSlotLore) {
                if (lore.contains(":")) continue;
                Matcher matcher = requiredPattern.matcher(lore.replace(",", "").trim());
                if (!matcher.find()) {
                    continue;
                }
                String name = matcher.group(1);
                int amount = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
                int amountInInv = InventoryUtils.getAmountOfItemInInventory(name);

                if (amountInInv > amount) continue;

                itemToBuy = new Pair<>(name, amount - amountInInv);
                break;
            }
            onEnable();
        }
        else{
            LogUtils.debug("No Slot Found");
        }
    }

    @SubscribeEvent
    public void onGuiClose(GuiClosedEvent event) {
        if (!(mc.currentScreen instanceof GuiChest) || (!isRunning() && shouldCheck)) return;

        LogUtils.debug("GuiClosed");
        onDisable();
    }

    @SubscribeEvent
    public void onRender(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.gui instanceof GuiChest) || !isRunning() || button == null) return;
        button.drawButton(mc, MouseUtils.getX(), MouseUtils.getY());
    }

    @SubscribeEvent
    public void onClick(GuiScreenEvent.MouseInputEvent event) {
        if (!(event.gui instanceof GuiChest) || !isRunning() || !Mouse.isButtonDown(0) && !Mouse.getEventButtonState() || button == null)
            return;
        if (!button.mousePressed(mc, MouseUtils.getX(), MouseUtils.getY())) return;
        handleMousePress();
    }

    private void handleMousePress() {
        button.playPressSound(mc.getSoundHandler());
        if (itemToBuy == null) {
            LogUtils.error("No Item To Buy.");
            return;
        }
        AutoBazaar.getInstance().buy(config);
    }
}
