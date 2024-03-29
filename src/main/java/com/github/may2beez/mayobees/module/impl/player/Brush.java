package com.github.may2beez.mayobees.module.impl.player;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.module.ModuleManager;
import com.github.may2beez.mayobees.util.BlockUtils;
import com.github.may2beez.mayobees.util.KeyBindUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.helper.Clock;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Brush implements IModuleActive {
    private static Brush instance;
    @Getter
    private final List<WaypointList> waypoints = new ArrayList<>();
    private final Clock delay = new Clock();
    private boolean teleported = true;
    @Setter
    @Getter
    private int currentWaypointList = -1;

    public static Brush getInstance() {
        if (instance == null) {
            instance = new Brush();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean backwards = false;
    private boolean started = false;

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public String getName() {
        return "Brush";
    }

    @Override
    public void onEnable() {
        enabled = true;
        backwards = mc.gameSettings.keyBindSneak.isKeyDown();
        teleported = true;
        started = false;
        KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
    }

    @Override
    public void onDisable() {
        enabled = false;
        KeyBindUtils.stopMovement();
    }

    public void addWaypoint() {
        BlockPos pos = BlockUtils.getRelativeBlockPos(0, -1, 0);
        if (waypoints.isEmpty()) {
            addNewBrushWaypointList();
        }
        waypoints.get(currentWaypointList).addWaypoint(pos);
        LogUtils.info("[Brush] Added waypoint at " + pos + " to list no. " + (waypoints.size() - 1) + "!");
    }

    public void removeWaypoint() {
        BlockPos pos = BlockUtils.getRelativeBlockPos(0, -1, 0);
        for (WaypointList list : waypoints) {
            if (list.contains(pos)) {
                list.removeWaypoint(pos);
                LogUtils.info("[Brush] Removed waypoint at " + pos + " from list no. " + (waypoints.indexOf(list)) + "!");
                return;
            }
        }
    }

    public void addNewBrushWaypointList() {
        waypoints.add(new WaypointList());
        currentWaypointList = waypoints.size() - 1;
        LogUtils.info("[Brush] Added new waypoint list!");
    }

    public void clearWaypoints() {
        waypoints.clear();
        LogUtils.info("[Brush] Cleared waypoints!");
    }

    private final Color blockedVisionColor = new Color(255, 0, 0, 125);

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        for (WaypointList list : waypoints) {
            int index = 0;
            BlockPos prev = null;
            for (BlockPos pos : list.waypoints) {
                RenderUtils.drawBlockBox(pos, list.color);
                RenderUtils.drawText("List " + (waypoints.indexOf(list)) + ", Block " + index, pos.getX() + 0.5f, pos.getY() + 1.5f, pos.getZ() + 0.5f, 0.5f);

                if (prev != null) {
                    Vec3 direction = new Vec3(pos.getX() - prev.getX(), pos.getY() - prev.getY(), pos.getZ() - prev.getZ());
                    Vec3 from = new Vec3(prev.getX() + 0.5, prev.getY() + 0.5, prev.getZ() + 0.5);
                    from = from.add(direction.normalize());
                    Vec3 to = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(from, to, false, true, false);
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !mop.getBlockPos().equals(pos) && !mop.getBlockPos().equals(prev)) {
                        RenderUtils.drawBlockBox(mop.getBlockPos(), blockedVisionColor);
                    }
                }

                prev = pos;
                index++;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (RotationHandler.getInstance().isRotating()) return;
        if (delay.isScheduled() && !delay.passed()) return;
        if (!teleported) return;

        KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);

        BlockPos pos = BlockUtils.getRelativeBlockPos(0, -1, 0);
        Optional<WaypointList> list = waypoints.stream().filter(waypointList -> waypointList.contains(pos)).findFirst();

        if (!list.isPresent()) {
            LogUtils.error("[Brush] No waypoint list found for block at " + pos + "!");
            ModuleManager.getInstance().toggle(this);
            return;
        }

        int currentIndex = list.get().waypoints.indexOf(pos);

        if (currentIndex == -1) {
            LogUtils.error("[Brush] No waypoint found for block at " + pos + "!");
            ModuleManager.getInstance().toggle(this);
            return;
        }

        if (backwards) {
            if (currentIndex == 0) {
                if (!started) {
                    backwards = false;
                    return;
                }
                LogUtils.info("[Brush] Reached start of waypoint list!");
                ModuleManager.getInstance().toggle(this);
                return;
            }
        } else {
            if (currentIndex == list.get().waypoints.size() - 1) {
                if (!started) {
                    backwards = true;
                    return;
                }
                LogUtils.info("[Brush] Reached end of waypoint list!");
                ModuleManager.getInstance().toggle(this);
                return;
            }
        }

        BlockPos nextPos;
        Vec3 nextVec;

        if (backwards) {
            nextPos = list.get().waypoints.get(currentIndex - 1);
        } else {
            nextPos = list.get().waypoints.get(currentIndex + 1);
        }

        nextVec = new Vec3(nextPos.getX() + 0.5, nextPos.getY() + 0.75, nextPos.getZ() + 0.5);

        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1), nextVec, false, true, false);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mop.getBlockPos().equals(nextPos)) {
            LogUtils.warn("[Brush] Not visible, trying to find visible point!");
            List<Vec3> visiblePoints = BlockUtils.getVisiblePoints(nextPos, true);
            if (visiblePoints.isEmpty()) {
                LogUtils.error("[Brush] Obstacle in the way!");
                ModuleManager.getInstance().toggle(this);
                return;
            }
            nextVec = visiblePoints.get(0);
        }

        Rotation rotation = RotationHandler.getInstance().getRotation(nextVec);
        if (RotationHandler.getInstance().shouldRotate(rotation)) {
            long time = (long) (300 + Math.random() * 100);
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(nextVec),
                    time,
                    null
            ));
            delay.schedule(time + MayOBeesConfig.delayBeforeTP);
            return;
        }

        if (mc.thePlayer.getDistance(nextVec.xCoord, nextVec.yCoord, nextVec.zCoord) > 61) {
            LogUtils.error("[Brush] Next waypoint is too far away!");
            ModuleManager.getInstance().toggle(this);
            return;
        }


        KeyBindUtils.rightClick();
        started = true;
        delay.schedule(100);
        teleported = false;
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (!enabled) return;
        if (teleported) return;
        if (event.packet instanceof S08PacketPlayerPosLook) {
            teleported = true;
            LogUtils.debug("[Brush] Teleported!");
            delay.schedule(MayOBeesConfig.delayAfterTP);
        }
    }

    public static class WaypointList {
        @Getter
        private final List<BlockPos> waypoints = new ArrayList<>();
        private final Color color = new Color((int) (Math.random() * 255f), (int) (Math.random() * 255f), (int) (Math.random() * 255f), 125);

        public void addWaypoint(BlockPos pos) {
            waypoints.add(pos);
        }

        public void removeWaypoint(BlockPos pos) {
            waypoints.remove(pos);
        }

        public boolean contains(BlockPos pos) {
            return waypoints.contains(pos);
        }
    }
}
