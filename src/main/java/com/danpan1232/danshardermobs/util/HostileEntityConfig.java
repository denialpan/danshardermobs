package com.danpan1232.danshardermobs.util;

public record HostileEntityConfig(
        boolean disabled,
        int baseLevel,
        int maxHealth,
        float healthScalingMultiplier,
        int healthScalingFlatMaxGain,
        int levelCap,
        float baseScalingLevelKillReward,
        float xpRewardMultiplier,
        boolean isBoss
) {

    public static final HostileEntityConfig DEFAULT = new HostileEntityConfig(false, 1, -1,1F, 50, -1, 1.0F, 1.5F, false);

}
