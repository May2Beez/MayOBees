package com.github.may2beez.mayobees.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import com.github.may2beez.mayobees.module.ModuleManager;
import com.github.may2beez.mayobees.module.impl.combat.AutoClicker;
import com.github.may2beez.mayobees.module.impl.other.Dev;
import com.github.may2beez.mayobees.module.impl.player.GiftAura;
import com.github.may2beez.mayobees.module.impl.render.ESP;
import com.github.may2beez.mayobees.module.impl.skills.AlchemyHelper;
import com.github.may2beez.mayobees.util.LogUtils;
import org.lwjgl.input.Keyboard;

public class MayOBeesConfig extends Config {

    //<editor-fold desc="COMBAT">

    @Dropdown(
            name = "Mode of AutoClicker",
            description = "The mode of the auto clicker",
            category = "Combat",
            subcategory = "Auto Clicker",
            options = {"Holding Selected Mouse Button", "Holding Keybind", "Non-Stop"}
    )
    public static int autoClickerMode = 0;

    @DualOption(
            name = "AutoClicker Type",
            description = "The type of auto clicker",
            category = "Combat",
            subcategory = "Auto Clicker",
            left = "Left Click",
            right = "Right Click"
    )
    public static boolean autoClickerType = false;

    @KeyBind(
            name = "AutoClicker Keybind",
            description = "The keybind to use for the auto clicker",
            category = "Combat",
            subcategory = "Auto Clicker"
    )
    public static OneKeyBind autoClickerKeybind = new OneKeyBind(Keyboard.KEY_NONE);

    @Slider(
            name = "AutoClicker Minimum CPS",
            description = "The CPS of the auto clicker",
            category = "Combat",
            subcategory = "Auto Clicker - Delays",
            min = 1,
            max = 20
    )
    public static int autoClickerMinCPS = 10;

    @Slider(
            name = "AutoClicker Maximum CPS",
            description = "The CPS of the auto clicker",
            category = "Combat",
            subcategory = "Auto Clicker - Delays",
            min = 1,
            max = 20
    )
    public static int autoClickerMaxCPS = 15;


    @Dropdown(
            name = "Preset",
            description = "The preset to use",
            category = "Combat",
            subcategory = "Mob Aura - Presets",
            options = {"Melee", "Reach Melee", "Shortbow", "Terminator"}
    )
    public static int mobAuraPreset = 0;

    @Button(
            name = "",
            text = "Apply Preset",
            description = "Applies the preset",
            category = "Combat",
            subcategory = "Mob Aura - Presets"
    )
    public static void applyPreset() {
        switch (mobAuraPreset) {
            case 0:
                mobAuraRange = 3f;
                mobAuraFOV = 90;
                mobAuraRotationSpeed = 300;
                mobAuraRotationSpeedRandomizer = 100;
                mobAuraCooldown = 300;
                mobAuraCooldownRandomizer = 100;
                mobAuraMouseButton = 0;
                mobAuraRotationMode = true;
                mobAuraAttackMobs = true;
                mobAuraAttackUntilDead = true;
                mobAuraItemName = "Sword";
                break;
            case 1:
                mobAuraRange = 4.5f;
                mobAuraFOV = 120;
                mobAuraRotationSpeed = 300;
                mobAuraRotationSpeedRandomizer = 100;
                mobAuraCooldown = 500;
                mobAuraCooldownRandomizer = 100;
                mobAuraMouseButton = 0;
                mobAuraRotationMode = true;
                mobAuraAttackMobs = true;
                mobAuraAttackUntilDead = true;
                mobAuraItemName = "Sword";
                break;
            case 2:
                mobAuraRange = 25f;
                mobAuraFOV = 80;
                mobAuraRotationSpeed = 300;
                mobAuraRotationSpeedRandomizer = 100;
                mobAuraCooldown = 500;
                mobAuraCooldownRandomizer = 100;
                mobAuraMouseButton = 2;
                mobAuraRotationMode = false;
                mobAuraAttackMobs = true;
                mobAuraAttackUntilDead = false;
                mobAuraItemName = "Bow";
                break;
        }
    }

    @Text(
            name = "Mob Aura Item's Name",
            description = "The name of the item to use for the mob aura",
            category = "Combat",
            subcategory = "Mob Aura",
            size = 2
    )
    public static String mobAuraItemName = "Sword";

    @Switch(
            name = "Mob Aura Attack Animals",
            description = "Whether or not to attack mobs",
            category = "Combat",
            subcategory = "Mob Aura"
    )
    public static boolean mobAuraAttackMobs = true;

    @Switch(
            name = "Mob Aura Attack Until Dead",
            description = "Whether or not to attack mobs until they are dead",
            category = "Combat",
            subcategory = "Mob Aura"
    )
    public static boolean mobAuraAttackUntilDead = false;

    @DualOption(
            name = "Mob Aura Rotation Type",
            description = "The type of rotation to use for the mob aura",
            category = "Combat",
            subcategory = "Mob Aura",
            left = "Silent",
            right = "Client",
            size = 2
    )
    public static boolean mobAuraRotationType = false;

    @Dropdown(
            name = "Mob Aura Mouse Button",
            description = "The mouse button to use for the mob aura",
            category = "Combat",
            subcategory = "Mob Aura",
            options = {"Left", "Right", "Hold Right"},
            size = 2
    )
    public static int mobAuraMouseButton = 1;

    @DualOption(
            name = "Mob Aura Rotation Mode",
            description = "The mode of rotation to use for the mob aura",
            category = "Combat",
            subcategory = "Mob Aura",
            left = "Bow Rotation",
            right = "Straight Rotation"
    )
    public static boolean mobAuraRotationMode = false;

    @Slider(
            name = "Mob Aura Range",
            description = "The range of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - Range",
            min = 1f,
            max = 30f
    )
    public static float mobAuraRange = 3f;

    @Switch(
            name = "Mob Aura Unlimited Range",
            description = "Whether or not to have unlimited range for the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - Range"
    )
    public static boolean mobAuraUnlimitedRange = false;

    @Slider(
            name = "Mob Aura Y difference",
            description = "The Y difference of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - Range",
            min = 0,
            max = 30
    )
    public static int mobAuraYDifference = 8;

    @Slider(
            name = "Mob Aura FOV",
            description = "The FOV of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - Range",
            min = 0,
            max = 360
    )
    public static int mobAuraFOV = 120;

    @Slider(
            name = "Mob Aura Rotation Speed",
            description = "The speed of the mob aura's rotation",
            category = "Combat",
            subcategory = "Mob Aura - Rotation Speed",
            min = 50,
            max = 800
    )
    public static int mobAuraRotationSpeed = 300;

    @Slider(
            name = "Mob Aura Rotation Speed Randomizer",
            description = "The speed of the mob aura's rotation",
            category = "Combat",
            subcategory = "Mob Aura - Rotation Speed",
            min = 0,
            max = 500
    )
    public static int mobAuraRotationSpeedRandomizer = 100;

    public static long getRandomizedMobAuraRotationSpeed() {
        return (long) (mobAuraRotationSpeed + Math.random() * mobAuraRotationSpeedRandomizer);
    }

    @Slider(
            name = "Mob Aura Cooldown (ms)",
            description = "The cooldown of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - Attack Speed",
            min = 0,
            max = 1000
    )
    public static int mobAuraCooldown = 500;

    @Slider(
            name = "Mob Aura Cooldown Randomizer (ms)",
            description = "The randomizer of the mob aura cooldown",
            category = "Combat",
            subcategory = "Mob Aura - Attack Speed",
            min = 0,
            max = 1000
    )
    public static int mobAuraCooldownRandomizer = 100;

    public static long getRandomizedMobAuraCooldown() {
        return (long) (mobAuraCooldown + Math.random() * mobAuraCooldownRandomizer);
    }

    @Color(
            name = "Mob Aura Possible Target Color",
            description = "The color of the possible targets of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - ESP"
    )
    public static OneColor mobAuraPossibleTargetColor = new OneColor(255, 255, 0, 100);

    @Color(
            name = "Mob Aura Target Color",
            description = "The color of the target of the mob aura",
            category = "Combat",
            subcategory = "Mob Aura - ESP"
    )
    public static OneColor mobAuraCurrentTargetColor = new OneColor(255, 0, 0, 100);

    //</editor-fold>

    //<editor-fold desc="DUNGEON">
    //<editor-fold desc="Auto Boom TNT">
    @Switch(
            name = "Auto Superboom TNT",
            description = "Automatically chooses Superboom TNT when looking at a breakable wall",
            category = "Dungeon",
            subcategory = "Auto Superboom TNT"
    )
    public static boolean autoBoomTNT = false;

    @Switch(
            name = "Auto placement",
            description = "Automatically places the TNT",
            category = "Dungeon",
            subcategory = "Auto Superboom TNT"
    )
    public static boolean autoBoomTNTAutoPlacement = false;

    //</editor-fold>
    //<editor-fold desc="Auto Trap Defuser">
    @Switch(
            name = "Auto Trap Defuser",
            description = "Automatically equips Auto Defuser when looking at a trap",
            category = "Dungeon",
            subcategory = "Auto Trap Defuser"
    )
    public static boolean autoTrapDefuser = false;

    @Switch(
            name = "Auto defuser",
            description = "Automatically defuses the trap",
            category = "Dungeon",
            subcategory = "Auto Trap Defuser"
    )
    public static boolean autoTrapDefuserAutoDefuse = false;

    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="RENDER">
    //<editor-fold desc="Chest ESP">
    @Switch(
            name = "Chest ESP",
            description = "Highlights chests",
            category = "Render",
            subcategory = "Chest ESP"
    )
    public static boolean chestESP = false;

    @Color(
            name = "Chest ESP Color",
            description = "The color of the chest ESP",
            category = "Render",
            subcategory = "Chest ESP"
    )
    public static OneColor chestESPColor = new OneColor(194, 91, 12, 100);

    @Switch(
            name = "Chest ESP Tracers",
            description = "Draws lines to chests",
            category = "Render",
            subcategory = "Chest ESP"
    )
    public static boolean chestESPTracers = false;
    //</editor-fold>

    //<editor-fold desc="Fairy Soul ESP">
    @Switch(
            name = "Fairy Soul ESP",
            description = "Highlights fairy souls",
            category = "Render",
            subcategory = "Fairy Soul ESP"
    )
    public static boolean fairySoulESP = false;

    @Color(
            name = "Fairy Soul ESP Color",
            description = "The color of the fairy soul ESP",
            category = "Render",
            subcategory = "Fairy Soul ESP"
    )
    public static OneColor fairySoulESPColor = new OneColor(147, 8, 207, 100);

    @Switch(
            name = "Fairy Soul ESP Tracers",
            description = "Draws lines to fairy souls",
            category = "Render",
            subcategory = "Fairy Soul ESP"
    )
    public static boolean fairySoulESPTracers = false;

    @Switch(
            name = "Fairy Soul ESP Show Only Closest",
            description = "Only shows the closest fairy soul",
            category = "Render",
            subcategory = "Fairy Soul ESP"
    )
    public static boolean fairySoulESPShowOnlyClosest = false;

    @Switch(
            name = "Fairy Soul ESP Show Distance",
            description = "Shows the distance to the fairy soul",
            category = "Render",
            subcategory = "Fairy Soul ESP"
    )
    public static boolean fairySoulESPShowDistance = false;

    @Button(
            name = "Fairy Soul ESP Reset",
            text = "Reset",
            description = "Resets the fairy soul ESP",
            category = "Render",
            subcategory = "Fairy Soul ESP",
            size = 2
    )
    public static void fairySoulESPReset() {
        ESP.getInstance().resetClickedFairySouls();
    }

    @Button(
            name = "Fairy Souls ESP reset only current island",
            text = "Reset only current island",
            description = "Resets the fairy soul ESP only for the current island",
            category = "Render",
            subcategory = "Fairy Soul ESP",
            size = 2
    )
    public static void fairySoulESPResetOnlyCurrentIsland() {
        ESP.getInstance().resetClickedFairySoulsOnlyCurrentIsland();
    }

    @Button(
            name = "Fairy Souls ESP Add all visible souls to clicked list",
            text = "Add all visible souls to clicked list",
            description = "Adds all visible fairy souls to the clicked list",
            category = "Render",
            subcategory = "Fairy Soul ESP",
            size = 2
    )
    public static void fairySoulESPAddAllVisibleSoulsToClickedList() {
        ESP.getInstance().addAllVisibleFairySoulsToClickedList();
    }

    //</editor-fold>

    //<editor-fold desc="Gift ESP">
    @Switch(
            name = "Gift ESP",
            description = "Highlights gifts",
            category = "Render",
            subcategory = "Gift ESP"
    )
    public static boolean giftESP = false;

    @Color(
            name = "Gift ESP Color",
            description = "The color of the gift ESP",
            category = "Render",
            subcategory = "Gift ESP"
    )
    public static OneColor giftESPColor = new OneColor(230, 230, 230, 100);

    @Switch(
            name = "Gift ESP Tracers",
            description = "Draws lines to gifts",
            category = "Render",
            subcategory = "Gift ESP"
    )
    public static boolean giftESPTracers = false;

    @Switch(
            name = "Gift ESP Show Only on Jerry Workshop",
            description = "Only shows the gift on jerry workshop",
            category = "Render",
            subcategory = "Gift ESP"
    )
    public static boolean giftESPShowOnlyOnJerryWorkshop = false;

    @Switch(
            name = "Gift ESP Show Distance",
            description = "Shows the distance to the closest gift",
            category = "Render",
            subcategory = "Gift ESP"
    )
    public static boolean giftESPShowDistance = false;

    @Button(
            name = "Gift ESP Reset",
            text = "Reset",
            description = "Resets the gift ESP",
            category = "Render",
            subcategory = "Gift ESP",
            size = 2
    )
    public static void giftESPReset() {
        ESP.getInstance().resetClickedGifts();
    }

    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="MISC">
    @KeyBind(
            name = "SmartToggle keybind",
            category = "Misc"
    )
    public static OneKeyBind smartToggleKeybind = new OneKeyBind(Keyboard.KEY_NONE);

    @Switch(
            name = "Ungrab Mouse",
            category = "Misc"
    )
    public static boolean mouseUngrab = false;

    @Info(
            text = "Smart Toggle activates the appropriate macro depending on the situation",
            size = 2,
            category = "Misc",
            type = InfoType.WARNING
    )
    public boolean infoSmartToggle1 = false;

    @Info(
            text = "Gift Aura - Be at Jerry Workshop",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle3 = false;

    @Info(
            text = "Fishing Macro - Hold Rod",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle4 = false;

    @Info(
            text = "Shortbow Aura - Hold Your Item",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle5 = false;

    @Info(
            text = "Foraging Macro - Hold Treecapitator on private island",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle2 = false;

    @Info(
            text = "Fill Chests with Sapplings Macro - Hold Abiphone, Foraging Sack or Sappling and have Treecap in hotbar",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle6 = false;

    @Info(
            text = "Fill Foraging Sacks Macro - Stay in the Hub while looking at the building and holding any Foraging Sack",
            size = 2,
            category = "Misc",
            type = InfoType.INFO
    )
    public boolean infoSmartToggle7 = false;

    @Switch(
            name = "Auto Sprint",
            category = "Misc"
    )
    public static boolean autoSprint = false;

    //</editor-fold>

    //<editor-fold desc="SKILLS">
    //<editor-fold desc="Alchemy Helper">
    @Switch(
            name = "Alchemy Helper",
            description = "Automatically brews potions",
            category = "Skills",
            subcategory = "Alchemy Helper",
            size = 2
    )
    public static boolean alchemyHelper = false;

    @Switch(
            name = "Auto put water bottles",
            description = "Automatically puts water bottles in the brewing stand",
            category = "Skills",
            subcategory = "Alchemy Helper - Options"
    )
    public static boolean alchemyHelperAutoPutWaterBottles = false;

    @Switch(
            name = "Auto put ingredients",
            description = "Automatically puts ingredients in the brewing stand",
            category = "Skills",
            subcategory = "Alchemy Helper - Options"
    )
    public static boolean alchemyHelperAutoPutIngredients = false;

    @Dropdown(
            name = "Max ingredient type",
            description = "The max ingredient type to use",
            category = "Skills",
            subcategory = "Alchemy Helper - Options",
            options = {"None", "Enchanted Sugar Cane", "Enchanted Blaze Rod"},
            size = 2
    )
    public static int alchemyHelperMaxIngredientType = 0;

    @Switch(
            name = "Auto pick up finish potions",
            description = "Automatically picks up finish potions",
            category = "Skills",
            subcategory = "Alchemy Helper - Options"
    )
    public static boolean alchemyHelperAutoPickUpFinishPotions = false;

    @Switch(
            name = "Auto close GUI after picking up potions",
            description = "Automatically closes the GUI after picking up potions",
            category = "Skills",
            subcategory = "Alchemy Helper - Options"
    )
    public static boolean alchemyHelperAutoCloseGUIAfterPickingUpPotions = false;

    @Slider(
            name = "Delay between potion gui actions (ms)",
            description = "The delay between gui actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenPotionGuiActions = 300;

    @Slider(
            name = "Delay between gui potion actions randomizer (ms)",
            description = "The randomizer of the delay between gui actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenPotionGuiActionsRandomizer = 100;

    public static long getRandomizedDelayBetweenPotionGuiActions() {
        return (long) (alchemyHelperDelayBetweenPotionGuiActions + Math.random() * alchemyHelperDelayBetweenPotionGuiActionsRandomizer);
    }

    @Slider(
            name = "Delay between ingredients gui actions (ms)",
            description = "The delay between gui actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenIngredientsGuiActions = 300;

    @Slider(
            name = "Delay between gui ingredients actions randomizer (ms)",
            description = "The randomizer of the delay between gui actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenIngredientsGuiActionsRandomizer = 100;

    public static long getRandomizedDelayBetweenIngredientsGuiActions() {
        return (long) (alchemyHelperDelayBetweenIngredientsGuiActions + Math.random() * alchemyHelperDelayBetweenIngredientsGuiActionsRandomizer);
    }

    @Slider(
            name = "Delay between potion sell actions (ms)",
            description = "The delay between potion sell actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenPotionSellActions = 300;

    @Slider(
            name = "Delay between potion sell actions randomizer (ms)",
            description = "The randomizer of the delay between potion sell actions",
            category = "Skills",
            subcategory = "Alchemy Helper - Times",
            min = 0,
            max = 1000
    )
    public static int alchemyHelperDelayBetweenPotionSellActionsRandomizer = 100;

    public static long getRandomizedDelayBetweenPotionSellActions() {
        return (long) (alchemyHelperDelayBetweenPotionSellActions + Math.random() * alchemyHelperDelayBetweenPotionSellActionsRandomizer);
    }

    @KeyBind(
            name = "Auto sell potions to NPC",
            description = "Automatically sells potions to NPC",
            category = "Skills",
            subcategory = "Alchemy Helper - Additions"
    )
    public static OneKeyBind alchemyHelperAutoSellPotionsToNPCKeybind = new OneKeyBind(Keyboard.KEY_NONE);

    //</editor-fold>

    //<editor-fold desc="Foraging">
    @Switch(name = "Use Fishing Rod", category = "Skills",
            subcategory = "Foraging")
    public static boolean foragingUseRod = false;

    @Dropdown(name = "Fill Chest With Sapling Type", category = "Skills",
            subcategory = "Foraging", options = {"Spruce", "Jungle", "Dark Oak"})
    public static int fillChestSaplingType = 0;

    public static String getSaplingName() {
        switch (MayOBeesConfig.fillChestSaplingType) {
            case 0:
                return "Spruce Sapling";
            case 1:
                return "Jungle Sapling";
            case 2:
                return "Dark Oak Sapling";
            default:
                throw new IllegalStateException("Unexpected value: " + MayOBeesConfig.fillChestSaplingType);
        }
    }

    @DualOption(
            name = "Foraging Mode",
            description = "The mode of foraging",
            category = "Skills",
            subcategory = "Foraging",
            left = "Camera rotations",
            right = "Skulls and moving",
            size = 2
    )
    public static boolean foragingMode = false;

    @DualOption(
            name = "Dirt Detection Mode",
            description = "The mode of dirt detection. Blocks Scanner won't make the proper Vec3's for now. Doesn't really matter in skull mode",
            category = "Skills",
            subcategory = "Foraging",
            left = "Relative Blocks",
            right = "Blocks Scanner"
    )
    public static boolean dirtDetectionMode = false;

    @Slider(
            name = "Foraging Macro Base Rotation Speed",
            description = "The base rotation speed of the foraging macro",
            category = "Skills",
            subcategory = "Foraging - Options",
            min = 50,
            max = 800
    )
    public static int foragingMacroBaseRotationSpeed = 150;

    @Slider(
            name = "Foraging Macro Rotation Speed Randomizer",
            description = "The randomizer of the rotation speed of the foraging macro",
            category = "Skills",
            subcategory = "Foraging - Options",
            min = 0,
            max = 500
    )
    public static int foragingMacroRotationSpeedRandomizer = 50;

    public static long getRandomizedForagingMacroRotationSpeed() {
        return (long) (foragingMacroBaseRotationSpeed + Math.random() * foragingMacroRotationSpeedRandomizer);
    }

    @Slider(name = "Foraging Macro Delay", category = "Skills",
            subcategory = "Foraging - Options", max = 500, min = 0.0F, step = 10)
    public static int foragingDelay = 50;

    @Slider(
            name = "Foraging Macro Extra Break Delay",
            description = "The extra delay between breaking blocks. Most of the time, it's your ping",
            category = "Skills",
            subcategory = "Foraging - Options",
            min = 0,
            max = 800
    )
    public static int foragingMacroExtraBreakDelay = 100;

    @Slider(name = "Stuck timeout",
            category = "Skills",
            subcategory = "Foraging - Options",
            max = 2500, min = 0.0F, step = 100
    )
    public static int stuckTimeout = 1500;

    @Slider(
            name = "Monkey level",
            description = "The monkey level to calculate delay",
            category = "Skills",
            subcategory = "Foraging - Options",
            min = 0,
            max = 100
    )
    public static int monkeyLevel = 0;

    //</editor-fold>

    //<editor-fold desc="Fishing">
    @Switch(
            name = "Fishing",
            description = "Automatically fishes",
            category = "Skills",
            subcategory = "Fishing"
    )
    public static boolean fishing = false;
    @Switch(
            name = "Sneak while fishing",
            description = "Sneaks while fishing",
            category = "Skills",
            subcategory = "Fishing"
    )
    public static boolean sneakWhileFishing = false;
    @Switch(
            name = "Anti AFK",
            description = "Anti AFK",
            category = "Skills",
            subcategory = "Fishing"
    )
    public static boolean antiAfkWhileFishing = false;
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="DEV">
    @Switch(
            name = "Debug Mode",
            description = "Enables debug mode",
            category = "Debug"
    )
    public static boolean debugMode = false;

    @Switch(
            name = "Fly Pathfinder Oringo Compatible",
            description = "Makes the fly pathfinder compatible with Oringo",
            category = "Debug"
    )
    public static boolean flyPathfinderOringoCompatible = false;

    //<editor-fold desc="Tablist">
    @DualOption(
            name = "Save Tablist",
            description = "Saves the tablist to a file",
            category = "Debug",
            subcategory = "Tablist",
            left = "Print",
            right = "Save"
    )
    public static boolean saveTablistToFile = false;
    @Switch(
            name = "Transposed Tablist",
            description = "Transposes the tablist",
            category = "Debug",
            subcategory = "Tablist"
    )
    public static boolean transposedTablist = false;

    @Button(
            name = "Get Tablist",
            text = "Get Tablist",
            description = "Gets the tablist",
            category = "Debug",
            subcategory = "Tablist"
    )
    public static void getTablist() {
        Dev.getInstance().getTablist();
    }
    //</editor-fold>

    //<editor-fold desc="Inventory">
    @DualOption(
            name = "Save Inventory",
            description = "Saves the inventory to a file",
            category = "Debug",
            subcategory = "Inventory",
            left = "Print",
            right = "Save"
    )
    public static boolean saveInventoryToFile = false;

    @Button(
            name = "Get Inventory",
            text = "Get Inventory",
            description = "Gets the inventory",
            category = "Debug",
            subcategory = "Inventory"
    )
    public static void getInventory() {
        Dev.getInstance().getInventory();
    }
    //</editor-fold>

    //<editor-fold desc="Item Lore">
    @DualOption(
            name = "Save Item Lore",
            description = "Saves the item lore of specific slot to a file",
            category = "Debug",
            subcategory = "Item Lore",
            left = "Print",
            right = "Save"
    )
    public static boolean saveItemLoreToFile = false;
    @Number(
            name = "Item Lore Slot",
            description = "The slot to get the item lore from",
            category = "Debug",
            subcategory = "Item Lore",
            min = 0,
            max = 44
    )
    public static int itemLoreSlot = 0;

    @Button(
            name = "Get Item Lore",
            text = "Get Item Lore",
            description = "Gets the item lore of specific slot",
            category = "Debug",
            subcategory = "Item Lore"
    )
    public static void getItemLore() {
        Dev.getInstance().getItemLore(itemLoreSlot);
    }
    //</editor-fold>

    //<editor-fold desc="Scoreboard">
    @DualOption(
            name = "Save Scoreboard",
            description = "Saves the scoreboard to a file",
            category = "Debug",
            subcategory = "Scoreboard",
            left = "Print",
            right = "Save"
    )
    public static boolean saveScoreboardToFile = false;

    @Switch(
            name = "Clean scoreboard lines",
            description = "Gets the cleaned scoreboard lines",
            category = "Debug",
            subcategory = "Scoreboard"
    )
    public static boolean cleanScoreboardLines = true;

    @Button(
            name = "Get Scoreboard",
            text = "Get Scoreboard",
            description = "Gets the scoreboard",
            category = "Debug",
            subcategory = "Scoreboard"
    )
    public static void getScoreboard() {
        Dev.getInstance().getScoreboard();
    }

    @Switch(
            name = "Include scoreboard title",
            description = "Includes the scoreboard title",
            category = "Debug",
            subcategory = "Scoreboard"
    )
    public static boolean includeScoreboardTitle = false;
    //</editor-fold>

    //<editor-fold desc="Packet listener">
    @Switch(
            name = "Listen to incoming packets",
            description = "Listens to packets",
            category = "Debug",
            subcategory = "Packet listener"
    )
    public static boolean listenToIncomingPackets = false;
    @Switch(
            name = "Listen to outgoing packets",
            description = "Listens to packets",
            category = "Debug",
            subcategory = "Packet listener"
    )
    public static boolean listenToOutgoingPackets = false;
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="OTHER">
    //<editor-fold desc="Ghost Blocks">
    @Switch(
            name = "Enable Ghost Blocks",
            description = "Middle clicks turns blocks into air for a short period of time",
            category = "Other",
            subcategory = "Ghost Blocks"
    )
    public static boolean enableGhostBlocks = false;

    @Switch(
            name = "Ghost Blocks only while holding Stonk",
            description = "Middle clicks turns blocks into air for a short period of time",
            category = "Other",
            subcategory = "Ghost Blocks"
    )
    public static boolean ghostBlocksOnlyWhileHoldingStonk = false;

    @Slider(
            name = "Ghost Blocks Duration (ms)",
            description = "The duration of the ghost blocks",
            category = "Other",
            subcategory = "Ghost Blocks",
            min = 500,
            max = 5000
    )
    public static int ghostBlocksDuration = 1000;

    @Dropdown(
            name = "Failsafe sound",
            description = "The sound to play when the failsafe is triggered",
            category = "Other",
            subcategory = "Failsafe",
            options = {"Exp Orbs", "Anvil"},
            size = 2
    )
    public static int failsafeSoundSelected = 0;

    @Switch(
            name = "Stop active modules on rotation/teleport packet",
            description = "Stops all active modules when a rotation packet is received",
            category = "Other",
            subcategory = "Failsafe"
    )
    public static boolean stopMacrosOnRotationTeleportCheck = true;

    @Switch(
            name = "Stop active modules on world change",
            description = "Stops all active modules when a world change is detected",
            category = "Other",
            subcategory = "Failsafe"
    )
    public static boolean stopMacrosOnWorldChange = true;

    //</editor-fold>

    //<editor-fold desc="Gift Aura">
    @Switch(
            name = "Gift Aura",
            description = "Automatically opens gifts",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static boolean giftAura = false;

    @Button(
            name = "Gift Aura Reset",
            text = "Reset",
            description = "Resets the Gift Aura",
            category = "Other",
            subcategory = "Gift Aura",
            size = 2
    )
    public static void giftAuraReset() {
        GiftAura.getInstance().reset();
    }

    @DualOption(
            name = "Gift Aura Rotation Type",
            description = "The type of rotation to use for the gift aura",
            category = "Other",
            subcategory = "Gift Aura",
            left = "Silent",
            right = "Client",
            size = 2
    )
    public static boolean giftAuraRotationType = true;

    @Color(
            name = "Gift Aura ESP Color",
            description = "The color of the gift aura ESP",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static OneColor giftAuraESPColor = new OneColor(240, 93, 94, 100);
    @Switch(
            name = "Open default gifts at Jerry's Workshop",
            description = "Automatically opens gifts hidden throughout Jerry's Workshop",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static boolean giftAuraOpenDefaultGiftsAtJerryWorkshop = false;
    @Switch(
            name = "Open player gifts",
            description = "Automatically opens gifts from other players",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static boolean giftAuraOpenPlayerGifts = false;
    @Switch(
            name = "Open gifts outside of Jerry's Workshop",
            description = "Automatically opens gifts outside of Jerry's Workshop",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static boolean giftAuraOpenGiftsOutsideOfJerryWorkshop = false;
    @Switch(
            name = "Don't check for visibility",
            description = "Don't check if the gift is visible before opening it",
            category = "Other",
            subcategory = "Gift Aura"
    )
    public static boolean giftAuraDontCheckForVisibility = false;
    @Slider(
            name = "Delay (ms)",
            description = "The delay between opening gifts",
            category = "Other",
            subcategory = "Gift Aura",
            min = 150,
            max = 2000
    )
    public static int giftAuraDelay = 750;

    //</editor-fold>
    //</editor-fold>

    public MayOBeesConfig() {
        super(new Mod("MayOBees", ModType.HYPIXEL), "/mayobees/config.json");
        initialize();

        this.addDependency("mobAuraRange", "mobAuraUnlimitedRange", () -> !mobAuraUnlimitedRange);
        this.addDependency("mobAuraYDifference", "mobAuraUnlimitedRange", () -> !mobAuraUnlimitedRange);

        registerKeyBind(smartToggleKeybind, () -> {
            ModuleManager.getInstance().smartToggle();
        });

        registerKeyBind(autoClickerKeybind, () -> {
            if (autoClickerMode == 1) return;
            ModuleManager.getInstance().toggle(AutoClicker.getInstance());
        });

        registerKeyBind(alchemyHelperAutoSellPotionsToNPCKeybind, () -> {
            LogUtils.info("[Alchemy Helper] Selling to NPC: " + (!AlchemyHelper.getInstance().isSellingPotions() ? "Enabled" : "Disabled"));
            AlchemyHelper.getInstance().setSellingPotions(!AlchemyHelper.getInstance().isSellingPotions());
        });
    }
}
