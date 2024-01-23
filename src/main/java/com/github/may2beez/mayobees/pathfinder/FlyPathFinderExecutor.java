package com.github.may2beez.mayobees.pathfinder;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.util.BlockUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class FlyPathFinderExecutor {
    private static FlyPathFinderExecutor instance;

    public static FlyPathFinderExecutor getInstance() {
        if (instance == null) {
            instance = new FlyPathFinderExecutor();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pathfinderTask;
    private ScheduledFuture<?> timeoutTask;
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
    private final FlyNodeProcessor flyNodeProcessor = new FlyNodeProcessor();
    private final PathFinder pathFinder = new PathFinder(flyNodeProcessor);
    @Getter
    private float neededYaw = Integer.MIN_VALUE;
    private final int MAX_DISTANCE = 1500;

    public void findPath(Vec3 pos, boolean follow, boolean smooth) {
        state = State.CALCULATING;
        this.follow = follow;
        this.target = pos;
        this.smooth = smooth;
        LogUtils.debug("Cache size: " + WorldCache.getInstance().getWorldCache().size());
        try {
            System.out.println("Starting pathfinding");
            pathfinderTask = executor.schedule(() -> {
                long startTime = System.currentTimeMillis();
                int maxDistance = Math.min(MAX_DISTANCE, (int) mc.thePlayer.getPositionVector().distanceTo(pos) + 5);
                LogUtils.debug("Max distance: " + maxDistance);
                PathEntity route = pathFinder.createEntityPathTo(mc.theWorld, mc.thePlayer, new BlockPos(pos), maxDistance);
                LogUtils.debug("Pathfinding took " + (System.currentTimeMillis() - startTime) + "ms");
                if (route == null) {
                    state = State.FAILED;
                    LogUtils.error("Failed to find path to " + pos);
                    double distance = mc.thePlayer.getPositionVector().distanceTo(this.target);
                    if (distance > maxDistance) {
                        LogUtils.error("Distance to target is too far. Distance: " + distance + ", Max distance: " + maxDistance);
                        stop();
                    }
                    return;
                }
                List<Vec3> finalRoute = new ArrayList<>();
                for (int i = 0; i < route.getCurrentPathLength(); i++) {
                    PathPoint pathPoint = route.getPathPointFromIndex(i);
                    finalRoute.add(new Vec3(pathPoint.xCoord + 0.5f, pathPoint.yCoord + 0.25f, pathPoint.zCoord + 0.5f));
                }
                startTime = System.currentTimeMillis();
                if (smooth) {
                    finalRoute = smoothPath(finalRoute);
                }
                this.path = new CopyOnWriteArrayList<>(finalRoute);
                state = State.PATHING;
                lastTargetScanned = pos;
                lastPositionScanned = mc.thePlayer.getPositionVector();
                LogUtils.debug("Path smoothing took " + (System.currentTimeMillis() - startTime) + "ms");
                if (timeoutTask != null) {
                    timeoutTask.cancel(true);
                }
            }, 0, TimeUnit.MILLISECONDS);
            System.out.println("Starting timeout");
            timeoutTask = executor.schedule(() -> {
                System.out.println("Timeout");
                if (state == State.CALCULATING) {
                    LogUtils.error("Pathfinding took too long");
                    if (pathfinderTask != null) {
                        pathfinderTask.cancel(true);
                    }
                }
            }, 1_500, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LogUtils.error("Pathfinding took too long");
            RotationHandler.getInstance().reset();
        }
    }

    public void findPath(Entity target, boolean follow, boolean smooth) {
        this.targetEntity = target;
        findPath(new Vec3(target.posX, target.posY, target.posZ), follow, smooth);
    }

    public List<Vec3> smoothPath(List<Vec3> path) {
        if (path.size() < 2) {
            return path;
        }
        List<Vec3> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        int lowerIndex = 0;
        while (lowerIndex < path.size() - 2) {
            Vec3 start = path.get(lowerIndex);
            Vec3 lastValid = path.get(lowerIndex + 1);
            for (int upperIndex = lowerIndex + 2; upperIndex < path.size(); upperIndex++) {
                Vec3 end = path.get(upperIndex);
                if (start.distanceTo(end) > 10) {
                    break;
                }
                if (traversable(start, end) && traversable(start.addVector(0, 1, 0), end.addVector(0, 1, 0)) && traversable(start.addVector(0 , 1.3, 0), end.addVector(0, 1.3, 0))) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = path.indexOf(lastValid);
        }

        return smoothed;
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0.5, 0.1),
            new Vec3(0.5, 0.5, 0.9),
            new Vec3(0.1, 0.5, 0.5),
            new Vec3(0.9, 0.5, 0.5)
    };

    public boolean traversable(Vec3 from, Vec3 to) {
        for (Vec3 offset : BLOCK_SIDE_MULTIPLIERS) {
            Vec3 fromVec = new Vec3(from.xCoord + 0.5f + offset.xCoord, from.yCoord + 0.5 + offset.yCoord, from.zCoord + 0.5 + offset.zCoord);
            Vec3 toVec = new Vec3(to.xCoord + 0.5 + offset.xCoord, to.yCoord + 0.5 + offset.yCoord, to.zCoord + 0.5 + offset.zCoord);
            MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(fromVec, toVec, false, true, false);

            if (trace != null) {
                return false;
            }
        }

        return true;
    }

    public boolean isPathing() {
        return state == State.PATHING;
    }

    public void stop() {
        path = null;
        target = null;
        targetEntity = null;
        state = State.NONE;
        KeyBindUtils.stopMovement();
        neededYaw = Integer.MIN_VALUE;
        minimumDelayBetweenSpaces.reset();
        RotationHandler.getInstance().reset();
        if (pathfinderTask != null) {
            pathfinderTask.cancel(true);
            pathfinderTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (state == State.PATHING) {
            stop();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (target == null) return;
        tick = (tick + 1) % 5;

        if (tick != 0) return;
        if (state == State.CALCULATING) return;
//        if (lastTargetScanned != null && lastTargetScanned.distanceTo(target) < 0.5 && lastPositionScanned != null && lastPositionScanned.distanceTo(mc.thePlayer.getPositionVector()) < 0.5)
//            return;

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

    private final Clock minimumDelayBetweenSpaces = new Clock();

    @SubscribeEvent
    public void onTickNeededYaw(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (state == State.NONE) return;
        if (state == State.FAILED) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        if (path == null) {
            return;
        }
        Vec3 current = mc.thePlayer.getPositionVector();
        if (current.distanceTo(path.get(path.size() - 1)) < 2 || ((targetEntity != null && mc.thePlayer.getDistanceToEntity(targetEntity) < 4 && mc.thePlayer.canEntityBeSeen(targetEntity)) || (target != null && BlockUtils.canBlockBeSeen(new BlockPos(target), 4, new Vec3(0, 0, 0), (block) -> block.equals(new BlockPos(target)))))) {
            stop();
            LogUtils.info("Arrived at destination");
            return;
        }
        Vec3 closestNode = path.stream().min(Comparator.comparingDouble((Vec3 v) -> v.distanceTo(current))).orElse(path.get(0));
        int index = path.indexOf(closestNode);
        Vec3 next = path.get(Math.min(index + 1, path.size() - 1));
        if (current.distanceTo(next) < 1 && path.size() > index + 2 && traversable(current, next)) {
            next = path.get(index + 2);
        }

        if (mc.thePlayer.onGround && next.yCoord - current.yCoord > 0.5) {
            mc.thePlayer.jump();
            Multithreading.schedule(() -> {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }, (long) (50 + Math.random() * 50), TimeUnit.MILLISECONDS);
            return;
        } else if (next.yCoord - current.yCoord > 0.5) {
            if (!mc.thePlayer.capabilities.isFlying) {
                return;
            }
        }

        Rotation rotation = RotationHandler.getInstance().getRotation(current, next);
        List<KeyBinding> keyBindings = new ArrayList<>();
        List<KeyBinding> neededKeys = KeyBindUtils.getNeededKeyPresses(rotation.getYaw());
        // if sprint in pathfinder
        mc.thePlayer.setSprinting(neededKeys.contains(mc.gameSettings.keyBindForward));
        float distanceXZ = (float) Math.sqrt(Math.pow(next.xCoord - current.xCoord, 2) + Math.pow(next.zCoord - current.zCoord, 2));
        if (MayOBeesConfig.flyPathfinderOringoCompatible) {
            keyBindings.addAll(neededKeys);
            neededYaw = Integer.MIN_VALUE;
        } else {
            neededYaw = rotation.getYaw();
            keyBindings.add(mc.gameSettings.keyBindForward);
        }
        if (distanceXZ < 0.3) {
            keyBindings.remove(mc.gameSettings.keyBindForward);
        }
        if (next.yCoord - current.yCoord > 0.1) {
            keyBindings.add(mc.gameSettings.keyBindJump);
            System.out.println("Jumping");
        } else if (next.yCoord - current.yCoord < -0.1 && mc.thePlayer.capabilities.isFlying && doesntHaveBlockUnderneath(current)) {
            keyBindings.add(mc.gameSettings.keyBindSneak);
            System.out.println("Sneaking");
        } else {
            System.out.println("Not jumping or sneaking");
        }
        KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
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
        Vec3 current = mc.thePlayer.getPositionVector();
        Vec3 closestNode = path.stream().min(Comparator.comparingDouble(
                (Vec3 v) -> v.distanceTo(current))).orElse(path.get(0));
        int index = path.indexOf(closestNode);
        Vec3 next = path.get(Math.min(index + 1, path.size() - 1));
        if (current.distanceTo(next) < 1 && path.size() > index + 2 && traversable(current, next)) {
            next = path.get(index + 2);
        }
        AxisAlignedBB closest = new AxisAlignedBB(closestNode.xCoord - 0.05, closestNode.yCoord - 0.05, closestNode.zCoord - 0.05, closestNode.xCoord + 0.05, closestNode.yCoord + 0.05, closestNode.zCoord + 0.05);
        AxisAlignedBB nextBB = new AxisAlignedBB(next.xCoord - 0.05, next.yCoord - 0.05, next.zCoord - 0.05, next.xCoord + 0.05, next.yCoord + 0.05, next.zCoord + 0.05);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        closest = closest.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        nextBB = nextBB.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        RenderUtils.drawBox(closest, Color.GREEN);
        RenderUtils.drawBox(nextBB, Color.BLUE);
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
