package com.github.may2beez.mayobees.module;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.impl.combat.ShortbowAura;
import com.github.may2beez.mayobees.module.impl.other.GhostBlocks;
import com.github.may2beez.mayobees.module.impl.player.GiftAura;
import com.github.may2beez.mayobees.module.impl.render.ESP;
import com.github.may2beez.mayobees.module.impl.skills.AlchemyHelper;
import com.github.may2beez.mayobees.module.impl.skills.FillChestWithSaplingMacro;
import com.github.may2beez.mayobees.module.impl.skills.Fishing;
import com.github.may2beez.mayobees.module.impl.skills.Foraging;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

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
                AlchemyHelper.getInstance(),
                Fishing.getInstance(),
                Foraging.getInstance(),
                FillChestWithSaplingMacro.getInstance()
        );
    }

    public void disableAll() {
        modules.forEach(module -> {
            if (module.isRunning() && module instanceof IModuleActive)
                ((IModuleActive) module).onDisable();
        });
    }

    public void toggle(IModuleActive module) {
        if (module.isRunning())
            module.onDisable();
        else
            module.onEnable();
        LogUtils.info("[" + module.getName() + "] " + (module.isRunning() ? "Enabled" : "Disabled"));
    }

    public void smartToggle() {
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)
            toggle(GiftAura.getInstance());
        ItemStack heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() == Items.fishing_rod)
            toggle(Fishing.getInstance());
        if (heldItem != null && heldItem.getDisplayName().contains(MayOBeesConfig.shortBowAuraItemName))
            toggle(ShortbowAura.getInstance());
        if (heldItem != null && heldItem.getDisplayName().contains("Sapling") || heldItem != null && heldItem.getDisplayName().contains("Treecapitator")
                && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            toggle(Foraging.getInstance());
        if (heldItem != null && heldItem.getDisplayName().contains("Abiphone") && InventoryUtils.hasItemInHotbar("Treecapitator") && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND) {
            toggle(FillChestWithSaplingMacro.getInstance());
        }
    }
}
