package com.github.may2beez.mayobees.pathfinder;

import com.github.may2beez.mayobees.event.BlockChangeEvent;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.util.BlockUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class WorldCache {
    private static WorldCache instance;
    private final ExecutorService executorService = Executors.newScheduledThreadPool(10);
    private final HashMap<Coordinate, Chunk> chunkCache = new HashMap<>();

    public static WorldCache getInstance() {
        if (instance == null) {
            instance = new WorldCache();
        }
        return instance;
    }

    private final HashMap<BlockPos, BlockUtils.PathNodeType> worldCache = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (mc.theWorld == null) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) return;

        if (!worldCache.containsKey(event.pos)) return;

        if (BlockUtils.isFree(event.pos.getX(), event.pos.getY(), event.pos.getZ(), true)) {
            worldCache.put(event.pos, BlockUtils.PathNodeType.OPEN);
        } else {
            worldCache.put(event.pos, BlockUtils.PathNodeType.BLOCKED);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        worldCache.clear();
        chunkCache.clear();
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.theWorld == null) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) return;

        if (event.packet instanceof S21PacketChunkData) {
            S21PacketChunkData packet = (S21PacketChunkData) event.packet;
            if (packet.getExtractedSize() == 0) return;
            Chunk c = mc.theWorld.getChunkFromChunkCoords(packet.getChunkX(), packet.getChunkZ());
            Coordinate coordinate = new Coordinate(packet.getChunkX(), packet.getChunkZ());
            if (chunkCache.containsKey(coordinate)) return;
            for (int x = 0; x < 16; x++)
                for (int y = 60; y < 100; y++)
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(x + c.xPosition * 16, y, z + c.zPosition * 16);
                        if (BlockUtils.isFree(pos.getX(), pos.getY(), pos.getZ(), true)) {
                            worldCache.put(pos, BlockUtils.PathNodeType.OPEN);
                        } else {
                            worldCache.put(pos, BlockUtils.PathNodeType.BLOCKED);
                        }
                    }
            chunkCache.put(coordinate, c);
        }
    }

    private static class Coordinate {
        private final int x;
        private final int z;

        public Coordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Coordinate)) return false;
            Coordinate c = (Coordinate) obj;
            return c.x == x && c.z == z;
        }

        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }
}
