package com.danpan1232.danshardermobs.util;

public record HostileEntityConfig(
        boolean disabled,
        boolean isBoss,
        float bossChance,
        float bossHealthMultiplier,
        float playerLevelPercentMin,
        float playerLevelPercentMax,
        float playerLevelScalingKillReward,
        int mobMinHealth,
        int mobMaxHealth,
        float mobHealthIncrements,
        float mobXpRewardMultiplier,
        boolean mobCanHaveArmor,
        boolean mobCanHaveWeapons,
        boolean mobCanHaveEffects,
        boolean mobCanHaveEnchantments

) {

    public static final HostileEntityConfig DEFAULT = new HostileEntityConfig(false, false, 0.01f, 2, -1F,-1F, 1F, -1,-1, 1, 1.5F, true, true, true, true);

}
