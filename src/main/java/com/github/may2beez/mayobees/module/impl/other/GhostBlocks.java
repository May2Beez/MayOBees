package com.github.may2beez.mayobees.module.impl.other;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.module.IModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class GhostBlocks implements IModule {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static GhostBlocks instance;
    private final CopyOnWriteArrayList<GhostBlock> clickedBlocks = new CopyOnWriteArrayList<>();

    public static GhostBlocks getInstance() {
        if (instance == null) {
            instance = new GhostBlocks();
        }
        return instance;
    }

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.enableGhostBlocks;
    }

    @SubscribeEvent
    public void onMiddleClick(ClickEvent.Middle event) {
        if (event.block == null) return;
        if (!isRunning()) return;

        if (MayOBeesConfig.ghostBlocksOnlyWhileHoldingStonk) {
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem == null || !heldItem.getDisplayName().contains("Stonk")) return;
        }

        if (clickedBlocks.stream().noneMatch(ghostBlock -> ghostBlock.pos.equals(event.blockPos))) {
            GhostBlock gb = new GhostBlock(event.blockPos, mc.theWorld.getBlockState(event.blockPos));
            clickedBlocks.add(gb);
            mc.theWorld.setBlockToAir(event.blockPos);
            Multithreading.schedule(() -> {
                if (!clickedBlocks.contains(gb)) return;
                mc.theWorld.setBlockState(gb.pos, gb.previousBlockState);
                clickedBlocks.remove(gb);
            }, MayOBeesConfig.ghostBlocksDuration, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        clickedBlocks.clear();
    }

    private static class GhostBlock {
        private final BlockPos pos;
        private final IBlockState previousBlockState;

        public GhostBlock(BlockPos pos, IBlockState previousBlockState) {
            this.pos = pos;
            this.previousBlockState = previousBlockState;
        }
    }
}
