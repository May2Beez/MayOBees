package com.github.may2beez.mayobees.module.impl.skills;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.event.ContainerClosedEvent;
import com.github.may2beez.mayobees.event.DrawScreenAfterEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AlchemyHelper implements IModule {
    private static AlchemyHelper instance;

    public static AlchemyHelper getInstance() {
        if (instance == null) {
            instance = new AlchemyHelper();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    private final int POTION_SLOT_1 = 38;
    private final int POTION_SLOT_2 = 40;
    private final int POTION_SLOT_3 = 42;
    private final int INGREDIENT_SLOT = 13;
    private final int TIME_SLOT = 22;

    private BlockPos lastClickedBrewingStand;

    private final Clock delay = new Clock();

    private final ArrayList<BrewingStand> brewingStands = new ArrayList<>();
    private BrewingStand currentBrewingStand;

    @Setter
    @Getter
    private boolean sellingPotions = false;

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.alchemyHelper;
    }

    @SubscribeEvent
    public void onContainerClosedEvent(ContainerClosedEvent event) {
        if (!isRunning()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.PRIVATE_ISLAND) return;
        if (lastClickedBrewingStand == null) return;
        lastClickedBrewingStand = null;
        currentBrewingStand = null;
    }

    @SubscribeEvent
    public void onRightClick(ClickEvent.Right event) {
        if (!isRunning()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.PRIVATE_ISLAND) return;
        if (event.blockPos == null) return;
        if (event.block.equals(Blocks.brewing_stand)) {
            lastClickedBrewingStand = event.blockPos;
            Optional<BrewingStand> brewingStand = brewingStands.stream().filter(bs -> bs.getPos().equals(event.blockPos)).findFirst();
            delay.schedule(300 + (int) (Math.random() * 250));
            if (!brewingStand.isPresent()) {
                BrewingStand bs = new BrewingStand(event.blockPos);
                brewingStands.add(bs);
                currentBrewingStand = bs;
            } else {
                currentBrewingStand = brewingStand.get();
            }
        }
    }

    @SubscribeEvent
    public void onGuiDraw(DrawScreenAfterEvent event) {
        if (!isRunning()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.PRIVATE_ISLAND) return;
        if (sellingPotions) return;
        if (currentBrewingStand == null) return;
        String inventoryName = InventoryUtils.getInventoryName();
        if (inventoryName == null || !inventoryName.equals("Brewing Stand")) return;

        Slot ingredient = InventoryUtils.getSlotOfIdInContainer(INGREDIENT_SLOT);
        Slot potion1 = InventoryUtils.getSlotOfIdInContainer(POTION_SLOT_1);
        Slot potion2 = InventoryUtils.getSlotOfIdInContainer(POTION_SLOT_2);
        Slot potion3 = InventoryUtils.getSlotOfIdInContainer(POTION_SLOT_3);
        Slot time = InventoryUtils.getSlotOfIdInContainer(TIME_SLOT);

        getPotion(potion1, POTION_SLOT_1, 0);

        getPotion(potion2, POTION_SLOT_2, 0);

        getPotion(potion3, POTION_SLOT_3, 500);

        putPotion(potion1, POTION_SLOT_1, 0);

        putPotion(potion2, POTION_SLOT_2, 0);

        putPotion(potion3, POTION_SLOT_3, 500);

        if (ingredient != null && ingredient.getHasStack()) {
            String ingredientName = ingredient.getStack().getDisplayName();
            currentBrewingStand.setIngredientName(ingredientName);
        } else {
            currentBrewingStand.setIngredientName("EMPTY");
            if (currentBrewingStand.timeToFinish.getRemainingTime() == 0) {
                if (MayOBeesConfig.alchemyHelperAutoPutIngredients) {
                    String potionName = StringUtils.stripControlCodes(currentBrewingStand.getPotionName());
                    if (potionName.equals("Water Bottle")) {
                        putItem(INGREDIENT_SLOT, "Nether Wart", MayOBeesConfig.getRandomizedDelayBetweenIngredientsGuiActions());
                    } else if (potionName.equals("Awkward Potion")) {
                        switch (MayOBeesConfig.alchemyHelperMaxIngredientType) {
                            case 1: {
                                putItem(INGREDIENT_SLOT, "Enchanted Sugar Cane", MayOBeesConfig.getRandomizedDelayBetweenIngredientsGuiActions());
                            }
                            case 2: {
                                putItem(INGREDIENT_SLOT, "Enchanted Blaze Rod", MayOBeesConfig.getRandomizedDelayBetweenIngredientsGuiActions());
                            }
                        }
                    }
                }
            }
        }

        if (time != null && time.getHasStack()) {
            String timeToFinish = time.getStack().getDisplayName();
            try {
                float number = Float.parseFloat(StringUtils.stripControlCodes(timeToFinish).replace("s", ""));
                if (!currentBrewingStand.getTimeToFinish().isScheduled() || currentBrewingStand.getTimeToFinish().passed()) {
                    currentBrewingStand.getTimeToFinish().schedule(number * 1000);
                }
            } catch (NumberFormatException e) {
                currentBrewingStand.getTimeToFinish().reset();
            }
        } else {
            currentBrewingStand.getTimeToFinish().reset();
        }
    }

    @SubscribeEvent
    public void onTickSellingPotions(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!isRunning()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.PRIVATE_ISLAND) return;
        if (!sellingPotions) return;
        if (!delay.passed()) return;

        String inventoryName = InventoryUtils.getInventoryName();
        if (inventoryName == null) {
            mc.thePlayer.sendChatMessage("/trades");
            delay.schedule(MayOBeesConfig.getRandomizedDelayBetweenPotionSellActions());
            return;
        }
        if (!inventoryName.equals("Trades")) {
            mc.thePlayer.closeScreen();
            delay.schedule(MayOBeesConfig.getRandomizedDelayBetweenPotionSellActions());
            return;
        }

        Slot potion = InventoryUtils.getSlotOfItemFromInventoryInOpenContainer("V Potion", false);
        if (potion == null || !potion.getHasStack()) {
            LogUtils.info("[Alchemy Helper] Sold all potions!");
            mc.thePlayer.closeScreen();
            sellingPotions = false;
            return;
        }
        InventoryUtils.clickContainerSlot(potion.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
        delay.schedule(MayOBeesConfig.getRandomizedDelayBetweenPotionSellActions());
    }

    private void putPotion(Slot potion, int potionSlot, long extraDelay) {
        if (potion != null && potion.getHasStack()) {
            String potionName = potion.getStack().getDisplayName();
            currentBrewingStand.setPotionName(potionName);
        } else {
            currentBrewingStand.setPotionName("Not Filled");
            if (MayOBeesConfig.alchemyHelperAutoPutWaterBottles)
                putItem(potionSlot, "Water Bottle", MayOBeesConfig.getRandomizedDelayBetweenPotionGuiActions() + extraDelay);
        }
    }

    private void getPotion(Slot potion, int potionSlot, long extraDelay) {
        if (!MayOBeesConfig.alchemyHelperAutoPickUpFinishPotions) return;
        if (!delay.passed()) return;
        if (potion == null || !potion.getHasStack()) return;
        String potionName = potion.getStack().getDisplayName();
        if (!potionName.endsWith("V Potion")) return;
        InventoryUtils.clickContainerSlot(potionSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.QUICK_MOVE);
        delay.schedule(MayOBeesConfig.getRandomizedDelayBetweenPotionGuiActions() + extraDelay);
        if (potionSlot == POTION_SLOT_3) {
            if (MayOBeesConfig.alchemyHelperAutoCloseGUIAfterPickingUpPotions) {
                Multithreading.schedule(() -> {
                    if (mc.currentScreen != null) InventoryUtils.closeScreen();
                }, MayOBeesConfig.getRandomizedDelayBetweenPotionGuiActions() + extraDelay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void putItem(int slot, String itemName, long delayTime) {
        if (!delay.passed()) return;
        Slot itemNameSlot = InventoryUtils.getSlotOfItemFromInventoryInOpenContainer(itemName, true);
        if (itemNameSlot == null) return;
        int stackSize = itemNameSlot.getStack().stackSize;
        LogUtils.debug("Putting " + itemName + " in slot " + slot + " from slot " + itemNameSlot.slotNumber);
        if (stackSize == 1) {
            InventoryUtils.clickContainerSlot(itemNameSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.QUICK_MOVE);
        } else {
            InventoryUtils.clickContainerSlot(itemNameSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
            Multithreading.schedule(() -> {
                if (currentBrewingStand == null) return;
                InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.RIGHT, InventoryUtils.ClickMode.PICKUP);
                Slot firstEmptySlot = InventoryUtils.getFirstEmptySlotInInventory();
                if (firstEmptySlot != null) {
                    Multithreading.schedule(() -> InventoryUtils.clickContainerSlot(firstEmptySlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP), delayTime / 2, TimeUnit.MILLISECONDS);
                } else {
                    LogUtils.error("[Alchemy Helper] Inventory is full!");
                    InventoryUtils.closeScreen();
                }
                delay.schedule(delayTime);
            }, delayTime / 2, TimeUnit.MILLISECONDS);
        }
        delay.schedule(delayTime);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.PRIVATE_ISLAND) return;

        for (BrewingStand brewingStand : brewingStands) {
            RenderUtils.drawText(EnumChatFormatting.GOLD + "Time:", brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 2.5, brewingStand.getPos().getZ() + 0.5, 0.5f);
            RenderUtils.drawText(brewingStand.getTimeToFinish().getRemainingTime() > 0 ? (EnumChatFormatting.AQUA + String.format("%.2f", brewingStand.getTimeToFinish().getRemainingTime() / 1000f)) : "Finished", brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 2.25, brewingStand.getPos().getZ() + 0.5, 0.5f);
            RenderUtils.drawText(EnumChatFormatting.BLUE + "Potion:", brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 1.75, brewingStand.getPos().getZ() + 0.5, 0.5f);
            RenderUtils.drawText(String.valueOf(brewingStand.getPotionName()), brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 1.5, brewingStand.getPos().getZ() + 0.5, 0.5f);
            RenderUtils.drawText(EnumChatFormatting.RED + "Ingredient:", brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 1.0, brewingStand.getPos().getZ() + 0.5, 0.5f);
            RenderUtils.drawText(String.valueOf(brewingStand.getIngredientName()), brewingStand.getPos().getX() + 0.5, brewingStand.getPos().getY() + 0.75, brewingStand.getPos().getZ() + 0.5, 0.5f);
        }
    }

    @Getter
    @Setter
    public static class BrewingStand {
        private final BlockPos pos;
        private final Clock timeToFinish = new Clock();
        private String potionName = "";
        private String ingredientName = "";

        public BrewingStand(BlockPos pos) {
            this.pos = pos;
        }
    }
}
