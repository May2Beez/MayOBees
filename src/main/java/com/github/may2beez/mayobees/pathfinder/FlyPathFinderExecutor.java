package com.github.may2beez.mayobees.pathfinder;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.mixin.client.EntityPlayerAccessor;
import com.github.may2beez.mayobees.util.BlockUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.Potion;
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
    private Vec3 lookingTarget;
    private boolean follow;
    private boolean smooth;
    private final FlyNodeProcessor flyNodeProcessor = new FlyNodeProcessor();
    private final PathFinder pathFinder = new PathFinder(flyNodeProcessor);
    @Getter
    private float neededYaw = Integer.MIN_VALUE;
    private final int MAX_DISTANCE = 1500;
    private int ticksAtLastPos = 0;
    private Vec3 lastPosCheck = new Vec3(0, 0, 0);
    private final Clock stuckBreak = new Clock();

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
                    finalRoute.add(new Vec3(pathPoint.xCoord + 0.5f, pathPoint.yCoord + 0.1, pathPoint.zCoord + 0.5f));
                }
                startTime = System.currentTimeMillis();
                if (smooth) {
                    finalRoute = smoothPath(finalRoute);
                }
                this.path = new CopyOnWriteArrayList<>(finalRoute);
                state = State.PATHING;
                LogUtils.debug("Path smoothing took " + (System.currentTimeMillis() - startTime) + "ms");
                if (timeoutTask != null) {
                    timeoutTask.cancel(true);
                }
            }, 0, TimeUnit.MILLISECONDS);
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
                if (traversable(start, end) &&
                        traversable(start.addVector(0, 0.8, 0), end.addVector(0, 0.8, 0)) &&
                        traversable(start.addVector(0, 1, 0), end.addVector(0, 1, 0)) &&
                        traversable(start.addVector(0, 1.8, 0), end.addVector(0, 1.8, 0))) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = path.indexOf(lastValid);
        }

        return smoothed;
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.0, 0.4, -0.4),
            new Vec3(0.0, 0.4, 0.4),
            new Vec3(-0.4, 0.4, 0),
            new Vec3(0.4, 0.4, 0)
    };

    public boolean traversable(Vec3 from, Vec3 to) {
        for (Vec3 offset : BLOCK_SIDE_MULTIPLIERS) {
            Vec3 fromVec = new Vec3(from.xCoord + offset.xCoord, from.yCoord + offset.yCoord, from.zCoord + offset.zCoord);
            Vec3 toVec = new Vec3(to.xCoord + offset.xCoord, to.yCoord + offset.yCoord, to.zCoord + offset.zCoord);
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
        lookingTarget = null;
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
        ticksAtLastPos = 0;
        lastPosCheck = new Vec3(0, 0, 0);
        stuckBreak.reset();
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
        tick = (tick + 1) % 10;

        if (tick != 0) return;
        if (state == State.CALCULATING) return;

        Target target;
        if (lookingTarget != null)
            target = new Target(this.lookingTarget);
        else if (this.targetEntity != null)
            target = new Target(this.targetEntity);
        else
            target = new Target(this.target);

        Vec3 lastElement = path.get(path.size() - 1);
        if (mc.thePlayer.getPositionVector().distanceTo(lastElement) < 1) {
            if (RotationHandler.getInstance().isRotating()) {
                RotationHandler.getInstance().reset();
            }
        } else if (!RotationHandler.getInstance().isRotating() && target.getTarget().isPresent()) {
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    target,
                    (long) (800 + Math.random() * 200),
                    null
            ).randomness(true).followTarget(true));
        }

        if (this.targetEntity != null) {
            findPath(this.targetEntity, this.follow, this.smooth);
        } else {
            findPath(this.target, this.follow, this.smooth);
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
        if (checkForStuck(current)) {
            LogUtils.debug("Stuck");
            stuckBreak.schedule(800);
            float rotationToEscape;
            for (rotationToEscape = 0; rotationToEscape < 360; rotationToEscape += 20) {
                Vec3 escape = current.addVector(Math.cos(Math.toRadians(rotationToEscape)), 0, Math.sin(Math.toRadians(rotationToEscape)));
                if ((traversable(current, escape) &&
                        traversable(current.addVector(0, 0.8, 0), escape.addVector(0, 0.8, 0)) &&
                        traversable(current.addVector(0, 1, 0), escape.addVector(0, 1, 0)) &&
                        traversable(current.addVector(0, 1.8, 0), escape.addVector(0, 1.8, 0)))) {
                    break;
                }
            }
            neededYaw = rotationToEscape;
            if (MayOBeesConfig.flyPathfinderOringoCompatible) {
                List<KeyBinding> keyBindings = new ArrayList<>(KeyBindUtils.getNeededKeyPresses(rotationToEscape));
                KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
            }
            Multithreading.schedule(KeyBindUtils::stopMovement, 500, TimeUnit.MILLISECONDS);
            return;
        }
        if (current.distanceTo(path.get(path.size() - 1)) < 2 || ((targetEntity != null && mc.thePlayer.getDistanceToEntity(targetEntity) < 4 && mc.thePlayer.canEntityBeSeen(targetEntity)) || (target != null && BlockUtils.canBlockBeSeen(new BlockPos(target), 4, new Vec3(0, 0, 0), (block) -> block.equals(new BlockPos(target)))))) {
            stop();
            LogUtils.info("Arrived at destination");
            return;
        }
        if (!mc.thePlayer.capabilities.allowFlying) {
            Vec3 lastWithoutY = new Vec3(path.get(path.size() - 1).xCoord, current.yCoord, path.get(path.size() - 1).zCoord);
            if (current.distanceTo(lastWithoutY) < 1) {
                stop();
                LogUtils.info("Arrived at destination");
                return;
            }
        }
        Vec3 next = getNext(current);

        Rotation rotation = RotationHandler.getInstance().getRotation(current, next);
        List<KeyBinding> keyBindings = new ArrayList<>();
        List<KeyBinding> neededKeys = KeyBindUtils.getNeededKeyPresses(rotation.getYaw());

        // if sprint in pathfinder
        mc.thePlayer.setSprinting(neededKeys.contains(mc.gameSettings.keyBindForward) && current.distanceTo(next) > 5);

        if (MayOBeesConfig.flyPathfinderOringoCompatible) {
            keyBindings.addAll(neededKeys);
            neededYaw = Integer.MIN_VALUE;
        } else {
            neededYaw = rotation.getYaw();
            keyBindings.add(mc.gameSettings.keyBindForward);
        }

        if (mc.thePlayer.capabilities.allowFlying) { // flying + walking
            System.out.println("Difference in y: " + (next.yCoord - current.yCoord));
            if (shouldJump(next, current)) {
                mc.thePlayer.jump();
                System.out.println("Jumping");
            } else if (fly(next, current)) return;
            if ((next.yCoord - current.yCoord > 0.2 || (!traversable(current.addVector(0, -0.05, 0), next) && next.yCoord - current.yCoord > 0.1)) && (next.yCoord - current.yCoord > 0.3 || ((EntityPlayerAccessor) mc.thePlayer).getFlyToggleTimer() == 0)) {
                keyBindings.add(mc.gameSettings.keyBindJump);
                System.out.println("Flying up");
            } else if ((next.yCoord - current.yCoord < -0.2 || (!traversable(current.addVector(0, 0.05, 0), next) && next.yCoord - current.yCoord < -0.1)) && mc.thePlayer.capabilities.isFlying && doesntHaveBlockUnderneath(current)) {
                keyBindings.add(mc.gameSettings.keyBindSneak);
                System.out.println("Sneaking down");
            } else {
                System.out.println("Not jumping or sneaking");
            }
        } else { // only walking
            if (shouldJump(next, current)) {
                mc.thePlayer.jump();
            }
        }

        KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
    }

    private boolean checkForStuck(Vec3 positionVec3) {
        if (this.ticksAtLastPos > 50) {
            this.ticksAtLastPos = 0;
            this.lastPosCheck = positionVec3;
            return positionVec3.squareDistanceTo(this.lastPosCheck) < 2.25;
        }
        double diff = positionVec3.squareDistanceTo(this.lastPosCheck);
        System.out.println("Diff: " + diff);
        if (diff < 2.25) {
            this.ticksAtLastPos++;
            System.out.println(this.ticksAtLastPos);
        } else {
            this.ticksAtLastPos = 0;
            this.lastPosCheck = positionVec3;
        }
        return false;
    }

    private boolean shouldJump(Vec3 next, Vec3 current) {
        int jumpBoost = mc.thePlayer.getActivePotionEffect(Potion.jump) != null ? mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1 : 0;
        return next.yCoord - current.yCoord > 0.25 + jumpBoost * 0.1 && mc.thePlayer.onGround && next.yCoord - current.yCoord < jumpBoost * 0.1 + 0.5;
    }

    private final Clock flyDelay = new Clock();

    private boolean fly(Vec3 next, Vec3 current) {
        if (mc.thePlayer.onGround && next.yCoord - current.yCoord > 0.5) {
            mc.thePlayer.jump();
            flyDelay.schedule(50 + (long) (Math.random() * 50));
            return true;
        } else if (next.yCoord - current.yCoord > 0.5) {
            if (flyDelay.passed()) {
                if (!mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = true;
                    mc.thePlayer.sendPlayerAbilities();
                }
                flyDelay.reset();
            }
            if (!flyDelay.isScheduled()) {
                flyDelay.schedule(50 + (long) (Math.random() * 50));
            }
            return !mc.thePlayer.capabilities.isFlying;
        }
        return false;
    }

    private boolean doesntHaveBlockUnderneath(Vec3 current) {
        return traversable(current, current.addVector(0, -0.5, 0));
    }

    @SubscribeEvent
    public void onDraw(RenderWorldLastEvent event) {
        if (path == null) return;
        RenderManager renderManager = mc.getRenderManager();
        Vec3 current = mc.thePlayer.getPositionVector();
        Vec3 next = getNext(current);
        AxisAlignedBB currenNode = new AxisAlignedBB(current.xCoord - 0.05, current.yCoord - 0.05, current.zCoord - 0.05, current.xCoord + 0.05, current.yCoord + 0.05, current.zCoord + 0.05);
        AxisAlignedBB nextBB = new AxisAlignedBB(next.xCoord - 0.05, next.yCoord - 0.05, next.zCoord - 0.05, next.xCoord + 0.05, next.yCoord + 0.05, next.zCoord + 0.05);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        currenNode = currenNode.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        nextBB = nextBB.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        RenderUtils.drawBox(currenNode, Color.GREEN);
        RenderUtils.drawBox(nextBB, Color.BLUE);
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 from = new Vec3(path.get(i).xCoord, path.get(i).yCoord, path.get(i).zCoord);
            Vec3 to = new Vec3(path.get(i + 1).xCoord, path.get(i + 1).yCoord, path.get(i + 1).zCoord);
            from = from.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            to = to.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            RenderUtils.drawTracer(from, to, Color.RED);
        }
    }

    private Vec3 getNext(Vec3 current) {
        Vec3 next = path.get(0);
        if (path.size() > 2) {
            for (Vec3 vec3 : path) {
                if ((traversable(current, vec3) &&
                        traversable(current.addVector(0, 0.8, 0), vec3.addVector(0, 0.8, 0)) &&
                        traversable(current.addVector(0, 1, 0), vec3.addVector(0, 1, 0)) &&
                        traversable(current.addVector(0, 1.8, 0), vec3.addVector(0, 1.8, 0)))) {
                    next = vec3;
                } else {
                    break;
                }
            }
        } else {
            next = path.get(1);
        }
        return next;
    }

    public enum State {
        NONE,
        CALCULATING,
        FAILED,
        PATHING
    }
}
