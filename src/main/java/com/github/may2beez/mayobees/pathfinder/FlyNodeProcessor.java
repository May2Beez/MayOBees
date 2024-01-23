package com.github.may2beez.mayobees.pathfinder;

import cc.polyfrost.oneconfig.libs.checker.nullness.qual.Nullable;
import com.github.may2beez.mayobees.util.BlockUtils;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.pathfinder.NodeProcessor;

public class FlyNodeProcessor extends NodeProcessor {

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
        if (BlockUtils.isFree(x, y, z)) {
            return this.openPoint(x, y, z);
        }
        return null;
    }
}