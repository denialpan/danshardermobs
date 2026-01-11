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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;


public final class ScaleFactor {

    private ScaleFactor() {}

    // possible killstreak implementation
    private static final long KILL_WINDOW_MS = 6000;

    private static final String TAG_MOD = "danshardermobs";
    private static final String TAG_COMBAT = "combat";

    private static final String TAG_KILLS = "kills";
    private static final String TAG_KILLS_TIME = "time";

    private static final String TAG_AGGRO = "aggro";
    private static final String TAG_LEVEL = "level";

    public static void recordKill(Player player, float ttkMs, Mob mob) {

        CompoundTag combatData = getCombatTag(player);
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
        danshardermobs.LOGGER.info("player death and combat reset: {}", player);
        var data = player.getPersistentData();

        // reset all stats except level
        data.remove(TAG_COMBAT);
        CompoundTag combatData = getCombatTag(player);
        int playerLevel = combatData.getInt(TAG_LEVEL);
        double ratio = Config.DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH.get();

        // calculate new level from loss
        int minimumLevelPercent = (int) Math.floor(playerLevel * (1.0f - ratio));
        int minimumLevelFlat = Math.max(0, playerLevel - Config.DANSHARDERMOBS_PLAYER_PERCENT_LOSE_LEVELS_DEATH_FLAT.get());
        int minimumAllowedLevel = Math.max(minimumLevelPercent, minimumLevelFlat);
        minimumAllowedLevel = Math.max(0, minimumAllowedLevel);

        combatData.putInt(TAG_LEVEL, minimumAllowedLevel);
        danshardermobs.LOGGER.info("player death and combat reset: {}, decreased level to: {}", player, minimumAllowedLevel);


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
        CompoundTag mobData = mob.getPersistentData();
        int mobLevel = mobData.getInt(TAG_LEVEL);

        double ratio = Config.DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL.get();
        int minClamp = -Config.DANSHARDERMOBS_PLAYER_VS_MOB_LEVEL_FLAT.get();

        int minimumMobLevel = Math.max((int) Math.round(playerLevel * (1.0f - ratio)), minClamp);

        danshardermobs.LOGGER.info("minimum mob level: {}", minimumMobLevel);
        danshardermobs.LOGGER.info("player level: {}", playerLevel);

        if (mobLevel < minimumMobLevel) {
            danshardermobs.LOGGER.info("mob did not count");

            return playerLevel;
        }
        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));

        if (Config.DANSHARDERMOBS_MOB_BOSS_ALWAYS_LEVEL_UP.get() && hostileEntityConfig.isBoss()) {
            danshardermobs.LOGGER.info("boss killed");
            ttkMs = -1;
        }
        int value = EvaluatePlayerLevel.update(player, ttkMs);
        danshardermobs.LOGGER.info("level increased by: {}", value);

        playerLevel += (int) (value * hostileEntityConfig.baseScalingLevelKillReward());

        return Math.min(Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get(),Math.max(0, playerLevel));
    }

    public static void refreshMobEffects(Mob mob) {
        for (var entry : HostileEffectData.getAll().entrySet()) {

            MobEffect effect = entry.getKey();
            MobEffectInstance current = mob.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));

            if (current != null && current.getDuration() <= 60) {
                mob.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                        200,
                        0,
                        false,
                        true
                ));
            }
        }
    }

    // util related nbt ahh things
    public static int getPlayerLevel(Player player) {
        CompoundTag combatTag = getCombatTag(player);
        return combatTag.getInt(TAG_LEVEL);
    }

    public static CompoundTag getCombatTag(Player player) {

        CompoundTag data = player.getPersistentData();
        CompoundTag modTag = data.getCompound(TAG_MOD);
        CompoundTag combatTag = modTag.getCompound(TAG_COMBAT);
        modTag.put(TAG_COMBAT, combatTag);
        data.put(TAG_MOD, modTag);

        return combatTag;

    }

    public static boolean isModded(Mob mob) {
        var mobData = mob.getPersistentData();
        return mobData.contains(TAG_MOD);
    }

}
