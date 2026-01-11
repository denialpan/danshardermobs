package com.danpan1232.danshardermobs.event;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import com.danpan1232.danshardermobs.util.HostileEffectData;
import com.danpan1232.danshardermobs.util.HostileEntityData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

import static com.danpan1232.danshardermobs.scale.ScaleFactor.refreshMobEffects;

public final class ScaleEvents {

    @SubscribeEvent
    public void onMobDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Monster mob)) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;
        if (event.getNewDamage() <= 0) return;
        ScaleFactor.markFirstAggrovation(mob);
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster mob)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        danshardermobs.LOGGER.info("mob die: {}", mob);
        ScaleFactor.recordMobDeath(player, mob);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if ((event.getEntity() instanceof Player player)) {
            ScaleFactor.recordPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public void onMobTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;

        if (!(event.getEntity() instanceof Mob mob)) return;

        if (!ScaleFactor.isModded(mob)) return;
        refreshMobEffects(mob);
    }

    @SubscribeEvent
    public void onMobJoinSpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        if (!(entity instanceof Mob mob)) return;
        RandomSource random = mob.getRandom();

        // initial mob checks
        // mob is hostile
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!(mob instanceof Monster) && !HostileEntityData.isHostile(id)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        int simulationDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();

        Player player = serverLevel.getNearestPlayer(mob, simulationDistance * 16);
        if (player == null) return;

        // get player level
        int playerLevel = ScaleFactor.getPlayerLevel(player);
        if (playerLevel <= 0) return;
        float bonusHP = playerLevel * 1.0f;

        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        maxHealth.setBaseValue(maxHealth.getBaseValue() + bonusHP);
        mob.setHealth(mob.getMaxHealth());


        // roll effects based on % of cap, if exists. otherwise base 1%
        float rollEffectChance = 0.0F;
        if (Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get() == Integer.MAX_VALUE) {
            rollEffectChance += 0.01F;
        } else {
            rollEffectChance = (float) playerLevel / Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get();
        }
        danshardermobs.LOGGER.info("roll chance: {}", rollEffectChance);
        for (MobEffect effect : HostileEffectData.getAll()) {
            danshardermobs.LOGGER.info("roll chance: {}, effect: {}", rollEffectChance, effect.getDescriptionId());
            float roll = random.nextFloat();
            if (roll <= rollEffectChance) {

                mob.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                        200,
                        0,
                        false,
                        true
                ));
            }
        }

        var mobData = mob.getPersistentData();
        mobData.putBoolean("danshardermobs", true);
        mobData.putInt("level", playerLevel);

        danshardermobs.LOGGER.info("spawned hostile mob with: {}hp", mob.getMaxHealth());
    }

}
