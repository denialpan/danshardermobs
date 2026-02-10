package com.danpan1232.danshardermobs;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_SCALE_HEALTH = BUILDER
        .comment("Whether to modify mob health.")
        .define("mobScaleHealth", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ARMOR = BUILDER
            .comment("Whether mobs can spawn with armor.")
            .define("mobArmor", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_EFFECTS = BUILDER
            .comment("Whether mobs can spawn with effects.")
            .define("mobEffects", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_WEAPONS = BUILDER
            .comment("Whether mobs can spawn with weapons. This does not affect vanilla behavior.")
            .define("mobWeapons", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ENCHANTMENTS = BUILDER
            .comment("Whether mobs can spawn with enchanted equipment. This does not affect vanilla behavior.")
            .define("mobEnchantments", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ILLEGAL_ENCHANTMENTS = BUILDER
            .comment("Whether to allow mobs to spawn with illegal enchantments.\n" +
                    "\n ON: Mobs can spawn with items can spawn crazily. For example, a chestplate can have sharpness, and a bow can have Infinity and Mending" +
                    "\n OFF: arguably not as fun. you should enable this haha")
            .define("mobIllegalEnchantments", false);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_MOB_RANDOMLY_WEAKER_CHANCE = BUILDER
            .comment("Chance for mob to randomly spawn slightly weaker\n" +
                    "\n0: no chance for mob to be weaker." +
                    "\n1: 100% chance for mob to be weaker.")
            .defineInRange("mobRandomlyWeakerChance", 0.4, 0, 1);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_MOB_RANDOMLY_WEAKER_AMOUNT = BUILDER
            .comment("Ranged amount % weaker when mob spawns weaker\n" +
                    "\n0: 0% weaker than normal." +
                    "\n1: 100% weaker than normal. Basically no scaling.")
            .defineInRange("mobRandomlyWeakerAmount", 0.2, 0, 1);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_BOSS_ALWAYS_LEVEL_UP = BUILDER
            .comment("Whether to always level up on boss kills.")
            .define("mobBossAlwaysLevelUp", true);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_LEVEL_CAP = BUILDER
            .comment("Maximum level a player can attain.")
            .defineInRange("playerLevelCap", 35, 5, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_SCALING_RANGE = BUILDER
            .comment("Range of levels that a player can gain/lose. This pairs in how quickly it is to reach the level cap" +
                    "\nExample: range of 10 roughly nets 3 - 5 levels on leveling up through normal gameplay")
            .defineInRange("playerScaleRange", 5, 2, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH = BUILDER
            .comment("Percent of current level amount of levels to lose on death.\n" +
                    "\n0: no levels lost" +
                    "\n1: lose all levels")
            .defineInRange("playerLoseLevelsDeath", 0.20, 0.0, 1);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH_CAP = BUILDER
            .comment("Maximum amount of levels to lose on death. This effectively clamps the Player % lose levels death after its calculation.\n" +
                    "\n0: no levels lost" +
                    "\nInteger.MAX_VALUE: unclamped")
            .defineInRange("playerLoseLevelsDeathCap", 30, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_LOSE_LEVELS_MULTIPLIER = BUILDER
            .comment("Multiplier of towards the losing of levels. Often times the base losing level may be small, so a multiplier may help in decreasing larger levels.\n" +
                    "\n0: lose no levels, will make gameplay miserable as mobs will scale infinitely no matter what" +
                    "\n1: lose normal amount, limited to player scaling range" +
                    "\n2: twice as much etc")
            .defineInRange("playerLoseLevelsMultiplier", 1.1, 0.0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_LOSE_LEVELS_CAP = BUILDER
            .comment("Maximum amount of levels to lose when scaling naturally.")
            .defineInRange("playerLoseLevelsCap", 35, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL = BUILDER
            .comment("Minimum % mob level to count towards scaling upon kill. This helps to mitigate farming low level thus weaker mobs to scale faster.\n" +
                    "\n For example 0.3: for a mob to count towards scaling, its level must be at least greater than or within the player's current level - 30% the player's level.")
            .defineInRange("playerVsMobLevel", 0.3, 0, 1);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL_CAP = BUILDER
            .comment("Maximum amount of level difference that a mob can be below the player to count towards scaling upon kill.\n" +
                    "\n For example 50: for a mob to count towards scaling, its level must be at least greater than or within the player's current level - 50." +
                    "\n 0: must be exact level" +
                    "\n 100: mob can be 100 levels below player level to count" +
                    "\n Integer.MAX_VALUE: unclamped, mob can be any level")
            .defineInRange("playerVsMobLevelCap", 50, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_CLAMP = BUILDER
            .comment("Clamps the strength of gaining/losing levels upon kills.\n" +
                    "\nLower: more sensitive" +
                    "\nHigher: less sensitive")
            .defineInRange("playerZClamp", 2.0, 1, 5);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_BIAS = BUILDER
            .comment("Shifts the central point of the scaling curve, either in favor of gaining/losing levels.\n" +
                    "\n0: no bias, equal chance to gain/lose levels" +
                    "\nHigher: more likely to level up, even with lower performance")
            .defineInRange("playerZBias", 0.40, 0.0, 2);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_EWMA_FACTOR = BUILDER
            .comment("Number of recent datapoints to account for in determining the average scaling calculation.\n" +
                    "\nLower = abrupt changes in scaling" +
                    "\nHigher = smoother changes in scaling")
            .defineInRange("playerEWMA", 7, 5, 50);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_CURVE_EXPONENT = BUILDER
            .comment("Sets the aggressiveness of scaling as player performance deviates. \n" +
                    "\n0.1: react strongly" +
                    "\n2: stable leveling, only detects massive outliers")
            .defineInRange("playerZCurveExponent", 0.85, 0.1, 2);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_VARIANCE_FLOOR = BUILDER
            .comment("Prevents scaling overreaction to outliers if recent performance has been consistent. \n" +
                    "\n0: sensitive abrupt scaling" +
                    "\n1: slow scaling from changes, may feel sluggish")
            .defineInRange("playerVariance", 0.25, 0, 1);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_DEBUG = BUILDER
            .comment("Debug text into console")
            .define("debugText", false);



    // a list of strings that are treated as resource locations for items
//    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
//            .comment("A list of items to log on common setup.")
//            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

}
