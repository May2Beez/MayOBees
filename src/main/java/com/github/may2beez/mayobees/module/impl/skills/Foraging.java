package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.UpdateTablistEvent;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.*;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import com.github.may2beez.mayobees.util.helper.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
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
        LOOK,
        PLACE,
        FIND_BONE,
        PLACE_BONE,
        FIND_ROD,
        THROW_ROD,
        FIND_TREECAPITATOR,
        BREAK,
        SWITCH
    }

    private MacroState macroState = MacroState.LOOK;
    private MacroState lastState = null;

    public Vec3 currentTarget;
    private final ArrayList<Vec3> dirtBlocks = new ArrayList<>();

    private final Timer stuckTimer = new Timer();
    private boolean stuck = false;

    private boolean enabled = false;
    private long lastBreakTime = 0;
    public long startTime = 0;
    public long stopTime = 0;

    public boolean isRunning() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        if (MayOBeesConfig.mouseUngrab)
            UngrabUtils.ungrabMouse();
        startTime = System.currentTimeMillis() - (stopTime - startTime);
        dirtBlocks.clear();
        dirtBlocks.addAll(getDirts());
        currentTarget = null;
        macroState = MacroState.LOOK;
        lastBreakTime = 0;
        stuckTimer.schedule();
        stuck = false;
    }

    @Override
    public void onDisable() {
        stopTime = System.currentTimeMillis();
        KeyBindUtils.stopMovement();
        enabled = false;
        UngrabUtils.regrabMouse();
    }

    private static final Timer waitTimer = new Timer();
    private static final Timer waitAfterFinishTimer = new Timer();
    private int randomTime = (int) (150 + Math.random() * 100);

    private final Color color = new Color(0, 200, 0, 80);
    private final Color targetColor = new Color(200, 0, 0, 80);

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;

        double x = mc.getRenderManager().viewerPosX;
        double y = mc.getRenderManager().viewerPosY;
        double z = mc.getRenderManager().viewerPosZ;
        if (currentTarget != null) {
            AxisAlignedBB bb = new AxisAlignedBB(currentTarget.xCoord - 0.05, currentTarget.yCoord - 0.05, currentTarget.zCoord - 0.05, currentTarget.xCoord + 0.05, currentTarget.yCoord + 0.05, currentTarget.zCoord + 0.05);
            bb = bb.offset(-x, -y, -z);
            RenderUtils.drawBox(bb, targetColor);
        }

        for (Vec3 dirtBlock : dirtBlocks) {
            if (dirtBlock == currentTarget) continue;
            AxisAlignedBB bb = new AxisAlignedBB(dirtBlock.xCoord - 0.05, dirtBlock.yCoord - 0.05, dirtBlock.zCoord - 0.05, dirtBlock.xCoord + 0.05, dirtBlock.yCoord + 0.05, dirtBlock.zCoord + 0.05);
            bb = bb.offset(-x, -y, -z);
            RenderUtils.drawBox(bb, color);
        }
    }

    private Vec3 getBestDirt() {
        Vec3 furthest = null;
        for (Vec3 pos : dirtBlocks) {
            Block block = mc.theWorld.getBlockState(new BlockPos(pos.xCoord, pos.yCoord + 0.1, pos.zCoord)).getBlock();
            pos = new Vec3(pos.xCoord + getBetween(-0.15f, 0.15f), pos.yCoord, pos.zCoord + getBetween(-0.15f, 0.15f));
            if (!(block instanceof net.minecraft.block.BlockLog) && block != Blocks.sapling) {
                if (furthest == null || mc.thePlayer.getDistance(pos.xCoord, pos.yCoord, pos.zCoord) > mc.thePlayer.getDistance(furthest.xCoord, furthest.yCoord, furthest.zCoord))
                    furthest = pos;
            }
        }
        return furthest;
    }

    private float getBetween(float min, float max) {
        return min + (new Random().nextFloat() * (max - min));
    }

    private List<Vec3> getDirts() {
        if (!MayOBeesConfig.dirtDetectionMode) {
            return getRelativeDirts();
        }
        List<Vec3> dirtBlocks = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = 1; z <= 2; z++) {
                BlockPos blockPos = BlockUtils.getRelativeBlockPos(x, 0, z, AngleUtils.getClosest());
                Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                if (block.equals(Blocks.dirt) || block.equals(Blocks.grass)) {
                    dirtBlocks.add(new Vec3(blockPos.getX() + 0.5D, blockPos.getY() + 1, blockPos.getZ() + 0.5D));
                }
            }
        }
        if (dirtBlocks.isEmpty()) {
            LogUtils.error("No dirt blocks found!");
            onDisable();
            return dirtBlocks;
        } else if (dirtBlocks.size() > 4) {
            return getRelativeDirts();
        }
        return dirtBlocks;
    }

    private List<Vec3> getRelativeDirts() {
        List<Vec3> dirtBlocks = new ArrayList<>();
        boolean leftSide = !BlockUtils.getRelativeBlock(-1, 0, 1).equals(Blocks.dirt);
        BlockPos frontLeftDirt;
        BlockPos frontRightDirt;
        BlockPos backLeftDirt;
        BlockPos backRightDirt;
        if (leftSide) {
            frontLeftDirt = BlockUtils.getRelativeBlockPos(0, 0, 2, AngleUtils.getClosest());
            frontRightDirt = BlockUtils.getRelativeBlockPos(1, 0, 2, AngleUtils.getClosest());
            backLeftDirt = BlockUtils.getRelativeBlockPos(0, 0, 1, AngleUtils.getClosest());
            backRightDirt = BlockUtils.getRelativeBlockPos(1, 0, 1, AngleUtils.getClosest());
        } else {
            frontLeftDirt = BlockUtils.getRelativeBlockPos(-1, 0, 2, AngleUtils.getClosest());
            frontRightDirt = BlockUtils.getRelativeBlockPos(0, 0, 2, AngleUtils.getClosest());
            backLeftDirt = BlockUtils.getRelativeBlockPos(-1, 0, 1, AngleUtils.getClosest());
            backRightDirt = BlockUtils.getRelativeBlockPos(0, 0, 1, AngleUtils.getClosest());
        }

        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        switch (facing) {
            case NORTH:
                dirtBlocks.add(new Vec3(frontLeftDirt.getX() + 0.75, frontLeftDirt.getY() + 1, frontLeftDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(frontRightDirt.getX() + 0.25, frontRightDirt.getY() + 1, frontRightDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(backLeftDirt.getX() + 0.75, backLeftDirt.getY() + 1, backLeftDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(backRightDirt.getX() + 0.25, backRightDirt.getY() + 1, backRightDirt.getZ() + 0.25));
                break;
            case SOUTH:
                dirtBlocks.add(new Vec3(frontLeftDirt.getX() + 0.25, frontLeftDirt.getY() + 1, frontLeftDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(frontRightDirt.getX() + 0.75, frontRightDirt.getY() + 1, frontRightDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(backLeftDirt.getX() + 0.25, backLeftDirt.getY() + 1, backLeftDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(backRightDirt.getX() + 0.75, backRightDirt.getY() + 1, backRightDirt.getZ() + 0.75));
                break;
            case WEST:
                dirtBlocks.add(new Vec3(frontLeftDirt.getX() + 0.75, frontLeftDirt.getY() + 1, frontLeftDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(frontRightDirt.getX() + 0.75, frontRightDirt.getY() + 1, frontRightDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(backLeftDirt.getX() + 0.25, backLeftDirt.getY() + 1, backLeftDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(backRightDirt.getX() + 0.25, backRightDirt.getY() + 1, backRightDirt.getZ() + 0.75));
                break;
            case EAST:
                dirtBlocks.add(new Vec3(frontLeftDirt.getX() + 0.25, frontLeftDirt.getY() + 1, frontLeftDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(frontRightDirt.getX() + 0.25, frontRightDirt.getY() + 1, frontRightDirt.getZ() + 0.25));
                dirtBlocks.add(new Vec3(backLeftDirt.getX() + 0.75, backLeftDirt.getY() + 1, backLeftDirt.getZ() + 0.75));
                dirtBlocks.add(new Vec3(backRightDirt.getX() + 0.75, backRightDirt.getY() + 1, backRightDirt.getZ() + 0.25));
                break;
            default:
                LogUtils.error("Invalid facing: " + facing);
                onDisable();
                break;
        }

        return dirtBlocks;
    }

    private void unstuck() {
        LogUtils.warn("[Foraging] I'm stuck! Unstuck process activated");
        stuck = true;
        KeyBindUtils.stopMovement();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) {
            macroState = MacroState.LOOK;
            return;
        }

        if (RotationHandler.getInstance().isRotating()) return;
        if (!waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) return;

        if (isStuck()) return;

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
                if (MayOBeesConfig.foragingMode) {
                    Rotation rotation = new Rotation(AngleUtils.getClosest(), 18.5f);
                    if (RotationHandler.getInstance().shouldRotate(rotation)) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(rotation, (long) (MayOBeesConfig.getRandomizedForagingMacroRotationSpeed() * 1.5f), RotationConfiguration.RotationType.CLIENT, null));
                    } else {
                        macroState = MacroState.PLACE;
                    }
                    break;
                } else {
                    for (Vec3 pos : dirtBlocks) {
                        Block block = mc.theWorld.getBlockState(new BlockPos(pos.xCoord, pos.yCoord + 0.1, pos.zCoord)).getBlock();
                        if (block instanceof BlockLog) {
                            unstuck();
                            return;
                        }
                    }
                    currentTarget = getBestDirt();
                    if (currentTarget != null) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(currentTarget), (long) (MayOBeesConfig.getRandomizedForagingMacroRotationSpeed() * 1.5f), RotationConfiguration.RotationType.CLIENT, null));
                        macroState = MacroState.PLACE;
                    } else {
                        macroState = MacroState.FIND_BONE;
                    }
                }
                return;
            case PLACE:
                if (MayOBeesConfig.foragingMode) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, true);
                    List<Block> sapplings = dirtBlocks.stream().map(pos -> mc.theWorld.getBlockState(new BlockPos(pos.xCoord, pos.yCoord + 0.1, pos.zCoord)).getBlock()).filter(block -> block.equals(Blocks.sapling)).collect(java.util.stream.Collectors.toList());
                    if (Math.abs(mc.thePlayer.motionX) < 0.05 || Math.abs(mc.thePlayer.motionZ) < 0.05) {
                        if (sapplings.size() == 2) {
                            Block[] skull = Arrays.asList(BlockUtils.getRelativeBlock(0, 1, 0), BlockUtils.getRelativeBlock(0, 0, 0)).toArray(new Block[0]);
                            Block[] leftSkull = Arrays.asList(BlockUtils.getRelativeBlock(-1, 1, 0), BlockUtils.getRelativeBlock(-1, 0, 0)).toArray(new Block[0]);
                            Block[] rightSkull = Arrays.asList(BlockUtils.getRelativeBlock(1, 1, 0), BlockUtils.getRelativeBlock(1, 0, 0)).toArray(new Block[0]);
                            if (Arrays.asList(skull).contains(Blocks.skull) && Arrays.asList(leftSkull).contains(Blocks.skull)) {
                                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
                            } else if (Arrays.asList(skull).contains(Blocks.skull) && Arrays.asList(rightSkull).contains(Blocks.skull)) {
                                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
                            } else {
                                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
                            }
                        }
                        if (sapplings.size() == 4) {
                            KeyBindUtils.stopMovement();
                            macroState = MacroState.FIND_BONE;
                        }
                        waitTimer.schedule();
                    }
                    break;
                } else {
                    KeyBindUtils.rightClick();
                    macroState = MacroState.LOOK;
                }
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
                MovingObjectPosition mop = mc.objectMouseOver;
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.sapling)) {
                    KeyBindUtils.rightClick();
                }
                waitTimer.schedule();
                if (MayOBeesConfig.foragingUseRod) {
                    macroState = MacroState.FIND_ROD;
                } else {
                    macroState = MacroState.FIND_TREECAPITATOR;
                }
                break;
            case FIND_ROD:
                int rod = InventoryUtils.getSlotIdOfItemInHotbar("Rod");
                if (rod == -1) {
                    LogUtils.error("No Fishing Rod found in hotbar!");
                    onDisable();
                    break;
                }
                mc.thePlayer.inventory.currentItem = rod;
                waitTimer.schedule();
                macroState = MacroState.THROW_ROD;
                break;
            case THROW_ROD:
                KeyBindUtils.rightClick();
                waitTimer.schedule();
                macroState = MacroState.FIND_TREECAPITATOR;
                break;
            case FIND_TREECAPITATOR:
                int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
                if (treecapitator == -1) {
                    LogUtils.error("No Treecapitator found in hotbar!");
                    onDisable();
                    break;
                }
                waitTimer.schedule();
                mc.thePlayer.inventory.currentItem = treecapitator;
                macroState = MacroState.BREAK;
                break;
            case BREAK:
                if (lastBreakTime != 0 && System.currentTimeMillis() - lastBreakTime < (2000 - (MayOBeesConfig.monkeyLevel / 100f * 2000 * 0.5)))
                    return;
                MovingObjectPosition objectMouseOver = mc.objectMouseOver;
                if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
                    return;
                BlockPos blockPos = objectMouseOver.getBlockPos();
                Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                if (!(block instanceof BlockLog)) return;
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, true);
                waitAfterFinishTimer.schedule();
                macroState = MacroState.SWITCH;
                lastBreakTime = System.currentTimeMillis() + MayOBeesConfig.foragingMacroExtraBreakDelay;
                break;
            case SWITCH:
                if (waitAfterFinishTimer.hasPassed(randomTime) && mc.gameSettings.keyBindAttack.isKeyDown()) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                    if (MayOBeesConfig.foragingMode) {
                        macroState = MacroState.LOOK;
                        waitTimer.schedule();
                        randomTime = (int) (100 + Math.random() * 100);
                        break;
                    }
                }
                if (waitAfterFinishTimer.hasPassed(randomTime + 150)) {
                    macroState = MacroState.LOOK;
                    randomTime = (int) (100 + Math.random() * 100);
                }
                break;
        }

        if (lastState != macroState) {
            lastState = macroState;
            stuckTimer.schedule();
        }
    }

    private boolean isStuck() {
        if (stuck) {
            Vec3 closest = null;
            Vec3 player = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ);
            for (Vec3 dirt : dirtBlocks) {
                Block block = mc.theWorld.getBlockState(new BlockPos(dirt.xCoord, dirt.yCoord + 0.1, dirt.zCoord)).getBlock();
                BlockPos blockPos = new BlockPos(dirt.xCoord, dirt.yCoord + 0.1, dirt.zCoord);
                if ((block instanceof BlockLog) || block == Blocks.sapling) {
                    Vec3 distance = new Vec3(blockPos.getX() + 0.5D + getBetween(-0.1f, 0.1f), (blockPos.getY() + (block.getBlockBoundsMaxY() * 0.75) + getBetween(-0.1f, 0.1f)), blockPos.getZ() + 0.5D + getBetween(-0.1f, 0.1f));
                    if (closest == null || player.squareDistanceTo(distance) <= player.squareDistanceTo(closest))
                        closest = distance;
                }
            }
            int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
            if (treecapitator == -1) {
                LogUtils.error("No Treecapitator found in hotbar!");
                onDisable();
                return true;
            }

            mc.thePlayer.inventory.currentItem = treecapitator;

            MovingObjectPosition mop = mc.objectMouseOver;

            boolean shouldBreak = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.dirt);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, shouldBreak);

            if (closest != null) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(closest), MayOBeesConfig.getRandomizedForagingMacroRotationSpeed(), RotationConfiguration.RotationType.CLIENT, null));
            } else {
                stuck = false;
                macroState = MacroState.LOOK;
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                stuckTimer.schedule();
            }
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (!isRunning()) return;
        onDisable();
    }

    // Skill Tracker
    private final Pattern skillPattern = Pattern.compile("(\\d+):\\s([\\d.]+)%");
    private float lastSkillPercentage = 0;
    public float totalXpGained = 0; // This Session

    @SubscribeEvent
    public void onTablistUpdate(UpdateTablistEvent event) {
        if (!isRunning()) return;
        for (String line : event.tablist) {
            if (!line.contains("Foraging ")) continue;
            Matcher matcher = skillPattern.matcher(line);
            if (!matcher.find()) {
                LogUtils.error("Cannot find skill and level from string: " + line);
                return;
            }
            int foragingLevel = Integer.parseInt(matcher.group(1));
            float skillPercentage = Float.parseFloat(matcher.group(2));
            if (lastSkillPercentage != 0) {
                if (skillPercentage < lastSkillPercentage) {
                    totalXpGained += SkillUtils.xpRequiredToReach[foragingLevel - 1] * ((100 - lastSkillPercentage) / 100f);
                    lastSkillPercentage = 0;
                }
                totalXpGained += SkillUtils.xpRequiredToReach[foragingLevel] * ((skillPercentage - lastSkillPercentage) / 100f);
            }
            lastSkillPercentage = skillPercentage;
            break;
        }
    }
}
