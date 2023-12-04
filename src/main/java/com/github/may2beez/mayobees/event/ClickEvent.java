package com.github.may2beez.mayobees.event;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ClickEvent extends Event {
    public Entity entity;
    public BlockPos blockPos;
    public Block block;

    protected ClickEvent() {
    }

    protected ClickEvent(Entity entity) {
        this.entity = entity;
    }

    protected ClickEvent(BlockPos blockPos) {
        this.blockPos = blockPos;
        this.block = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).getBlock();
    }

    public static class Left extends ClickEvent {
        public Left(Entity entity) {
            super(entity);
        }
        public Left(BlockPos blockPos) {
            super(blockPos);
        }
        public Left() {
            super();
        }
    }

    public static class Right extends ClickEvent {
        public Right(Entity entity) {
            super(entity);
        }
        public Right(BlockPos blockPos) {
            super(blockPos);
        }
        public Right() {
            super();
        }
    }

    public static class Middle extends ClickEvent {
        public Middle(Entity entity) {
            super(entity);
        }
        public Middle(BlockPos blockPos) {
            super(blockPos);
        }
        public Middle() {
            super();
        }
    }
}
