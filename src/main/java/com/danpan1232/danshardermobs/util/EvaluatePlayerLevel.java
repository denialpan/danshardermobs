package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

// simple bell curve implementation
public final class EvaluatePlayerLevel {

    private static final String TAG_MEAN = "mean";
    private static final String TAG_VARIANCE = "variance";
    private static final String TAG_INITIALIZED = "initialized";

    public static int update(Player player, float ttkMs) {

        CompoundTag combatData = ScaleFactor.getCombatTag(player);

        float ttk = ttkMs / 1000;

        // killed boss
        if (ttkMs == -1) {
            danshardermobs.LOGGER.info("exception kill");

            return Config.DANSHARDERMOBS_PLAYER_SCALE_RANGE.get();
        }

        float mean;
        float variance;
        float alpha = 2F / (Config.DANSHARDERMOBS_PLAYER_EWMA_FACTOR.get() + 1);

        // not initialized yet check
        if (!combatData.getBoolean(TAG_INITIALIZED)) {
            mean = ttk;
            variance = 1.0F;
            combatData.putBoolean(TAG_INITIALIZED, true);
        } else {

            mean = combatData.getFloat(TAG_MEAN);
            variance = combatData.getFloat(TAG_VARIANCE);

            danshardermobs.LOGGER.info("mean: {}, var: {}", mean, variance);
            float diff = ttk - mean;

            mean += alpha * diff;
            variance = (1 - alpha) * (variance + alpha * diff * diff);

            combatData.putFloat(TAG_MEAN, mean);
            combatData.putFloat(TAG_VARIANCE, variance);

            // one shot detection to avoid deadzone
            if (ttkMs < 500) {
                return Config.DANSHARDERMOBS_PLAYER_SCALE_RANGE.get();
            }
        }

        float std = (float) Math.sqrt(Math.max(
                variance,
                Config.DANSHARDERMOBS_PLAYER_VARIANCE_FLOOR.get()
        ));

        float z = (mean - ttk) / std;
        z *= Config.DANSHARDERMOBS_PLAYER_Z_SCORE_MULTIPLIER.get();
        z += Config.DANSHARDERMOBS_PLAYER_Z_BIAS.get();

        double Z_MAX = Config.DANSHARDERMOBS_PLAYER_Z_CLAMP.get();
        z = (float) Mth.clamp(z, -Z_MAX, Z_MAX);

        // deadzone
        double deadzone = Config.DANSHARDERMOBS_PLAYER_Z_DEADZONE.get();
        if (Math.abs(z) < deadzone) {
            return 0;
        }

        // normalize magnitude
        float t = (float) Math.min(Math.abs(z) / Z_MAX, 1f);

        // curve shaping
        t = (float) Math.pow(t, Config.DANSHARDERMOBS_PLAYER_Z_CURVE_EXPONENT.get());

        // restore direction
        float signed = Math.signum(z) * t;

        int maxDelta = Config.DANSHARDERMOBS_PLAYER_SCALE_RANGE.get();
        int delta = Math.round(signed * maxDelta);

        danshardermobs.LOGGER.info("z score: {}, mean: {}, var: {}", z, mean, variance);

        // lose % levels
        if (delta < 0) {

            double percent = Config.DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS.get();
            combatData.getInt("level");
            int loss = (int) (percent * combatData.getInt("level")) * Math.abs(delta);
            danshardermobs.LOGGER.info("delta: {}, loss: {}", delta, -loss);
            return -loss;

        }

        return delta;

    }

}
