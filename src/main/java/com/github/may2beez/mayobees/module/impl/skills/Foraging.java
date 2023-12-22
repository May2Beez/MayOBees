package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.UngrabUtils;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import com.github.may2beez.mayobees.util.helper.Timer;
import com.google.common.base.Splitter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Foraging implements IModuleActive {
    private static Foraging instance;
    public static Foraging getInstance() {
        if (instance == null) {
            instance = new Foraging();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Foraging";
    }

    public static Minecraft mc = Minecraft.getMinecraft();

    private enum MacroState {
        LOOK, PLACE, PLACE_BONE, BREAK, FIND_ROD, FIND_BONE, THROW_ROD, THROW_BREAK_DELAY, SWITCH
    }

    private static MacroState macroState = MacroState.LOOK;
    private static MacroState lastState = null;

    public static Vec3 bestDirt;

    private final Timer stuckTimer = new Timer();
    private boolean stuck = false;

    public boolean isRunning() {
        return MayOBeesConfig.foraging;
    }

    @Override
    public void onEnable() {
        MayOBeesConfig.foraging = true;
        bestDirt = null;
        macroState = MacroState.LOOK;
        startedAt = System.currentTimeMillis();
        earnedXp = 0;
        stuckTimer.schedule();
        stuck = false;
        updateXpTimer.schedule();
        if (MayOBeesConfig.mouseUngrab)
            UngrabUtils.ungrabMouse();
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(), false);
        MayOBeesConfig.foraging = false;
        UngrabUtils.regrabMouse();
    }

    private static final Timer updateXpTimer = new Timer();
    private static final Timer waitTimer = new Timer();
    private static final Timer waitAfterFinishTimer = new Timer();
    private static double xpPerHour = 0;

    public static String[] drawFunction() {
        String[] textToDraw = new String[4];
        if (updateXpTimer.hasPassed(100)) {
            xpPerHour = earnedXp / ((System.currentTimeMillis() - startedAt) / 3600000.0);
            updateXpTimer.reset();
        }
        textToDraw[0] = "§r§lForaging Macro";
        textToDraw[1] = "§r§lState: §f" + macroState;
        textToDraw[2] = "§r§lXP/H: §f" + String.format("%.2f", xpPerHour);
        textToDraw[3] = "§r§lXP Since start: §f" + String.format("%.2f", earnedXp);
        return textToDraw;
    }

    private static long startedAt = 0;
    private static double earnedXp = 0;

    private static final Splitter SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults();
    private static final Pattern SKILL_PATTERN = Pattern.compile("\\+([\\d.]+)\\s+([A-Za-z]+)\\s+\\((\\d+(\\.\\d+)?)%\\)");

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (!isRunning()) return;
        if (event.type != 2) return;

        String actionBar = StringUtils.stripControlCodes(event.message.getUnformattedText());

        List<String> components = SPACE_SPLITTER.splitToList(actionBar);

        for (String component : components) {
            Matcher matcher = SKILL_PATTERN.matcher(component);
            // System.out.println(component);
            if (matcher.matches()) {
                String addedXp = matcher.group(1);
                String skillName = matcher.group(2);
                String percentage = matcher.group(3);
                if (skillName.equalsIgnoreCase("foraging")) {
                    earnedXp += Double.parseDouble(addedXp) * 6.5;
                }
            }
        }
    }

//    @SubscribeEvent
//    public void onRenderWorldLast(RenderWorldLastEvent event) {
//        if (!isRunning()) return;
//
//        if (bestDirt != null) {
//            AxisAlignedBB bb = new AxisAlignedBB(bestDirt.xCoord - 0.5, bestDirt.yCoord - 0.5, bestDirt.zCoord - 0.5, bestDirt.xCoord + 0.5, bestDirt.yCoord + 0.5, bestDirt.zCoord + 0.5);
//            RenderUtils.drawBox(bb, Color.green);
//        }
//    }

    private Vec3 getDirt() {
        Vec3 furthest = null;
        Vec3 player = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ);
        for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(mc.thePlayer.posX + 4.0D, mc.thePlayer.posY, mc.thePlayer.posZ + 4.0D), new BlockPos(mc.thePlayer.posX - 4.0D, mc.thePlayer.posY - 1.0D, mc.thePlayer.posZ - 4.0D))) {
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.dirt || mc.theWorld.getBlockState(pos).getBlock() == Blocks.grass) {
                Block block = mc.theWorld.getBlockState(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())).getBlock();
                if (!(block instanceof net.minecraft.block.BlockLog) && block != Blocks.sapling) {
                    Vec3 distance = new Vec3(pos.getX() + 0.5D + (Math.random() * 0.4) - 0.2, (pos.getY() + 1), pos.getZ() + 0.5D + (Math.random() * 0.4) - 0.2);
                    if (furthest == null || player.squareDistanceTo(distance) > player.squareDistanceTo(furthest))
                        furthest = distance;
                }
            }
        }
        return furthest;
    }

    private void unstuck() {
        LogUtils.debug("I'm stuck! Unstuck process activated");
        stuck = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) {
            macroState = MacroState.LOOK;
            return;
        }

        if (RotationHandler.getInstance().isRotating()) return;

        if (stuck) {
            Vec3 closest = null;
            Vec3 player = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ);
            for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(mc.thePlayer.posX + 4.0D, mc.thePlayer.posY, mc.thePlayer.posZ + 4.0D), new BlockPos(mc.thePlayer.posX - 4.0D, mc.thePlayer.posY - 1.0D, mc.thePlayer.posZ - 4.0D))) {
                if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.dirt || mc.theWorld.getBlockState(pos).getBlock() == Blocks.grass) {
                    Block block = mc.theWorld.getBlockState(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())).getBlock();
                    if ((block instanceof net.minecraft.block.BlockLog) || block == Blocks.sapling) {
                        Vec3 distance = new Vec3(pos.getX() + 0.5D, (pos.getY() + 1), pos.getZ() + 0.5D);
                        if (closest == null || player.squareDistanceTo(distance) <= player.squareDistanceTo(closest))
                            closest = distance;
                    }
                }
            }
            int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
            if (treecapitator == -1) {
                LogUtils.error("No Treecapitator found in hotbar!");
                onDisable();
                return;
            }

            mc.thePlayer.inventory.currentItem = treecapitator;

            MovingObjectPosition mop = mc.objectMouseOver;

            boolean shouldBreak = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.dirt);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), shouldBreak);

            if(closest != null) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(closest), 125, RotationConfiguration.RotationType.CLIENT, null));
            } else {
                stuck = false;
                macroState = MacroState.LOOK;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                stuckTimer.schedule();
            }
            return;
        }

        if (stuckTimer.hasPassed(MayOBeesConfig.stuckTimeout)) {
            unstuck();
            return;
        }

        switch (macroState) {
            case LOOK:
                int saplingSlot = InventoryUtils.getSlotIdOfItemInHotbar("Sapling");
                if (saplingSlot == -1) {
                    LogUtils.error("No saplings found in hotbar!");
                    onDisable();
                    return;
                }
                mc.thePlayer.inventory.currentItem = saplingSlot;
                bestDirt = getDirt();
                if (bestDirt != null) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(bestDirt), 150 + (new Random().nextInt(50)), RotationConfiguration.RotationType.CLIENT, null));
                    macroState = MacroState.PLACE;
                } else {
                    macroState = MacroState.FIND_BONE;
                }
                return;
            case PLACE:
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                macroState = MacroState.LOOK;
                return;
            case FIND_BONE:
                int boneMeal = InventoryUtils.getSlotIdOfItemInHotbar("Bone Meal");
                if (boneMeal == -1) {
                    LogUtils.error("No Bone Meal found in hotbar!");
                    onDisable();
                    return;
                }
                mc.thePlayer.inventory.currentItem = boneMeal;
                macroState = MacroState.PLACE_BONE;
                waitTimer.schedule();
                break;
            case PLACE_BONE:
                if(waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    MovingObjectPosition mop = mc.objectMouseOver;
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.sapling)) {
                        KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                    }
                    waitTimer.schedule();
                    if(MayOBeesConfig.foragingUseRod) {
                        macroState = MacroState.FIND_ROD;
                    } else {
                        macroState = MacroState.THROW_BREAK_DELAY;
                    }
                }
                break;
            case FIND_ROD:
                if(waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    int rod = InventoryUtils.getSlotIdOfItemInHotbar("Rod");
                    if (rod == -1) {
                        LogUtils.error("No Fishing Rod found in hotbar!");
                        onDisable();
                        break;
                    }
                    mc.thePlayer.inventory.currentItem = rod;
                    waitTimer.schedule();
                    macroState = MacroState.THROW_ROD;
                }
                break;
            case THROW_ROD:
                if(waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                    waitTimer.schedule();
                    macroState = MacroState.THROW_BREAK_DELAY;
                }
                break;
            case THROW_BREAK_DELAY:
                if(waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    waitTimer.schedule();
                    macroState = MacroState.BREAK;
                }
                break;
            case BREAK:
                int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
                if (treecapitator == -1) {
                    LogUtils.error("No Treecapitator found in hotbar!");
                    onDisable();
                    break;
                }
                mc.thePlayer.inventory.currentItem = treecapitator;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                BlockPos logPos = mc.objectMouseOver.getBlockPos();
                if(logPos != null && !(mc.theWorld.getBlockState(logPos).getBlock() instanceof BlockLog)) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                    waitAfterFinishTimer.schedule();
                    macroState = MacroState.SWITCH;
                }
                break;
            case SWITCH:
                if(waitAfterFinishTimer.hasPassed(MayOBeesConfig.foragingWaitAfter)) {
                    macroState = MacroState.LOOK;
                }
                break;
        }

        if (lastState != macroState) {
            lastState = macroState;
            stuckTimer.schedule();
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if(!isRunning()) return;
        onDisable();
    }
}
