package com.github.may2beez.mayobees.pathfinder;

import com.github.may2beez.mayobees.event.BlockChangeEvent;
import com.github.may2beez.mayobees.event.PacketEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.util.BlockUtils;
import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class WorldCache {
    private static WorldCache instance;
    private final ExecutorService executorService = Executors.newScheduledThreadPool(10);
    private final HashMap<Coordinate, Chunk> chunkCache = new HashMap<>();
    private final List<Class<? extends Block>> directionDependentBlocks = Lists.newArrayList(BlockDoor.class, BlockTrapDoor.class, BlockStairs.class);

    public static WorldCache getInstance() {
        if (instance == null) {
            instance = new WorldCache();
        }
        return instance;
    }

    private final HashMap<BlockPos, CacheEntry> worldCache = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) return;

        if (!worldCache.containsKey(event.pos)) return;

        if (!BlockUtils.blockHasCollision(event.pos, event.update, event.update.getBlock())) {
            worldCache.put(event.pos, new CacheEntry(event.update.getBlock(), event.pos, BlockUtils.PathNodeType.OPEN));
        } else {
            worldCache.put(event.pos, new CacheEntry(event.update.getBlock(), event.pos, BlockUtils.PathNodeType.BLOCKED));
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

        if (event.packet instanceof S21PacketChunkData) {
            S21PacketChunkData packet = (S21PacketChunkData) event.packet;
            if (packet.getExtractedSize() == 0) return;
            Chunk c = mc.theWorld.getChunkFromChunkCoords(packet.getChunkX(), packet.getChunkZ());
            Coordinate coordinate = new Coordinate(c.xPosition, c.zPosition);
            cacheChunk(c, coordinate);
        } else if (event.packet instanceof S26PacketMapChunkBulk) {
            S26PacketMapChunkBulk packet = (S26PacketMapChunkBulk) event.packet;
            for (int i = 0; i < packet.getChunkCount(); i++) {
                Chunk c = mc.theWorld.getChunkFromChunkCoords(packet.getChunkX(i), packet.getChunkZ(i));
                Coordinate coordinate = new Coordinate(c.xPosition, c.zPosition);
                cacheChunk(c, coordinate);
            }
        }
    }

    private void cacheChunk(Chunk c, Coordinate coordinate) {
        if (chunkCache.containsKey(coordinate)) return;
        for (int x = 0; x < 16; x++)
            for (int y = 60; y < 100; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x + c.xPosition * 16, y, z + c.zPosition * 16);
                    if (!BlockUtils.blockHasCollision(pos, c.getBlockState(pos), c.getBlock(pos))) {
                        worldCache.put(pos, new CacheEntry(c.getBlock(x, y, z), pos, BlockUtils.PathNodeType.OPEN));
                    } else {
                        worldCache.put(pos, new CacheEntry(c.getBlock(x, y, z), pos, BlockUtils.PathNodeType.BLOCKED));
                    }
                }
        chunkCache.put(coordinate, c);
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

    @Getter
    public static class CacheEntry {
        private final Block block;
        private final BlockPos pos;
        private final BlockUtils.PathNodeType pathNodeType;

        public CacheEntry(Block block, BlockPos pos, BlockUtils.PathNodeType pathNodeType) {
            this.block = block;
            this.pos = pos;
            this.pathNodeType = pathNodeType;
        }
    }
}
