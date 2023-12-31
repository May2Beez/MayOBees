package com.github.may2beez.mayobees.module;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.impl.combat.MobAura;
import com.github.may2beez.mayobees.module.impl.other.GhostBlocks;
import com.github.may2beez.mayobees.module.impl.player.GiftAura;
import com.github.may2beez.mayobees.module.impl.render.ESP;
import com.github.may2beez.mayobees.module.impl.skills.*;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
                MobAura.getInstance(),
                GhostBlocks.getInstance(),
                GiftAura.getInstance(),
                ESP.getInstance(),
                AlchemyHelper.getInstance(),
                Fishing.getInstance(),
                Foraging.getInstance(),
                FillChestWithSaplingMacro.getInstance(),
                FillForagingSackMacro.getInstance()
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
        Optional<IModuleActive> activeModule = modules.stream().filter(module -> module instanceof IModuleActive && module.isRunning()).map(module -> (IModuleActive) module).findFirst();
        if (activeModule.isPresent()) {
            activeModule.get().onDisable();
            LogUtils.info("[" + activeModule.get().getName() + "] Disabled");
            return;
        }

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)
            toggle(GiftAura.getInstance());
        ItemStack heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        if (heldItem == null) return;

        if (heldItem.getItem() == Items.fishing_rod)
            toggle(Fishing.getInstance());

        if (!MayOBeesConfig.mobAuraItemName.isEmpty() && heldItem.getDisplayName().contains(MayOBeesConfig.mobAuraItemName))
            toggle(MobAura.getInstance());

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND) {
            if (heldItem.getDisplayName().contains("Treecapitator"))
                toggle(Foraging.getInstance());

            if ((heldItem.getDisplayName().contains("Abiphone") || heldItem.getDisplayName().contains("Foraging Sack") || heldItem.getDisplayName().contains("Sapling")) && InventoryUtils.hasItemInHotbar("Treecapitator")) {
                toggle(FillChestWithSaplingMacro.getInstance());
            }
        }

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
            if (heldItem.getDisplayName().contains("Foraging Sack")) {
                toggle(FillForagingSackMacro.getInstance());
            }
        }
    }
}
