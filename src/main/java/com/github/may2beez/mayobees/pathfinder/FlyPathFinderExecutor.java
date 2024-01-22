package com.github.may2beez.mayobees.pathfinder;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.util.BlockUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FlyPathFinderExecutor {
    private static FlyPathFinderExecutor instance;

    public static FlyPathFinderExecutor getInstance() {
        if (instance == null) {
            instance = new FlyPathFinderExecutor();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Getter
    private State state = State.NONE;

    private int tick = 0;

    private CopyOnWriteArrayList<Vec3> path;
    private Vec3 target;
    private Entity targetEntity;
    private Vec3 lastPositionScanned;
    private Vec3 lastTargetScanned;
    private boolean follow;
    private boolean smooth;
    private FlyNodeProcessor flyNodeProcessor = new FlyNodeProcessor();
    @Getter
    private float neededYaw = Integer.MIN_VALUE;

    public void findPath(Vec3 pos, boolean follow, boolean smooth) {
        state = State.CALCULATING;
        this.follow = follow;
        this.target = pos;
        this.smooth = smooth;
        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            if (flyNodeProcessor == null) flyNodeProcessor = new FlyNodeProcessor();
            PathFinder pathFinder = new PathFinder(flyNodeProcessor);
            List<Vec3> finalRoute = new ArrayList<>();
            PathEntity route = pathFinder.createEntityPathTo(mc.theWorld, mc.thePlayer, new BlockPos(pos), 35);
            if (route == null) {
                state = State.FAILED;
                LogUtils.error("Failed to find path to " + pos);
                double distance = mc.thePlayer.getPositionVector().distanceTo(this.target);
                if (distance > 35) {
                    LogUtils.error("Distance to target is too far. Distance: " + distance + ", Max distance: 35");
                    stop();
                }
                return;
            }
            for (int i = 0; i < route.getCurrentPathLength(); i++) {
                PathPoint pathPoint = route.getPathPointFromIndex(i);
                finalRoute.add(new Vec3(pathPoint.xCoord + 0.5f, pathPoint.yCoord + 0.7f, pathPoint.zCoord + 0.5f));
                LogUtils.debug("Path point " + i + ": " + pathPoint.xCoord + ", " + pathPoint.yCoord + ", " + pathPoint.zCoord);
            }
            if (smooth) {
                finalRoute = findDirectionChangePoints(finalRoute);
            }
            LogUtils.debug("Pathfinding took " + (System.currentTimeMillis() - startTime) + "ms");
            path = new CopyOnWriteArrayList<>(finalRoute);
            state = State.PATHING;
            lastTargetScanned = pos;
            lastPositionScanned = mc.thePlayer.getPositionVector();
        });
    }

    public void findPath(Entity target, boolean follow, boolean smooth) {
        this.targetEntity = target;
        findPath(new Vec3(target.posX, target.posY, target.posZ), follow, smooth);
    }

    private static boolean rayTraceBlocks(BlockPos start, BlockPos end) {
        for (int i = 0; i < 8; i++) {
            Vec3 startVec = getBlockCorners(start)[i];
            Vec3 endVec = getBlockCorners(end)[i];

            MovingObjectPosition result = Minecraft.getMinecraft().theWorld.rayTraceBlocks(
                    new Vec3(startVec.xCoord, startVec.yCoord, startVec.zCoord),
                    new Vec3(endVec.xCoord, endVec.yCoord, endVec.zCoord));
            if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return true;
            }
        }
        return false;
    }

    public static Vec3[] getBlockCorners(BlockPos blockPos) {
        Vec3[] corners = new Vec3[8];
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();

        for (int i = 0; i < 8; i++) {
            double xOffset = (i & 1) == 0 ? 0.1 : 0.9;
            double yOffset = (i & 2) == 0 ? 0.1 : 0.9;
            double zOffset = (i & 4) == 0 ? 0.1 : 0.9;
            corners[i] = new Vec3(x + xOffset, y + yOffset, z + zOffset);
        }

        return corners;
    }

    public static List<Vec3> findDirectionChangePoints(List<Vec3> path) {
        long startTime = System.currentTimeMillis();
        List<Vec3> newDirectionChangePoints = new ArrayList<>();
        if (path.size() < 2) {
            return path;
        }

        newDirectionChangePoints.add(path.get(0));

        for (int i = 0; i < path.size(); i++) {
            BlockPos currentPos = new BlockPos(path.get(i));

            for (int j = i + 1; j < path.size(); j++) {
                BlockPos otherPos = new BlockPos(path.get(j));
                if (rayTraceBlocks(currentPos, otherPos)) {
                    newDirectionChangePoints.add(path.get(j - 1));
                    i = j - 2;
                    break;
                }
            }
        }

        newDirectionChangePoints.add(path.get(path.size() - 1));
        long endTime = System.currentTimeMillis();
        LogUtils.debug("Direction change points found in " + (endTime - startTime) + "ms");
        return newDirectionChangePoints;
    }

    public boolean isPathing() {
        return state == State.PATHING;
    }

    public void stop() {
        path = null;
        target = null;
        targetEntity = null;
        state = State.NONE;
        flyNodeProcessor.resetCache();
        KeyBindUtils.stopMovement();
        neededYaw = Integer.MIN_VALUE;
        RotationHandler.getInstance().reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (target == null) return;
        tick = (tick + 1) % 12;

        if (tick != 0) return;
        if (state == State.CALCULATING) return;
        if (lastTargetScanned != null && lastTargetScanned.distanceTo(target) < 0.5 && lastPositionScanned != null && lastPositionScanned.distanceTo(mc.thePlayer.getPositionVector()) < 0.5)
            return;

        if (this.targetEntity != null) {
            findPath(this.targetEntity, this.follow, this.smooth);
            if (RotationHandler.getInstance().isRotating()) return;
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(this.targetEntity),
                    1_000,
                    null
            ).followTarget(true).randomness(true));
        } else {
            findPath(this.target, this.follow, this.smooth);
            if (RotationHandler.getInstance().isRotating()) return;
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(this.target),
                    1_000,
                    null
            ).followTarget(true).randomness(true));
        }
    }

    @SubscribeEvent
    public void onTickNeededYaw(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (state == State.NONE) return;
        if (state == State.FAILED) {
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        if (path == null) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        Vec3 current = mc.thePlayer.getPositionVector();
        if (current.distanceTo(path.get(path.size() - 1)) < 2 || ((targetEntity != null && mc.thePlayer.getDistanceToEntity(targetEntity) < 4 && mc.thePlayer.canEntityBeSeen(targetEntity)) || (target != null && BlockUtils.canBlockBeSeen(new BlockPos(target), 4, new Vec3(0, 0, 0), (block) -> block.equals(new BlockPos(target)))))) {
            stop();
            LogUtils.info("Arrived at destination");
            return;
        }
        if (path.size() < 2) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        Vec3 closestNode = path.stream().min((v1, v2) -> (int) (v1.distanceTo(current) - v2.distanceTo(current))).orElse(null);
        int index = path.indexOf(closestNode);
        Vec3 next = path.get(Math.max(Math.min(index, path.size() - 2), 0) + 1);
        if (current.distanceTo(next) < 1 && path.size() > index + 2) {
            next = path.get(index + 2);
        }

        if (mc.thePlayer.onGround && next.yCoord - current.yCoord > 0.75) {
            mc.thePlayer.jump();
            Multithreading.schedule(() -> {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }, (long) (50 + Math.random() * 50), TimeUnit.MILLISECONDS);
            return;
        } else if (next.yCoord - current.yCoord > 0.75) {
            if (!mc.thePlayer.capabilities.isFlying) {
                KeyBindUtils.stopMovement();
                neededYaw = Integer.MIN_VALUE;
                return;
            }
        }

        if (next.yCoord - current.yCoord > 0.75) {
            neededYaw = Integer.MIN_VALUE;
            KeyBindUtils.holdThese(mc.gameSettings.keyBindJump, mc.gameSettings.keyBindForward);
        } else if (next.yCoord - current.yCoord < -0.75 && mc.thePlayer.capabilities.isFlying && doesntHaveBlockUnderneath(current)) {
            neededYaw = Integer.MIN_VALUE;
            KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindForward);
        } else {
            Rotation rotation = RotationHandler.getInstance().getRotation(current, next);
            neededYaw = rotation.getYaw();
            KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
        }
    }

    private boolean doesntHaveBlockUnderneath(Vec3 current) {
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(current, current.addVector(0, -1, 0));
        if (result == null) return true;
        return result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @SubscribeEvent
    public void onDraw(RenderWorldLastEvent event) {
        if (path == null) return;
        RenderManager renderManager = mc.getRenderManager();
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 from = new Vec3(path.get(i).xCoord, path.get(i).yCoord, path.get(i).zCoord);
            Vec3 to = new Vec3(path.get(i + 1).xCoord, path.get(i + 1).yCoord, path.get(i + 1).zCoord);
            from = from.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            to = to.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            RenderUtils.drawTracer(from, to, Color.RED);
        }
    }

    public enum State {
        NONE,
        CALCULATING,
        FAILED,
        PATHING
    }
}
