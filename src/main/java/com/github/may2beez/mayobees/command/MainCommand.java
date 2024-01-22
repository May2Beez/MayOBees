package com.github.may2beez.mayobees.command;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.github.may2beez.mayobees.MayOBees;
import com.github.may2beez.mayobees.pathfinder.FlyPathFinderExecutor;
import com.github.may2beez.mayobees.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.Vec3;

import java.util.List;

@Command(value = "m2b", description = "MayOBees Command", aliases = {"mayobees"})
public class MainCommand {

    @Main
    private void main() {
        MayOBees.CONFIG.openGui();
    }

    @SubCommand
    private void findpath2(String name, boolean follow, boolean smooth) {
        List<Entity> entities = Minecraft.getMinecraft().theWorld.getEntities(EntityLiving.class, entity -> entity.getName().equals(name));
        if (entities.isEmpty()) {
            LogUtils.error("Could not find entity with name " + name);
            return;
        }
        entities.sort((e1, e2) -> (int) (e1.getDistanceToEntity(Minecraft.getMinecraft().thePlayer) - e2.getDistanceToEntity(Minecraft.getMinecraft().thePlayer)));
        Entity entity = entities.get(0);
        FlyPathFinderExecutor.getInstance().findPath(entity, follow, smooth);
    }

    @SubCommand
    private void findpath2(String name, boolean follow) {
        findpath2(name, follow, false);
    }

    @SubCommand
    private void findpath(int x, int y, int z) {
        findpath(x, y, z, false, false);
    }

    @SubCommand
    private void findpath(int x, int y, int z, boolean follow) {
        findpath(x, y, z, follow, false);
    }

    @SubCommand()
    private void findpath(int x, int y, int z, boolean follow, boolean smooth) {
        Vec3 blockpos = new Vec3(x, y, z);
        FlyPathFinderExecutor.getInstance().findPath(blockpos, follow, smooth);
    }

    @SubCommand
    private void stoppath() {
        FlyPathFinderExecutor.getInstance().stop();
    }
}
