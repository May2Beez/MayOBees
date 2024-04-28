package com.github.may2beez.mayobees.module.impl.other;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.HeadUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.ScoreboardUtils;
import com.github.may2beez.mayobees.util.TablistUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
        if (MayOBeesConfig.saveScoreboardToFile) {
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

    //<editor-fold desc="Packet listener">
    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (MayOBeesConfig.listenToIncomingPackets) {
            LogUtils.debug("Received packet: " + event.packet.getClass().getSimpleName());
        }
    }
    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (MayOBeesConfig.listenToIncomingPackets) {
            LogUtils.debug("Sent packet: " + event.packet.getClass().getSimpleName());
        }
    }
    //</editor-fold>

    //<editor-fold desc="Entity NBT">
    public void getEntityNBT() {
        MovingObjectPosition objectMouseOver = Minecraft.getMinecraft().objectMouseOver;
        Entity entity = null;
        if (objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && objectMouseOver.entityHit != null) {
            entity = objectMouseOver.entityHit;
        }
        if (entity == null) {
            LogUtils.info("Entity is null!");
            return;
        }
        String nbt = getEntityNBTtoString(entity);
        if (nbt == null || nbt.isEmpty()) {
            LogUtils.info(MayOBeesConfig.entityNBTArmorStandSkullsOnly ? "Skull" : "Entity" + " NBT is empty!");
            return;
        }
        if (MayOBeesConfig.saveEntityNBTToFile) {
            try {
                FileWriter file = new FileWriter("entityNBT_" + getCurrentTime() + ".txt");
                file.write(nbt.replace("§", "&") + "\n");
                file.close();
                LogUtils.info("Saved entity NBT to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save entity NBT to file!");
                e.printStackTrace();
            }
        } else {
            System.out.println(nbt);
            LogUtils.info("Printed entity NBT to console!");
        }
    }

    private static String getEntityNBTtoString(Entity entity) {
        String nbt;
        try {
            if (MayOBeesConfig.entityNBTArmorStandSkullsOnly && !HeadUtils.isArmorStandWithSkull(entity))
                return null;
            nbt = entity.serializeNBT().toString();
        } catch (Exception ignored) {
            return null;
        }
        return nbt;
    }

    public void getAllLoadedEntityNBT() {
        if (mc.theWorld == null) {
            LogUtils.info("World is null!");
            return;
        }
        List<String> entityNBTs = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            String nbt = getEntityNBTtoString(entity);
            if (nbt == null || nbt.isEmpty()) {
                continue;
            }
            entityNBTs.add(nbt);
        }
        if (MayOBeesConfig.saveEntityNBTToFile) {
            try {
                FileWriter file = new FileWriter("allEntityNBT_" + getCurrentTime() + ".txt");
                for (String nbt : entityNBTs) {
                    file.write(nbt.replace("§", "&") + "\n");
                }
                file.close();
                LogUtils.info("Saved all entity NBTs to file!");
            } catch (IOException e) {
                LogUtils.error("Failed to save all entity NBTs to file!");
                e.printStackTrace();
            }
        } else {
            for (String nbt : entityNBTs) {
                System.out.println(nbt);
            }
            LogUtils.info("Printed all entity NBTs to console!");
        }
    }
    //</editor-fold>
}
