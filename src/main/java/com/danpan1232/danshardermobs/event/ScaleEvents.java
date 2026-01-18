package com.danpan1232.danshardermobs.event;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import com.danpan1232.danshardermobs.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;

import static com.danpan1232.danshardermobs.scale.ScaleFactor.refreshMobEffects;

/**
 * Event handler for mob and player events. Every function is gated to be server sided:
 *
 * MOB:
 * - on mob damaged -> detecting first mob hit
 * - on mob death
 * - on experience drops -> modify amount
 * - on mob tick -> refresh effects
 * - on mob join server -> essentially mob spawning
 *
 * PLAYER:
 * - on player death
 */
public final class ScaleEvents {

    public static final String TAG_LEVEL = "mobLevel";
    public static final String TAG_SPAWNED_PREVIOUSLY = "spawnedpreviously";

    // TODO: allow config to dictate global rates for armor, enchantment, weapon, effects
    // TODO: for default.jsons, define default rates as -1, if not -1, then default to datapack values
    // TODO: rename losing levels % to multiplier in config.java
    // TODO: refactor mob_health_scaling_multiplier to mob_health_increments

    @SubscribeEvent
    public void onMobDamaged(LivingDamageEvent.Post event) {

        // check if on hostile list
        if (event.getEntity().level().isClientSide()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!HostileEntityData.isHostile(id)) return;

        // not player damage or any relevant damage
        if (!(event.getSource().getEntity() instanceof Player)) return;
        if (event.getNewDamage() <= 0) return;

        ScaleFactor.markFirstAggrovation((Mob) event.getEntity());

    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {

        // check if hostile list
        if (event.getEntity().level().isClientSide()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!HostileEntityData.isHostile(id)) return;

        LivingEntity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // ender dragon exception check before casting, as it is not a Mob class
        if (entity instanceof EnderDragon) {
            ScaleFactor.updatePlayerLevel(player, -1, null);
            danshardermobs.LOGGER.info("ender dragon death by {}", player);
        }

        // entity -> mob cast check
        if (!(entity instanceof Mob mob)) return;
        danshardermobs.LOGGER.info("mob die: {}, experience spawned: {}", mob);

        ScaleFactor.recordMobDeath(player, mob);

        // roll drops and damage for equipment
        RandomSource random = mob.getRandom();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            // Apply random damage
            danshardermobs.LOGGER.info("attempting drop");

            // TODO: add damage property to item
            if (stack.isDamageableItem()) {
                int minDamage = (int) (stack.getMaxDamage()
                        * Config.DANSHARDERMOBS_MOB_DROP_MAX_DAMAGE_PERCENTAGE.get());

                int damage = Mth.nextInt(
                        random,
                        minDamage,
                        stack.getMaxDamage() - 1
                );

                stack.setDamageValue(damage);
            }

//            // Configure vanilla equipment drop chance
//            mob.setDropChance(
//                    slot,
//                    Config.DANSHARDERMOBS_MOB_DROP_RATE.get()
//            );
        }
    }

    @SubscribeEvent
    public void onExperienceOrbDrops(LivingExperienceDropEvent event) {

        if (event.getEntity().level().isClientSide()) return;
        LivingEntity entity = event.getEntity();

        int xp = event.getDroppedExperience();
        danshardermobs.LOGGER.info("spawned xp: {}", xp);

        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        int mobLevel = entity.getPersistentData().getInt(TAG_LEVEL);

        event.setDroppedExperience((int) (xp * mobLevel * hostileEntityConfig.mobXpRewardMultiplier()));
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {

        if (event.getEntity().level().isClientSide()) return;
        if ((event.getEntity() instanceof Player player)) {
            ScaleFactor.recordPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public void onMobTick(EntityTickEvent.Post event) {

        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // if mob has been modified
        boolean isModded = event.getEntity().getPersistentData().getBoolean(danshardermobs.MODID);
        if (isModded) {
            refreshMobEffects(mob);
        }
    }

    @SubscribeEvent
    public void onMobJoinSpawn(EntityJoinLevelEvent event) {

        // given that this is the most important method, there are multiple checks put in line here, in order:
        // - must be server side function
        // - mob is registered as hostile
        // - mob is not disabled in config
        // - mob has previously been spawned and modified

        // server side and mob registered
        if (event.getEntity().level().isClientSide()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!HostileEntityData.isHostile(id)) return;

        // mob is not disabled through config
        if (!(event.getEntity() instanceof Mob mob)) return;
        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        if (hostileEntityConfig.disabled()) return;

        // mob has not been spawned previously
        CompoundTag mobData = mob.getPersistentData();
        if (mobData.getBoolean(TAG_SPAWNED_PREVIOUSLY)) return;

        // get server simulation distance such that mobs that spawn within range of closest player are modified
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        int simulationDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
        Player player = serverLevel.getNearestPlayer(mob, simulationDistance * 16);
        if (player == null) return;

        // get player level
        int playerLevel = ScaleFactor.getPlayerLevel(player);
        if (playerLevel <= 0) return;

        // set new health
        if (Config.DANSHARDERMOBS_MOB_SCALE_HEALTH.get()) {

            danshardermobs.LOGGER.info("health run");


            // health calculation: ((playerLevel * mob config health multiplier) + original mob health) clamped too mob config max
            // is mob is marked as a boss, then

            // TODO: come back to boss health calculation mechanics
            // TODO: add property of boss percent chance
            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {

                float bonusHP = playerLevel * hostileEntityConfig.mobHealthScalingMultiplier();
                int maximumHealth = (int) (hostileEntityConfig.isBoss() ? (maxHealth.getBaseValue() + bonusHP) * hostileEntityConfig.mobHealthScalingMultiplier() : maxHealth.getBaseValue());

                int configMaximumHealth = hostileEntityConfig.mobMaxHealth() == -1 ? Integer.MAX_VALUE : hostileEntityConfig.mobMaxHealth();
                maximumHealth = (int) Math.min(configMaximumHealth, maximumHealth + bonusHP);
                danshardermobs.LOGGER.info("health applied: {}", maximumHealth);

                maxHealth.setBaseValue(maximumHealth);
                mob.setHealth(mob.getMaxHealth());

            }
        }

        // roll effect chance from player % of level cap
        RandomSource random = mob.getRandom();
        float playerChance = (float) playerLevel / Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get();

        if (Config.DANSHARDERMOBS_MOB_EFFECTS.get()) {

            danshardermobs.LOGGER.info("effect run");


            for (var entry : HostileEffectData.getAll().entrySet()) {

                // is disabled
                ResourceLocation effectId = entry.getKey();
                HostileEffectConfig hostileEffectConfig = entry.getValue();
                if (hostileEffectConfig.disabled()) continue;

                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);

                if (effect == null) {
                    danshardermobs.LOGGER.warn("unknown mob effect: {}", effectId);
                    continue;
                }

                float mobRoll = random.nextFloat();
                float chance = playerChance * hostileEffectConfig.baseRollChance();

                danshardermobs.LOGGER.info("mobRoll: {}, overall chance: {}", mobRoll, chance);

                // roll chance
                if (mobRoll > chance) continue;
                mob.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                    200,
                    0,
                    false,
                    true
                ));
            }
        }

        // roll mob weapons from player % of level cap
        if (Config.DANSHARDERMOBS_MOB_WEAPONS.get()) {

            danshardermobs.LOGGER.info("weapon run");


            HostileWeaponVariantConfig chosenVariant = null;
            Item chosenItem = null;

            for (var entry : HostileWeaponData.getAll().entrySet()) {

                Item item = entry.getKey();
                HostileWeaponStackConfig weaponStackConfig = entry.getValue();

                // item's variants
                for (HostileWeaponVariantConfig weaponVariantConfig : weaponStackConfig.variants()) {

                    if (weaponVariantConfig.disabled()) continue;

                    // TODO: refactor to percentages, instead of specific level

                    int minimumLevel = weaponVariantConfig.mobMinLevel() == -1 ? -1 : weaponVariantConfig.mobMinLevel();
                    int maximumlevel = weaponVariantConfig.mobMaxLevel() == -1 ? Integer.MAX_VALUE : weaponVariantConfig.mobMaxLevel();
                    if (playerLevel < minimumLevel || playerLevel > maximumlevel) continue;

                    float mobRoll = random.nextFloat();
                    float chance = playerChance * weaponVariantConfig.baseRollChance();

                    if (mobRoll > chance) continue;

                    if (chosenVariant == null || weaponVariantConfig.tier() > chosenVariant.tier()) {
                        chosenVariant = weaponVariantConfig;
                        chosenItem = item;
                    }

                    if (chosenItem != null && chosenVariant != null) {

                        ItemStack stack = new ItemStack(chosenItem);

                        // if enchantable

                        // TODO: add min max enchantment levels to config
                        if (Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
                            danshardermobs.LOGGER.info("variant enchantment levels min: {} max: {}", chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel());
                            rollEnchantments(mob, stack, chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel(), chosenVariant.blacklistEnchantments(), random, playerChance);
                        }

                        mob.setDropChance(EquipmentSlot.MAINHAND, chosenVariant.dropRate());
                        mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
                        // this may be overwritten by mob death event
                        // mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);


                        danshardermobs.LOGGER.info("equipped mob {} with tier {} weapon {}", mob.getType().toShortString(), chosenVariant.tier(), BuiltInRegistries.ITEM.getKey(chosenItem));

                    }

                }
            }
        }

        if (Config.DANSHARDERMOBS_MOB_ARMOR.get()) {

            danshardermobs.LOGGER.info("armor run");

            // functionally identical to rolling weapons
            HostileArmorVariantConfig chosenVariant = null;
            Item chosenItem = null;

            for (var entry : HostileArmorData.getAll().entrySet()) {

                Item item = entry.getKey();
                HostileArmorStackConfig stackConfig = entry.getValue();

                if (!(item instanceof ArmorItem armorItem)) continue;

                EquipmentSlot slot = armorItem.getEquipmentSlot();

                for (HostileArmorVariantConfig hostileArmorVariantConfig : stackConfig.variants()) {

                    if (hostileArmorVariantConfig.disabled()) continue;

                    // TODO: refactor to percentages, instead of specific level
                    int minimumLevel = hostileArmorVariantConfig.mobMinLevel() == -1 ? -1 : hostileArmorVariantConfig.mobMinLevel();
                    int maximumlevel = hostileArmorVariantConfig.mobMaxLevel() == -1 ? Integer.MAX_VALUE : hostileArmorVariantConfig.mobMaxLevel();
                    if (playerLevel < minimumLevel || playerLevel > maximumlevel) continue;

                    float roll = random.nextFloat();
                    float chance = playerChance * hostileArmorVariantConfig.baseRollChance();

                    if (roll > chance) continue;

                    if (chosenVariant == null || hostileArmorVariantConfig.tier() > chosenVariant.tier()) {
                        chosenVariant = hostileArmorVariantConfig;
                        chosenItem = item;
                    }

                    if (chosenItem != null && chosenVariant != null) {

                        ItemStack newStack = new ItemStack(item);

                        mob.setItemSlot(slot, newStack);

                        mob.setDropChance(slot, chosenVariant.dropRate());

                        danshardermobs.LOGGER.info(
                                "Equipped {} with armor {} tier {} in slot {}",
                                mob.getType().toShortString(),
                                BuiltInRegistries.ITEM.getKey(item),
                                chosenVariant.tier(),
                                slot
                        );

                        // enchantable, roll enchants
                        if (Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
                            danshardermobs.LOGGER.info("variant enchantment levels min: {} max: {}", chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel());
                            rollEnchantments(mob, newStack, chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel(), chosenVariant.blacklistEnchantments(), random, playerChance);
                        }
                    }
                }


            }
        }

        // mark that mob has been modified by this mod
        mobData.putBoolean(TAG_SPAWNED_PREVIOUSLY, true);

        // mob level
        mobData.putInt(TAG_LEVEL, playerLevel);

        danshardermobs.LOGGER.info("spawned hostile mob: {} with: {}hp", mob, mob.getMaxHealth());
    }

    /**
     * Roll enchantments for item
     *
     * @param mob
     * @param stack
     * @param minEnchantmentLevel
     * @param maxEnchantmentLevel
     * @param blacklistEnchantments
     * @param random
     * @param rollEffectChance
     */
    private void rollEnchantments(Mob mob, ItemStack stack, int minEnchantmentLevel, int maxEnchantmentLevel, Set<ResourceKey<Enchantment>> blacklistEnchantments, RandomSource random, float rollEffectChance) {

        var enchantmentRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var entryEnchantment : HostileEnchantmentData.getAll().entrySet()) {

            ResourceLocation enchId = entryEnchantment.getKey();
            HostileEnchantmentConfig hostileEnchantmentConfig = entryEnchantment.getValue();

            if (hostileEnchantmentConfig.disabled()) continue;

            var enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, enchId);
            var enchantmentHolder = enchantmentRegistry.getHolder(enchantmentKey);

            if (enchantmentHolder.isEmpty()) continue;
            Holder<Enchantment> enchantment = enchantmentHolder.get();

            if (blacklistEnchantments.contains(enchantmentKey)) {
                danshardermobs.LOGGER.info("blacklisted: {}", enchantment);
                continue;
            }

            if (enchantment.value().canEnchant(stack)) {

                boolean existingEnchantmentConflicts = false;
                if (!Config.DANSHARDERMOBS_MOB_ILLEGAL_ENCHANTMENTS.get()) {

                    // check conflicts
                    for (var e : stack.getEnchantments().entrySet()) {
                        Holder<Enchantment> existingEnchantment = e.getKey();

                        if (!Enchantment.areCompatible(existingEnchantment, enchantment)) {
                            existingEnchantmentConflicts = true;
                            break;
                        }
                    }
                }

                if (!existingEnchantmentConflicts) {

                    float rollEnchantment = random.nextFloat();
                    float chanceEnchantment = rollEffectChance * hostileEnchantmentConfig.baseRollChance();

                    minEnchantmentLevel = minEnchantmentLevel == -1 ? enchantment.value().getMinLevel() : minEnchantmentLevel;
                    maxEnchantmentLevel = maxEnchantmentLevel == -1 ? enchantment.value().getMaxLevel() : maxEnchantmentLevel;

                    if (rollEnchantment <= chanceEnchantment) {
                        int levelEnchantment = Mth.nextInt(
                            random,
                            minEnchantmentLevel,
                            maxEnchantmentLevel
                        );

                        stack.enchant(enchantment, levelEnchantment);

                        danshardermobs.LOGGER.info("gave enchantment {} lvl {} to {}. min: {}, max: {}", enchId, levelEnchantment, minEnchantmentLevel, maxEnchantmentLevel);
                    }
                }
            }
        }
    }
}
