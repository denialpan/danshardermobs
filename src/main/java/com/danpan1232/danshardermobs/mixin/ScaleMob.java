package com.danpan1232.danshardermobs.mixin;

import com.danpan1232.danshardermobs.danshardermobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class ScaleMob {

    @Inject(
        method = "finalizeSpawn",
        at = @At("TAIL")
    )
    private void scaleHealth(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {

        Mob mob = (Mob)(Object)(this);

        if (!(mob instanceof Monster)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Player player = serverLevel.getNearestPlayer(mob, 32);
        if (player == null) return;

        int kills = player.getPersistentData().getInt("killstreak");
        if (kills <= 0) return;

        float bonusHP = kills * 1.0f;

        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth == null) return;
        maxHealth.setBaseValue(maxHealth.getBaseValue() + bonusHP);

        mob.setHealth(mob.getMaxHealth());
        danshardermobs$mobTier(mob, kills);
        danshardermobs.LOGGER.info("spawned hostile mob with: {}hp", mob.getMaxHealth());


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