package com.danpan1232.danshardermobs.mixin;

import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import com.danpan1232.danshardermobs.util.HostileEntityData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class ScaleMob {

    // actually this entire mixin class might not be needed in place of joinentityevent subscribe
    // we'll see though

    private static final String TAG_MOB_LEVEL = "level";

    @Shadow
    protected abstract int getBaseExperienceReward();

    @Inject(
        method = "finalizeSpawn",
        at = @At("TAIL")
    )
    private void finalizeMob(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {

//        may not need this code section since EntityJoinLevel listener may do the purpose guarenteed mobs including withers etc
//        Mob mob = (Mob)(Object)(this);
//
//        // initial mob checks
//        // mob is hostile
//        danshardermobs.LOGGER.info("mob: {}", mob);
//        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
//        if (!(mob instanceof Monster) && !HostileEntityData.isHostile(id)) return;
//        if (!(level instanceof ServerLevel serverLevel)) return;
//
//        int simulationDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
//
//        Player player = serverLevel.getNearestPlayer(mob, simulationDistance * 16);
//        if (player == null) return;
//
//        // get player level
//        int playerLevel = ScaleFactor.getPlayerLevel(player);
//        if (playerLevel <= 0) return;
//        float bonusHP = playerLevel * 1.0f;
//
//        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
//        if (maxHealth == null) return;
//        maxHealth.setBaseValue(maxHealth.getBaseValue() + bonusHP);
//        mob.setHealth(mob.getMaxHealth());
//        mob.addEffect(new MobEffectInstance(
//                MobEffects.MOVEMENT_SPEED,
//                -1,
//                1,
//                false,
//                true
//        ));
//        var mobData = mob.getPersistentData();
//        mobData.putInt(TAG_MOB_LEVEL, playerLevel);
//
//        danshardermobs.LOGGER.info("spawned hostile mob with: {}hp", mob.getMaxHealth());

    }

    @Unique
    private static void danshardermobs$mobTier(Mob mob, int kills) {

        int tier = kills / 20;
        switch (tier) {

            case 10:
                // no armor
                break;
            default:
                danshardermobs$equipArmor(mob, tier, 0.1);
                break;

        }

    }

    @Unique
    private static void danshardermobs$equipArmor(Mob mob, int tier, double chance) {

        danshardermobs.LOGGER.info("mob has leather armor");
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));

    }

    @Unique
    private static void danshardermobs$equipWeapon(Mob mob) {

    }


}