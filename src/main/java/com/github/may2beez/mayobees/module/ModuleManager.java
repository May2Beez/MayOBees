package com.github.may2beez.mayobees.module;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.impl.combat.AutoClicker;
import com.github.may2beez.mayobees.module.impl.combat.MobAura;
import com.github.may2beez.mayobees.module.impl.dungeon.AutoBoomTNT;
import com.github.may2beez.mayobees.module.impl.other.GhostBlocks;
import com.github.may2beez.mayobees.module.impl.player.*;
import com.github.may2beez.mayobees.module.impl.render.ESP;
import com.github.may2beez.mayobees.module.impl.skills.AlchemyHelper;
import com.github.may2beez.mayobees.module.impl.skills.AutoExperiments;
import com.github.may2beez.mayobees.module.impl.skills.Fishing;
import com.github.may2beez.mayobees.module.impl.skills.foraging.FillChestWithSaplingMacro;
import com.github.may2beez.mayobees.module.impl.skills.foraging.FillForagingSackMacro;
import com.github.may2beez.mayobees.module.impl.skills.foraging.Foraging;
import com.github.may2beez.mayobees.module.impl.utils.AutoBazaar;
import com.github.may2beez.mayobees.util.InventoryUtils;
import com.github.may2beez.mayobees.util.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

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
                AutoClicker.getInstance(),
                MobAura.getInstance(),
                GhostBlocks.getInstance(),
                GiftAura.getInstance(),
                ESP.getInstance(),
                AlchemyHelper.getInstance(),
                Fishing.getInstance(),
                Foraging.getInstance(),
                FillChestWithSaplingMacro.getInstance(),
                FillForagingSackMacro.getInstance(),
                AutoBoomTNT.getInstance(),
                Brush.getInstance(),
                AutoHarp.getInstance(),
                VisitorHelper.getInstance(),
                AutoBazaar.getInstance(),
                NickHider.getInstance(),
                AutoExperiments.getInstance()
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
        LogUtils.info("[" + module.getName() + "] " + (module.isRunning() ? (EnumChatFormatting.GREEN + "Enabled") : (EnumChatFormatting.RED + "Disabled")));
    }


    public void smartToggle() {
        Optional<IModuleActive> activeModule = modules.stream().filter(module -> module instanceof IModuleActive && module.isRunning()).map(module -> (IModuleActive) module).findFirst();
        if (activeModule.isPresent()) {
            toggle(activeModule.get());
            return;
        }

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)
            toggle(GiftAura.getInstance());
        ItemStack heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        if (heldItem == null) return;

//        if (heldItem.getItem() == Items.fishing_rod)
//            toggle(Fishing.getInstance());

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

        if (heldItem.getDisplayName().contains("Aspect") && (heldItem.getDisplayName().contains("Void") || heldItem.getDisplayName().contains("End"))) {
            toggle(Brush.getInstance());
        }
    }
}
