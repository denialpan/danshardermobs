package com.danpan1232.danshardermobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_SCALE_HEALTH = BUILDER
        .comment("Whether to scale mob health.")
        .define("mobScaleHealth", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ARMOR = BUILDER
            .comment("Whether to give mobs armor.")
            .define("mobArmor", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_EFFECTS = BUILDER
            .comment("Whether to give mobs effects.")
            .define("mobEffects", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_WEAPONS = BUILDER
            .comment("Whether to give mobs weapons.")
            .define("mobWeapons", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ENCHANTMENTS = BUILDER
            .comment("Whether to give enchantments to mob equipment.")
            .define("mobEnchantments", true);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_ILLEGAL_ENCHANTMENTS = BUILDER
            .comment("Whether to allow mobs to spawn with illegal enchantments.\n" +
                    "\n ON: Mobs can spawn with items can spawn crazily. For example, a chestplate can have sharpness, and a bow can have Infinity and Mending" +
                    "\n OFF: arguably not as fun. you should enable this haha")
            .define("mobIllegalEnchantments", false);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_MOB_DROP_RATE = BUILDER
            .comment("Drop rate of equipment for mobs")
            .defineInRange("mobDropRate", 0.1, 0, 1);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_MOB_DROP_MAX_DAMAGE_PERCENTAGE = BUILDER
            .comment("Damage percentage of equipment dropped from mobs\n" +
                    "\n0: no damage" +
                    "\n0.99: nearly all damage")
            .defineInRange("mobDropDamagePercentage", 0.9, 0, 0.999);

    public static final ModConfigSpec.BooleanValue DANSHARDERMOBS_MOB_BOSS_ALWAYS_LEVEL_UP = BUILDER
            .comment("Whether to always level up on boss kills.")
            .define("mobBossAlwaysLevelUp", true);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_LEVEL_CAP = BUILDER
            .comment("Maximum level a player can attain.")
            .defineInRange("playerLevelCap", 100, 5, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_SCALE_RANGE = BUILDER
            .comment("Range of levels that a player can gain/lose.\n" +
                    "\nExample: range of 10 is roughly 5 levels either gained/lost")
            .defineInRange("playerScaleRange", 10, 2, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH = BUILDER
            .comment("Rough percentage of current player level number of levels to lose upon death.\n" +
                    "\n0: no levels lost" +
                    "\n1: lose all levels")
            .defineInRange("playerLoseLevelsDeath", 0.20, 0.0, 1);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH_FLAT = BUILDER
            .comment("Maximum flat levels to lose. Clamps on the % levels lost upon death\n" +
                    "\n0: no levels lost" +
                    "\n1: lose all levels")
            .defineInRange("playerLoseLevelsDeathFlat", 30, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS = BUILDER
            .comment("Rough percentage of current player level number of levels to lose when decreasing.\n" +
                    "\n0: least amount of levels lost (usually ~1% of current scaling range to prevent stuck level)" +
                    "\n1: lose all levels")
            .defineInRange("playerScaleRangeLoseLevels", 0.20, 0.0, 1);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL = BUILDER
            .comment("Threshold of mob level to current player level to count towards scaling.\n" +
                    "\n For example 0.3: for a mob to count towards scaling, its level must be at least greater than or within the player's current level - 30% the player's level.")
            .defineInRange("playerVsMobLevel", 0.3, 0, 1);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL_FLAT = BUILDER
            .comment("Flat minimum mob level to current player level to count towards scaling. This flat calculation clamps % setting above.\n" +
                    "\n For example 50: for a mob to count towards scaling, its level must be at least greater than or within the player's current level - 50." +
                    "\n 0: must be exact level" +
                    "\n 100: mob can be 100 levels below player level to count")
            .defineInRange("playerVsMobLevelFlat", 50, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_CLAMP = BUILDER
            .comment("How sensitive player scaling overall. (This is the Z clamp in the Z-score formula).\n" +
                    "\nLower: more sensitive" +
                    "\nHigher: less sensitive")
            .defineInRange("playerZClamp", 2.0, 1, 5);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_BIAS = BUILDER
            .comment("Z bias towards leveling up.\n" +
                    "\n0: no bias, equal chance to gain/lose levels" +
                    "\nHigher: more bias to level up")
            .defineInRange("playerZBias", 0.35, 0.0, 2);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_DEADZONE = BUILDER
            .comment("Detection of player scaling slowing down.\n" +
                    "\nLower: harder to detect stagnation" +
                    "\nHigher: easier to detect stagnation")
            .defineInRange("playerZDeadzone", 0.15, 0.01, 3);

    public static final ModConfigSpec.IntValue DANSHARDERMOBS_PLAYER_EWMA_FACTOR = BUILDER
            .comment("Number of samples (kills) to account for in scaling.\n" +
                    "\nLower = abrupt changes in scaling" +
                    "\nHigher = smoother changes in scaling")
            .defineInRange("playerEWMA", 7, 5, 50);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_CURVE_EXPONENT = BUILDER
            .comment("Difficulty scaling based on deviation from averaged performance.\n" +
                    "\nLower: react strongly" +
                    "\nHigher: react weaker")
            .defineInRange("playerZCurveExponent", 0.85, 0.1, 2);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_VARIANCE_FLOOR = BUILDER
            .comment("Reaction to player performance.\n" +
                    "\nLower: abrupt changes in scaling" +
                    "\nHigher: smoother changes in scaling")
            .defineInRange("playerVariance", 0.25, 0, 1);

    public static final ModConfigSpec.DoubleValue DANSHARDERMOBS_PLAYER_Z_SCORE_MULTIPLIER = BUILDER
            .comment("Applied to z score before scaling.\n" +
                    "\nx > 1: more sensitive" +
                    "\nx < 1: less sensitive")
            .defineInRange("playerZScoreMultiplier", 1.0, 0, 2);




    // a list of strings that are treated as resource locations for items
//    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
//            .comment("A list of items to log on common setup.")
//            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
