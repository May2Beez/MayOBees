package com.github.may2beez.mayobees.module;

import com.github.may2beez.mayobees.module.impl.combat.ShortbowAura;
import com.github.may2beez.mayobees.module.impl.other.GhostBlocks;
import com.github.may2beez.mayobees.module.impl.player.GiftAura;
import com.github.may2beez.mayobees.module.impl.render.ESP;
import com.github.may2beez.mayobees.module.impl.skills.AlchemyHelper;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class ModuleManager {
    private static ModuleManager instance;

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private final List<IModule> modules = fillModules();

    public List<IModule> fillModules() {
        return Arrays.asList(
                ShortbowAura.getInstance(),
                GhostBlocks.getInstance(),
                GiftAura.getInstance(),
                ESP.getInstance(),
                AlchemyHelper.getInstance()
        );
    }
}
