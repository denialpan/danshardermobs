package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

// simple gaussian normal implementation
public final class EvaluatePlayerLevel {

    private static final String TAG_MEAN = "mean";
    private static final String TAG_VARIANCE = "variance";
    private static final String TAG_INITIALIZED = "initialized";

    // handles only level balancing on mob death
    // does not include player death
    public static int update(Player player, float ttkMs) {

        CompoundTag combatData = ScaleFactor.getCombatTag(player);

        // quantize data to 500ms
        float ttk = ttkMs / 1000;
        ttk = Math.round(ttk * 2f) / 2f;

        // killed boss
        if (ttkMs == -1) {
            danshardermobs.LOGGER.info("exception boss kill");

            return Config.DANSHARDERMOBS_PLAYER_SCALING_RANGE.get();
        }

        float mean = combatData.getBoolean(TAG_INITIALIZED) ? combatData.getFloat(TAG_MEAN) : ttk;
        float variance = combatData.getBoolean(TAG_INITIALIZED) ? combatData.getFloat(TAG_VARIANCE) : 1F;
        float alpha = 2F / (Config.DANSHARDERMOBS_PLAYER_EWMA_FACTOR.get() + 1);
        danshardermobs.LOGGER.info("mean: {}, var: {}", mean, variance);

        // EWMA calculation
        float diff = ttk - mean;
        mean += alpha * diff;
        variance = (1 - alpha) * (variance + alpha * diff * diff);

        combatData.putFloat(TAG_MEAN, mean);
        combatData.putFloat(TAG_VARIANCE, variance);
        combatData.putBoolean(TAG_INITIALIZED, true);

        float standardDeviation = (float) Math.sqrt(Math.max(variance, Config.DANSHARDERMOBS_PLAYER_VARIANCE_FLOOR.get()));
        float z = (mean - ttk) / standardDeviation;

        // level up bias
        z += Config.DANSHARDERMOBS_PLAYER_Z_BIAS.get();

        // clamping sensitivitiy
        double Z_MAX = Config.DANSHARDERMOBS_PLAYER_Z_CLAMP.get();
        z = (float) Mth.clamp(z, -Z_MAX, Z_MAX);

        float t = (float) (Math.abs(z) / Z_MAX);
        t = (float) Math.pow(t, Config.DANSHARDERMOBS_PLAYER_Z_CURVE_EXPONENT.get());
        t *= Math.signum(z);

        int maxDelta = Config.DANSHARDERMOBS_PLAYER_SCALING_RANGE.get();

        if (maxDelta < 0) {
            maxDelta *= Config.DANSHARDERMOBS_PLAYER_LOSE_LEVELS_MULTIPLIER.get();
        }


        return Math.round(t * maxDelta);

    }

}
