package com.github.may2beez.mayobees.module.impl.combat;

import com.github.may2beez.mayobees.module.IModule;
import lombok.Getter;
import net.minecraft.client.Minecraft;

public class MobKiller implements IModule {
    private static MobKiller instance;
    public static MobKiller getInstance() {
        if (instance == null) {
            instance = new MobKiller();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Mob Killer";
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    public static boolean hasTarget = false;

    @Override
    public boolean isRunning() {
        return false;
    }

    public void start() {}

    public void stop() {}

    public void setTarget(String... mobsNames) {}
}
