package com.github.may2beez.mayobees.pathfinder;

import cc.polyfrost.oneconfig.libs.checker.nullness.qual.Nullable;
import com.github.may2beez.mayobees.util.BlockUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.pathfinder.NodeProcessor;

import java.util.HashMap;

public class FlyNodeProcessor extends NodeProcessor {
    private final HashMap<BlockPos, PathNodeType> chunkCache = new HashMap<>();

    public void resetCache() {
        chunkCache.clear();
    }

    @Override
    public PathPoint getPathPointTo(Entity entityIn) {
        return this.openPoint(MathHelper.floor_double(entityIn.getEntityBoundingBox().minX), MathHelper.floor_double(entityIn.getEntityBoundingBox().minY + 0.5D), MathHelper.floor_double(entityIn.getEntityBoundingBox().minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(Entity entityIn, double x, double y, double z) {
        return this.openPoint(MathHelper.floor_double(x - (double) (entityIn.width / 2.0F)), MathHelper.floor_double(y - (double) (entityIn.height / 2.0F)), MathHelper.floor_double(z - (double) (entityIn.width / 2.0F)));
    }

    @Override
    public int findPathOptions(PathPoint[] pathOptions, Entity entityIn, PathPoint currentPoint, PathPoint targetPoint, float maxDistance) {
        int i = 0;
        for (EnumFacing enumfacing : EnumFacing.values()) {
            PathPoint pathpoint = this.getAirNode(currentPoint.xCoord + enumfacing.getFrontOffsetX(), currentPoint.yCoord + enumfacing.getFrontOffsetY(), currentPoint.zCoord + enumfacing.getFrontOffsetZ());
            if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance)
                pathOptions[i++] = pathpoint;
        }
        return i;
    }

    @Nullable
    private PathPoint getAirNode(int x, int y, int z) {
        PathNodeType pathnodetype = this.isFree(x, y, z);
        return pathnodetype == PathNodeType.OPEN ? this.openPoint(x, y, z) : null;
    }

    private PathNodeType isFree(int x, int y, int z) {
        BlockPos blockpos = new BlockPos(x, y, z);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        PathNodeType pathnodetype = this.chunkCache.get(blockpos$mutableblockpos.set(x, y, z));
        if (pathnodetype != null) {
            return pathnodetype;
        }
        for (int i = x; i < x + this.entitySizeX; ++i) {
            for (int j = y; j < y + this.entitySizeY; ++j) {
                for (int k = z; k < z + this.entitySizeZ; ++k) {
                    IBlockState iblockStateUnder = this.blockaccess.getBlockState(blockpos.down());
                    IBlockState iblockstate = this.blockaccess.getBlockState(blockpos$mutableblockpos.set(i, j, k));
                    if (notPassable(iblockstate, blockpos) || notPassable(iblockStateUnder, blockpos.down())) {
                        pathnodetype = PathNodeType.BLOCKED;
                        this.chunkCache.put(blockpos, pathnodetype);
                        return pathnodetype;
                    }
                }
            }
        }
        chunkCache.put(blockpos$mutableblockpos, PathNodeType.OPEN);
        return PathNodeType.OPEN;
    }

    private boolean notPassable(IBlockState iblockstate, BlockPos pos) {
        return (iblockstate.getBlock().getMaterial() != Material.air && iblockstate.getBlock().getMaterial() != Material.water) || BlockUtils.blockHasCollision(pos);
    }

    enum PathNodeType {
        OPEN,
        BLOCKED
    }
}