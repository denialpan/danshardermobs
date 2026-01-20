package com.danpan1232.danshardermobs.scale;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import static com.danpan1232.danshardermobs.util.ModTags.TAG_KILLS;
import static com.danpan1232.danshardermobs.util.ModTags.TAG_KILLS_TIME;
import static com.danpan1232.danshardermobs.util.ModTags.TAG_LEVEL;
import static com.danpan1232.danshardermobs.util.ModTags.TAG_AGGRO;
import static com.danpan1232.danshardermobs.util.ModTags.TAG_COMBAT;
import static com.danpan1232.danshardermobs.util.ModTags.TAG_MOD;

public final class ScaleFactor {


    private ScaleFactor() {}

    // possible killstreak implementation
    private static final long KILL_WINDOW_MS = 6000;


    public static void recordKill(Player player, float ttkMs, Mob mob) {

        CompoundTag combatData = getCombatData(player);
        CompoundTag killsData = combatData.getCompound(TAG_KILLS);

        ListTag list = killsData.getList(TAG_KILLS_TIME, Tag.TAG_LONG);
        long now = System.currentTimeMillis();

        // clean up old kills outside killstreak window
        while (!list.isEmpty()) {
            long oldest = ((LongTag) list.getFirst()).getAsLong();
            if (now - oldest <= KILL_WINDOW_MS) break;
            list.removeFirst();
        }

        // add most recent kill, update player level
        list.add(LongTag.valueOf(now));
        killsData.put(TAG_KILLS_TIME, list);
        combatData.put(TAG_KILLS, killsData);
        combatData.putInt(TAG_LEVEL, updatePlayerLevel(player, ttkMs, mob));

        danshardermobs.LOGGER.info("killrate in {} window: {}", KILL_WINDOW_MS, getKillRate(player));
        danshardermobs.LOGGER.info("combat data: {}", combatData);

    }

    public static void recordPlayerDeath(Player player) {
        var data = player.getPersistentData();

        // reset all stats except level
        CompoundTag combatData = getCombatData(player);
        CompoundTag modData = data.getCompound(TAG_MOD);
        modData.remove(TAG_COMBAT);

        // calculate new level from loss
        int playerLevel = combatData.getInt(TAG_LEVEL);
        double ratio = Config.DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH.get();
        int percentCalculatedLoseLevels = (int) (playerLevel * ratio);
        int loseLevelsClamp = Config.DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH_CAP.get();
        int loseLevels = Math.min(loseLevelsClamp, percentCalculatedLoseLevels);

        CompoundTag newCombatData = new CompoundTag();
        newCombatData.putInt(TAG_LEVEL, playerLevel - loseLevels);
        modData.put(TAG_COMBAT, newCombatData);
        data.put(TAG_MOD, modData);
        danshardermobs.LOGGER.info("player death current level: {}, decreased level by: {}", playerLevel, loseLevels);

        danshardermobs.LOGGER.info("data: {}", data);
    }

    public static void recordMobDeath(Player player, Mob mob) {
        var data = mob.getPersistentData();
        if (!data.contains(TAG_AGGRO)) return;

        long start = data.getLong(TAG_AGGRO);
        long ttkMs = System.currentTimeMillis() - start;

        danshardermobs.LOGGER.info("player killed mob within: {}ms", ttkMs);
        recordKill(player, ttkMs, mob);
    }

    public static int getKillRate(Player player) {
//        return player.getPersistentData().getList(TAG_KILLS, Tag.TAG_LONG).size();
        return -1;
    }

    public static void markFirstAggrovation(Mob mob) {
        var data = mob.getPersistentData();
        if (!data.contains(TAG_AGGRO)) {
            data.putLong(TAG_AGGRO, System.currentTimeMillis());
        }
    }

    public static int updatePlayerLevel(Player player, float ttkMs, Mob mob) {

        int playerLevel = getPlayerLevel(player);

        // killed enderdragon
        if (ttkMs < 0) {
            HostileEntityConfig enderDragonConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON));
            playerLevel *= (int) enderDragonConfig.playerLevelScalingKillReward();
            danshardermobs.LOGGER.info("level increased to from enderdragon: {}", playerLevel);
            return Math.min(Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get(),Math.max(0, playerLevel));

        }

        CompoundTag mobData = mob.getPersistentData();
        int mobLevel = mobData.getInt(TAG_LEVEL);
        danshardermobs.LOGGER.info("moblevel: {}", mobLevel);

        double ratio = Config.DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL.get();

        int percentCalculatedMinimumMobLevelRange = (int) (playerLevel * ratio);
        int minimumMobLevelRangeClamp = Config.DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL_CAP.get();

        int minimumMobLevelSubtractRange = Math.min(minimumMobLevelRangeClamp, percentCalculatedMinimumMobLevelRange);
        int requiredMobLevel = mobLevel - minimumMobLevelSubtractRange;

        danshardermobs.LOGGER.info("minimum mob level: {}", requiredMobLevel);
        danshardermobs.LOGGER.info("player level: {}", playerLevel);

        if (mobLevel < requiredMobLevel) {
            danshardermobs.LOGGER.info("mob did not count");

            return playerLevel;
        }

        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));

        float levelMultiplier = 1;
        if (Config.DANSHARDERMOBS_MOB_BOSS_ALWAYS_LEVEL_UP.get() && hostileEntityConfig.isBoss()) {
            // mark impossible time as boss
            ttkMs = -1;
            levelMultiplier *= hostileEntityConfig.playerLevelScalingKillReward();

        }

        int value = EvaluatePlayerLevel.update(player, ttkMs);

        if (value < 0) {
            value = -Math.min(Config.DANSHARDERMOBS_PLAYER_LOSE_LEVELS_CAP.get(), Math.abs(value));
            playerLevel += value;
        } else {
            playerLevel += (int) (value * levelMultiplier);
        }

//        playerLevel = Math.min(Config.DANSHARDERMOBS_PLAYER_LOSE_LEVELS_MAX.get(), playerLevel);

        danshardermobs.LOGGER.info("level increased by: {}", value);

        return Math.min(Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get(),Math.max(0, playerLevel));
    }

    public static void refreshMobEffects(Mob mob) {
        for (var entry : HostileEffectData.getAll().entrySet()) {
            ResourceLocation effectId = entry.getKey();

            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
            if (effect == null) {
                danshardermobs.LOGGER.warn("Unknown mob effect: {}", effectId);
                continue;
            }

            MobEffectInstance current = mob.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
            if (current != null && current.getDuration() <= 60) {
                mob.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                    200,
                    current.getAmplifier(),
                    current.isAmbient(),
                    current.isVisible()
                ));
            }
        }
    }

    // util related nbt ahh things
    public static int getPlayerLevel(Player player) {
        CompoundTag combatData = getCombatData(player);
        return combatData.getInt(TAG_LEVEL);
    }

    public static CompoundTag getCombatData(Player player) {

        CompoundTag data = player.getPersistentData();
        CompoundTag modTag = data.getCompound(TAG_MOD);
        CompoundTag combatTag = modTag.getCompound(TAG_COMBAT);
        modTag.put(TAG_COMBAT, combatTag);
        data.put(TAG_MOD, modTag);

        return combatTag;

    }

}
