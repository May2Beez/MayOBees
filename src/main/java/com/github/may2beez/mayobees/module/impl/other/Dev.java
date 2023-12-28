package com.github.may2beez.mayobees.module.impl.other;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.ScoreboardUtils;
import com.github.may2beez.mayobees.util.TablistUtils;
import net.minecraft.client.Minecraft;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Dev implements IModule {
    private static Dev instance;
    public static Dev getInstance() {
        if (instance == null) {
            instance = new Dev();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Dev";
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public boolean isRunning() {
        return false;
    }

    //<editor-fold desc="Tablist">
    public static List<String> transposeList(List<String> originalTable, int numRows, int numCols) {
        List<String> transposedTable = new ArrayList<>();

        for (int col = 0; col < numCols; col++) {
            StringBuilder sb = new StringBuilder();
            for (int row = 0; row < numRows; row++) {
                int index = row * numCols + col;
                String originalRow = originalTable.get(index);
                sb.append(originalRow).append("\n");
            }
            transposedTable.add(sb.toString());
        }

        return transposedTable;
    }

    public void getTablist() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (TablistUtils.getTabListPlayersUnprocessed().isEmpty()) {
            LogUtils.info("Tablist is empty!");
            return;
        }
        List<String> tabList;

        // TODO: Header and footer

        if (MayOBeesConfig.transposedTablist)
            tabList = transposeList(TablistUtils.getTabListPlayersUnprocessed(), 4, 20);
        else
            tabList = TablistUtils.getTabListPlayersUnprocessed();

        if (MayOBeesConfig.saveTablistToFile) {
            try {
                FileWriter file = new FileWriter("tablist_" + getCurrentTime() + ".txt");
                for (String name : tabList) {
                    file.write(name.replace("§", "&") + "\n");
                }
                file.close();
                LogUtils.info("Saved tablist to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save tablist to file!");
                e.printStackTrace();
            }
        } else {
            for (String name : tabList) {
                System.out.println(name);
            }
            LogUtils.info("Printed tablist to console!");
        }
    }
    //</editor-fold>

    //<editor-fold desc="Inventory">
    public void getInventory() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.thePlayer.inventoryContainer == null) {
            LogUtils.info("Inventory is empty!");
            return;
        }
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++) {
            if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {
                String name = mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName();
                inventory.add(i + " | " + name);
            }
        }
        if (MayOBeesConfig.saveInventoryToFile) {
            try {
                FileWriter file = new FileWriter("inventory_" + getCurrentTime() + ".txt");
                for (String name : inventory) {
                    file.write(name.replace("§", "&") + "\n");
                }
                file.close();
                LogUtils.info("Saved inventory to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save inventory to file!");
                e.printStackTrace();
            }
        } else {
            for (String name : inventory) {
                System.out.println(name);
            }
            LogUtils.info("Printed inventory to console!");
        }
    }
    //</editor-fold>

    //<editor-fold desc="Item Lore">
    public void getItemLore(int slot) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.thePlayer.inventoryContainer == null) {
            LogUtils.info("Inventory is empty!");
            return;
        }
        if (mc.thePlayer.inventoryContainer.inventorySlots.get(slot).getStack() == null) {
            LogUtils.info("Slot is empty!");
            return;
        }
        List<String> lore = mc.thePlayer.inventoryContainer.inventorySlots.get(slot).getStack().getTooltip(mc.thePlayer, false);
        if (MayOBeesConfig.saveItemLoreToFile) {
            try {
                FileWriter file = new FileWriter("itemLore_" + getCurrentTime() + ".txt");
                for (String name : lore) {
                    file.write(name.replace("§", "&") + "\n");
                }
                file.close();
                LogUtils.info("Saved item lore to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save item lore to file!");
                e.printStackTrace();
            }
        } else {
            for (String name : lore) {
                System.out.println(name);
            }
            LogUtils.info("Printed item lore to console!");
        }
    }
    //</editor-fold>

    //<editor-fold desc="Scoreboard">
    public void getScoreboard() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (MayOBeesConfig.saveInventoryToFile) {
            try {
                FileWriter file = new FileWriter("scoreboard_" + getCurrentTime() + ".txt");
                if (MayOBeesConfig.includeScoreboardTitle)
                    System.out.println(ScoreboardUtils.getScoreboardTitle());
                for (String line : ScoreboardUtils.getScoreboardLines()) {
                    String cleanedLine = ScoreboardUtils.cleanSB(line);
                    file.write((MayOBeesConfig.cleanScoreboardLines ? cleanedLine : line).replace("§", "&") + "\n");
                }
                file.close();
                LogUtils.info("Saved scoreboard to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save scoreboard to file!");
                e.printStackTrace();
            }
        } else {
            if (MayOBeesConfig.includeScoreboardTitle)
                System.out.println(ScoreboardUtils.getScoreboardTitle());
            for (String line : ScoreboardUtils.getScoreboardLines()) {
                String cleanedLine = ScoreboardUtils.cleanSB(line);
                System.out.println(MayOBeesConfig.cleanScoreboardLines ? cleanedLine : line);
            }
            LogUtils.info("Printed inventory to console!");
        }
    }

    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }
    //</editor-fold>
}
