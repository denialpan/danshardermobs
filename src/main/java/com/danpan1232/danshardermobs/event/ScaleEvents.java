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

public final class ScaleEvents {

    public static final String TAG_LEVEL = "level";
    public static final String TAG_SPAWNED_PREVIOUSLY = "spawnedpreviously";

    @SubscribeEvent
    public void onMobDamaged(LivingDamageEvent.Post event) {

        // check if on hostile list
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!HostileEntityData.isHostile(id)) return;

        if (!(event.getSource().getEntity() instanceof Player)) return;
        if (event.getNewDamage() <= 0) return;
        ScaleFactor.markFirstAggrovation((Mob) event.getEntity());
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {

        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        if (level.isClientSide()) return;

        if (entity instanceof EnderDragon) {
            ScaleFactor.updatePlayerLevel(player, -1, null);
            danshardermobs.LOGGER.info("ender dragon death by {}", player);
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!HostileEntityData.isHostile(id)) return;

        Mob mob = (Mob) entity;
        danshardermobs.LOGGER.info("mob die: {}, experience spawned: {}", mob);

        ScaleFactor.recordMobDeath(player, mob);

        RandomSource random = mob.getRandom();
        for (EquipmentSlot slot : EquipmentSlot.values()) {

            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            int damage = (int) (stack.getMaxDamage() * Config.DANSHARDERMOBS_MOB_DROP_MAX_DAMAGE_PERCENTAGE.get());
            stack.setDamageValue(Mth.nextInt(random, damage, stack.getMaxDamage() - 1));
            float chance = random.nextFloat();
            if (chance > Config.DANSHARDERMOBS_MOB_DROP_RATE.get()) continue;

            ItemEntity drop = new ItemEntity(
                    mob.level(),
                    mob.getX(), mob.getY(), mob.getZ(),
                    stack.copy()
            );

            mob.level().addFreshEntity(drop);
            mob.setItemSlot(slot, ItemStack.EMPTY);

        }
    }

    @SubscribeEvent
    public void onExperienceOrbDrops(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = event.getAttackingPlayer();

        if (entity.level().isClientSide()) return;

        int xp = event.getDroppedExperience();
        danshardermobs.LOGGER.info("spawned xp: {}", xp);

        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        CompoundTag tierTag = entity.getPersistentData();
        int mobLevel = tierTag.getInt(TAG_LEVEL);

        event.setDroppedExperience((int) (xp * mobLevel * hostileEntityConfig.xpRewardMultiplier()));
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

        CompoundTag mobData = mob.getPersistentData();

        // holy massive bug fix to prevent infinite scaling on restarting server
        if (mobData.getBoolean(TAG_SPAWNED_PREVIOUSLY)) return;

        RandomSource random = mob.getRandom();

        // initial mob checks
        // mob is hostile and not disabled
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!HostileEntityData.isHostile(id)) return;

        if (!(level instanceof ServerLevel serverLevel)) return;
        HostileEntityConfig hostileEntityConfig = HostileEntityData.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        if (hostileEntityConfig.disabled()) return;

        int simulationDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();

        Player player = serverLevel.getNearestPlayer(mob, simulationDistance * 16);
        if (player == null) return;

        // get player level
        int playerLevel = ScaleFactor.getPlayerLevel(player);
        if (playerLevel <= 0) return;

        // set new health
        if (Config.DANSHARDERMOBS_MOB_SCALE_HEALTH.get()) {

            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth == null) return;

            float bonusHP = playerLevel * hostileEntityConfig.healthScalingMultiplier();
            danshardermobs.LOGGER.info("health applied: {}", hostileEntityConfig.healthScalingMultiplier());

            int maximumHealth = (int) (hostileEntityConfig.isBoss() ? (maxHealth.getBaseValue() + bonusHP) * hostileEntityConfig.healthScalingMultiplier() : maxHealth.getBaseValue());

            int configMaximumHealth = hostileEntityConfig.maxHealth() == -1 ? Integer.MAX_VALUE : hostileEntityConfig.maxHealth();
            maximumHealth = (int) Math.min(configMaximumHealth, maximumHealth + bonusHP);
            danshardermobs.LOGGER.info("health applied: {}", maximumHealth);

            maxHealth.setBaseValue(maximumHealth);
            mob.setHealth(mob.getMaxHealth());

        }

        // calculate roll effect chance from player % of level cap
        float rollEffectChance = 0.0F;
        if (Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get() == Integer.MAX_VALUE) {
            rollEffectChance += 0.01F;
        } else {
            rollEffectChance = (float) playerLevel / Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get();
        }

        if (Config.DANSHARDERMOBS_MOB_EFFECTS.get()) {
            for (var entry : HostileEffectData.getAll().entrySet()) {
                ResourceLocation effectId = entry.getKey();
                HostileEffectConfig config = entry.getValue();

                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect == null) {
                    danshardermobs.LOGGER.warn("Unknown mob effect {}", effectId);
                    continue;
                }

                float roll = random.nextFloat();
                float chance = rollEffectChance * config.baseRollChance();

                danshardermobs.LOGGER.info("roll chance: {}, effect: {}, overall chance: {}", chance, effect.getDescriptionId(), chance);

                if (roll <= chance) {
                    mob.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                        200,
                        0,
                        false,
                        true
                    ));
                    danshardermobs.LOGGER.info("this ran and mob got effect");
                }
            }
        }

        if (Config.DANSHARDERMOBS_MOB_WEAPONS.get()) {

            ItemStack current = mob.getMainHandItem();

            HostileWeaponVariantConfig chosenVariant = null;
            Item chosenItem = null;

            for (var entry : HostileWeaponData.getAll().entrySet()) {
                Item item = entry.getKey();
                HostileWeaponStackConfig weaponConfig = entry.getValue();

                for (HostileWeaponVariantConfig variant : weaponConfig.variants()) {
                    if (variant.disabled()) continue;

                    int minimumLevel = variant.mobMinLevel() == -1 ? -1 : variant.mobMinLevel();
                    int maximumlevel = variant.mobMaxLevel() == -1 ? Integer.MAX_VALUE : variant.mobMaxLevel();
                    if (playerLevel < minimumLevel || playerLevel > maximumlevel) continue;

                    float roll = random.nextFloat();
                    float chance = rollEffectChance * variant.baseRollChance();


                    if (roll <= chance) {

                        if (chosenVariant == null || variant.tier() > chosenVariant.tier()) {
                            chosenVariant = variant;
                            chosenItem = item;
                        }

                        if (chosenItem != null && chosenVariant != null) {

                            ItemStack stack = new ItemStack(chosenItem);

                            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
                            mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
                            mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);


                            danshardermobs.LOGGER.info("equipped mob {} with tier {} weapon {}", mob.getType().toShortString(), chosenVariant.tier(), BuiltInRegistries.ITEM.getKey(chosenItem));

                            // enchantable, roll enchants
                            if (Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
                                danshardermobs.LOGGER.info("variant enchantment levels min: {} max: {}", chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel());
                                rollEnchantments(mob, chosenItem, stack, chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel(), chosenVariant.blacklistEnchantments(), random, rollEffectChance);
                            }
                        }
                    }
                }
            }
        }

        if (Config.DANSHARDERMOBS_MOB_ARMOR.get()) {

            // roll armor based on % of cap
            Item chosenItem = null;

            for (var entry : HostileArmorData.getAll().entrySet()) {

                Item item = entry.getKey();
                HostileArmorStackConfig stackConfig = entry.getValue();

                if (!(item instanceof ArmorItem armorItem)) continue;

                EquipmentSlot slot = armorItem.getEquipmentSlot();

                HostileArmorVariantConfig chosenVariant = null;

                for (HostileArmorVariantConfig variant : stackConfig.variants()) {

                    if (variant.disabled()) continue;
                    int minimumLevel = variant.mobMinLevel() == -1 ? -1 : variant.mobMinLevel();
                    int maximumlevel = variant.mobMaxLevel() == -1 ? Integer.MAX_VALUE : variant.mobMaxLevel();
                    if (playerLevel < minimumLevel || playerLevel > maximumlevel) continue;

                    float roll = random.nextFloat();
                    float chance = rollEffectChance * variant.baseRollChance();

                    if (roll > chance) continue;

                    if (chosenVariant == null || variant.tier() > chosenVariant.tier()) {
                        chosenVariant = variant;
                        chosenItem = item;
                    }
                }

                if (chosenItem != null && chosenVariant != null) {

                    ItemStack newStack = new ItemStack(item);

                    mob.setItemSlot(slot, newStack);

                    mob.setDropChance(slot, 0.0f);

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
                        rollEnchantments(mob, chosenItem, newStack, chosenVariant.enchantmentMinLevel(), chosenVariant.enchantmentMaxLevel(), chosenVariant.blacklistEnchantments(), random, rollEffectChance);
                    }
                }
            }
        }

        // mark that mob has been modified by this mod
        mobData.putBoolean(TAG_SPAWNED_PREVIOUSLY, true);
        mobData.putInt(TAG_LEVEL, playerLevel);

        danshardermobs.LOGGER.info("spawned hostile mob: {} with: {}hp", mob, mob.getMaxHealth());
    }

    private void rollEnchantments(Mob mob, Item item, ItemStack stack, int minEnchantmentLevel, int maxEnchantmentLevel, Set<ResourceKey<Enchantment>> blacklistEnchantments, RandomSource random, float rollEffectChance) {

        var enchantmentRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);



        for (var entryEnchantment : HostileEnchantmentData.getAll().entrySet()) {

            ResourceLocation enchId = entryEnchantment.getKey();
            HostileEnchantmentConfig hostileEnchantmentConfig = entryEnchantment.getValue();

            if (hostileEnchantmentConfig.disabled()) continue;

            var enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, enchId);
            danshardermobs.LOGGER.info("blacklistedkey: {}", enchantmentKey);

            var enchantmentOpt = enchantmentRegistry.getHolder(enchantmentKey);

            if (enchantmentOpt.isEmpty()) continue;
            Holder<Enchantment> enchantment = enchantmentOpt.get();

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

                        danshardermobs.LOGGER.info("gave enchantment {} lvl {} to {}. min: {}, max: {}", enchId, levelEnchantment, BuiltInRegistries.ITEM.getKey(item), minEnchantmentLevel, maxEnchantmentLevel);
                    }
                }
            }
        }
    }
}
