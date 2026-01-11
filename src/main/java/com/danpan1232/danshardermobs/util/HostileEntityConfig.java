package com.danpan1232.danshardermobs.util;

public record HostileEntityConfig(
        int baseLevel,
        float scalingMultiplier,
        int scalingFlatMaxGain,
        int levelCap,
        float baseScalingLevelKillReward,
        boolean isBoss
) {}
