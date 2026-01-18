package com.danpan1232.danshardermobs.util;

public record HostileEntityConfig(
        boolean disabled,
        boolean isBoss,
        float playerLevelPercentMin,
        float playerLevelPercentMax,
        float playerLevelScalingKillReward,
        int mobMinHealth,
        int mobMaxHealth,
        float mobHealthScalingMultiplier,
        float mobXpRewardMultiplier,
        boolean mobCanHaveArmor,
        boolean mobCanHaveWeapons,
        boolean mobCanHaveEffects,
        boolean mobCanHaveEnchantments

) {

    public static final HostileEntityConfig DEFAULT = new HostileEntityConfig(false, false, -1F,-1F, 1F, -1,-1, 1F, 1.5F, true, true, true, true);

}
