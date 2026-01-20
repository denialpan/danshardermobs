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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.danpan1232.danshardermobs.scale.ScaleFactor.refreshMobEffects;
import static com.danpan1232.danshardermobs.util.ModTags.*;

public final class ScaleEvents {

    @SubscribeEvent
    public void onMobDamaged(LivingDamageEvent event) {

        // check if on hostile list
        if (event.getEntity().level().isClientSide()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!HostileEntityData.isHostile(id)) return;

        // not player damage or any relevant damage
        if (!(event.getSource().getEntity() instanceof Player)) return;
        if (event.getAmount() <= 0) return;

        ScaleFactor.markFirstAggrovation((Mob) event.getEntity());

    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {

        danshardermobs.LOGGER.info("mob die");

        // check if hostile list
        if (event.getEntity().level().isClientSide()) return;
        danshardermobs.LOGGER.info("client check");

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        danshardermobs.LOGGER.info("id: {}", id);

        if (!HostileEntityData.isHostile(id)) return;
        danshardermobs.LOGGER.info("is hostile");


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

            CompoundTag tag = stack.getTag();
            if (tag == null) continue;

            // damage
            if (stack.isDamageableItem()) {
                float damagePercent = tag.getFloat(ItemNBTKeys.DAMAGE_PERCENT);
                if (damagePercent > 0.0f) {
                    int minDamage = (int) (stack.getMaxDamage() * damagePercent);
                    int damage = Mth.nextInt(
                            random,
                            minDamage,
                            stack.getMaxDamage() - 1
                    );
                    stack.setDamageValue(damage);
                }
            }

            // drop chance
            float dropRate = tag.getFloat(ItemNBTKeys.DROP_RATE);
            if (random.nextFloat() > dropRate) continue;

            ItemEntity drop = new ItemEntity(
                    mob.level(),
                    mob.getX(),
                    mob.getY(),
                    mob.getZ(),
                    stack.copy()
            );

            mob.level().addFreshEntity(drop);
            mob.setItemSlot(slot, ItemStack.EMPTY);
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
        if (event.getEntity() instanceof Player player) {
            ScaleFactor.recordPlayerDeath(player);
        }

    }

    // transfer current data to new player clone
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player player = event.getEntity();

        CompoundTag originalData = original.getPersistentData();
        CompoundTag newData = player.getPersistentData();

//        ScaleFactor.recordPlayerDeath(player);

        if (originalData.contains(TAG_MOD)) {
            newData.put(TAG_MOD, originalData.getCompound(TAG_MOD).copy());
        }
    }

    @SubscribeEvent
    public void onMobTick(LivingEvent.LivingTickEvent event) {

        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // if mob has been modified
        boolean isModded = event.getEntity().getPersistentData().getBoolean(TAG_MOD);
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
        // roll effect chance from player % of level cap
        RandomSource random = mob.getRandom();
        float playerPercentProgression = (float) playerLevel / (float) Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get();

        danshardermobs.LOGGER.info("player level: {}, player level cap: {}, player percent progression: {}", playerLevel, Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get(), playerPercentProgression);

        float hostileMinPercent = hostileEntityConfig.playerLevelPercentMin() == -1 ? 0 : hostileEntityConfig.playerLevelPercentMin();
        float hostileMaxPercent = hostileEntityConfig.playerLevelPercentMax() == -1 ? Float.MAX_VALUE : hostileEntityConfig.playerLevelPercentMax();

        if (playerPercentProgression < hostileMinPercent || playerPercentProgression > hostileMaxPercent) return;

        // set new health
        if (Config.DANSHARDERMOBS_MOB_SCALE_HEALTH.get()) {

            danshardermobs.LOGGER.info("health run");
            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);

            if (maxHealth != null) {

                float health = (float) (maxHealth.getBaseValue() + playerLevel * hostileEntityConfig.mobHealthIncrements());

                // roll boss
                float bossChance = random.nextFloat();
                if (bossChance <= hostileEntityConfig.bossChance()) {
                    health *= hostileEntityConfig.bossHealthMultiplier();
                }

                // parse min/max clamps
                float min = hostileEntityConfig.mobMinHealth() == -1 ? 1 : hostileEntityConfig.mobMinHealth();
                float max = hostileEntityConfig.mobMaxHealth() == -1 ? Integer.MAX_VALUE : hostileEntityConfig.mobMaxHealth();

                // clamp health min/max
                health = Math.max(health, min);
                health = Math.min(health, max);

                maxHealth.setBaseValue(health);
                mob.setHealth(mob.getMaxHealth());

                danshardermobs.LOGGER.info("new mob health: {}", health);

            }

        }

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
                float chance = playerPercentProgression * hostileEffectConfig.baseRollChance();

                // roll chance
                if (mobRoll > chance) continue;

                int effectMinLevel = hostileEntityConfig.mobEffectMinAmplifier() == -1 ? 1 : hostileEntityConfig.mobEffectMinAmplifier();
                int effectMaxLevel = hostileEntityConfig.mobEffectMaxAmplifier() == -1 ? 3 : hostileEntityConfig.mobEffectMaxAmplifier();

                int amplifier = (int) (Mth.nextInt(random, effectMinLevel, effectMaxLevel) * playerPercentProgression);

                mob.addEffect(new MobEffectInstance(
                        effect,
                        200,
                        amplifier,
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

                    float weaponMinPercent = weaponVariantConfig.playerLevelPercentMin() == -1 ? 0 : weaponVariantConfig.playerLevelPercentMin();
                    float weaponMaxPercent = weaponVariantConfig.playerLevelPercentMax() == -1 ? Float.MAX_VALUE : weaponVariantConfig.playerLevelPercentMax();

                    if (playerPercentProgression < weaponMinPercent || playerPercentProgression > weaponMaxPercent) continue;

                    float mobRoll = random.nextFloat();
                    float chance = playerPercentProgression * weaponVariantConfig.baseRollChance();

                    if (mobRoll > chance) continue;

                    if (chosenVariant == null || weaponVariantConfig.tier() > chosenVariant.tier()) {
                        chosenVariant = weaponVariantConfig;
                        chosenItem = item;
                    }

                }
            }

            if (chosenItem != null && chosenVariant != null) {

                ItemStack newStack = new ItemStack(chosenItem);
                CompoundTag tag = newStack.getOrCreateTag();
                tag.putFloat(ItemNBTKeys.DAMAGE_PERCENT, chosenVariant.damagePercentage());
                tag.putFloat(ItemNBTKeys.DROP_RATE, chosenVariant.dropRate());

                if (Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
                    rollEnchantments(
                            mob,
                            newStack,
                            chosenVariant.enchantmentMinLevel(),
                            chosenVariant.enchantmentMaxLevel(),
                            chosenVariant.blacklistEnchantments(),
                            random,
                            playerPercentProgression);
                }

                mob.setDropChance(EquipmentSlot.MAINHAND, 0);
                mob.setItemSlot(EquipmentSlot.MAINHAND, newStack);

                danshardermobs.LOGGER.info("equipped mob {} with tier {} weapon {}", mob.getType().toShortString(), chosenVariant.tier(), BuiltInRegistries.ITEM.getKey(chosenItem));

            }
        }

        if (Config.DANSHARDERMOBS_MOB_ARMOR.get()) {

            danshardermobs.LOGGER.info("armor run");

            Map<EquipmentSlot, ChosenArmor> chosenBySlot = new EnumMap<>(EquipmentSlot.class);

            // store armor to add
            for (var entry : HostileArmorData.getAll().entrySet()) {

                Item item = entry.getKey();
                if (!(item instanceof ArmorItem armorItem)) continue;

                EquipmentSlot slot = armorItem.getEquipmentSlot();
                HostileArmorStackConfig stackConfig = entry.getValue();

                for (HostileArmorVariantConfig variant : stackConfig.variants()) {

                    if (variant.disabled()) continue;

                    float armorMinPercent = variant.playerLevelPercentMin() == -1 ? 0 : variant.playerLevelPercentMin();
                    float armorMaxPercent = variant.playerLevelPercentMax() == -1 ? Float.MAX_VALUE : variant.playerLevelPercentMax();

                    if (playerPercentProgression < armorMinPercent || playerPercentProgression > armorMaxPercent) continue;

                    float roll = random.nextFloat();
                    float chance = playerPercentProgression * variant.baseRollChance();
                    if (roll > chance) continue;

                    ChosenArmor current = chosenBySlot.get(slot);

                    if (current == null || variant.tier() > current.variant().tier()) {
                        chosenBySlot.put(slot, new ChosenArmor(item, variant));
                    }
                }
            }

            // roll enchantments and apply
            for (var entry : chosenBySlot.entrySet()) {

                EquipmentSlot slot = entry.getKey();
                ChosenArmor chosen = entry.getValue();

                Item item = chosen.item();
                HostileArmorVariantConfig chosenVariant = chosen.variant();

                ItemStack newStack = new ItemStack(item);

                // data to item
                CompoundTag tag = newStack.getOrCreateTag();
                tag.putFloat(ItemNBTKeys.DAMAGE_PERCENT, chosenVariant.damagePercentage());
                tag.putFloat(ItemNBTKeys.DROP_RATE, chosenVariant.dropRate());

                mob.setItemSlot(slot, newStack);
                mob.setDropChance(slot, 0);

                if (Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
                    rollEnchantments(
                            mob,
                            newStack,
                            chosenVariant.enchantmentMinLevel(),
                            chosenVariant.enchantmentMaxLevel(),
                            chosenVariant.blacklistEnchantments(),
                            random,
                            playerPercentProgression
                    );
                }
            }
        }

        // mark that mob has been modified by this mod
        mobData.putBoolean(TAG_SPAWNED_PREVIOUSLY, true);

        // mob level
        mobData.putInt(TAG_LEVEL, playerLevel);
        mobData.putBoolean(TAG_MOD, true);

        danshardermobs.LOGGER.info("spawned hostile mob: {} with: {}hp", mob, mob.getMaxHealth());
    }

    private record ChosenArmor(
            Item item,
            HostileArmorVariantConfig variant
    ) {}

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

            if (enchantment == null) {
                danshardermobs.LOGGER.warn("unknown enchantent effect: {}", enchantment);
                continue;
            }

            Enchantment ench = enchantment.value();

            if (ench.canEnchant(stack)) {

                boolean conflicts = false;

                if (!Config.DANSHARDERMOBS_MOB_ILLEGAL_ENCHANTMENTS.get()) {

                    Map<Enchantment, Integer> existingEnchantments =
                            EnchantmentHelper.getEnchantments(stack);

                    for (Enchantment existing : existingEnchantments.keySet()) {
                        if (!existing.isCompatibleWith(ench)) {
                            conflicts = true;
                            break;
                        }
                    }
                }

                if (!conflicts) {

                    float rollEnchantment = random.nextFloat();
                    float chanceEnchantment =
                            rollEffectChance * hostileEnchantmentConfig.baseRollChance();

                    int min = minEnchantmentLevel == -1
                            ? ench.getMinLevel()
                            : minEnchantmentLevel;

                    int max = maxEnchantmentLevel == -1
                            ? ench.getMaxLevel()
                            : maxEnchantmentLevel;

                    if (rollEnchantment <= chanceEnchantment) {

                        int level = Mth.nextInt(random, min, max);

                        // optional scaling
                        level = Math.max(1, (int)(level * rollEffectChance));

                        stack.enchant(ench, level);

                        danshardermobs.LOGGER.info(
                                "gave enchantment {} lvl {} min: {} max: {}",
                                BuiltInRegistries.ENCHANTMENT.getKey(ench),
                                level,
                                min,
                                max
                        );
                    }
                }
            }
        }
    }
}