package com.danpan1232.danshardermobs.event;

import com.danpan1232.danshardermobs.Config;
import com.danpan1232.danshardermobs.danshardermobs;
import com.danpan1232.danshardermobs.scale.ScaleFactor;
import com.danpan1232.danshardermobs.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
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
import net.neoforged.neoforge.event.tick.EntityTickEvent;

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


        // set new health
        if (Config.DANSHARDERMOBS_MOB_SCALE_HEALTH.get()) {

            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth == null) return;
            maxHealth.setBaseValue(maxHealth.getBaseValue() + bonusHP);
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
            // roll effects based on % of cap TODO: maybe amplifier??
            danshardermobs.LOGGER.info("roll chance: {}", rollEffectChance);
            for (var entry : HostileEffectData.getAll().entrySet()) {
                MobEffect effect = entry.getKey();
                HostileEffectConfig hostileEffectConfig = entry.getValue();
                float roll = random.nextFloat();
                danshardermobs.LOGGER.info("roll chance: {}, effect: {}", rollEffectChance * hostileEffectConfig.baseRollChance(), effect.getDescriptionId());
                if (roll <= rollEffectChance * hostileEffectConfig.baseRollChance()) {

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

        if (Config.DANSHARDERMOBS_MOB_WEAPONS.get()) {

            ItemStack current = mob.getMainHandItem();
            int currentTier = HostileWeaponData.getTier(current);

            danshardermobs.LOGGER.info("ITEM LOADED");

            HostileWeaponVariantConfig chosenVariant = null;
            Item chosenItem = null;

            for (var entry : HostileWeaponData.getAll().entrySet()) {
                Item item = entry.getKey();
                HostileWeaponStackConfig weaponConfig = entry.getValue();

                danshardermobs.LOGGER.info("ITEM LOADED");


                for (HostileWeaponVariantConfig variant : weaponConfig.variants()) {
                    if (variant.disabled()) continue;

                    float roll = random.nextFloat();
                    float chance = rollEffectChance * variant.baseRollChance();

                    danshardermobs.LOGGER.info("roll chance: {}, item: {}", rollEffectChance * variant.baseRollChance(), item);

                    if (roll <= chance) {

                        if (variant.tier() <= currentTier) continue;
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

                                var enchantmentRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                                for (var entryEnchantment : HostileEnchantmentData.getAll().entrySet()) {

                                    ResourceLocation enchId = entryEnchantment.getKey();
                                    HostileEnchantmentConfig cfg = entryEnchantment.getValue();

                                    if (cfg.disabled()) continue;

                                    var enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, enchId);
                                    var enchantmentOpt = enchantmentRegistry.getHolder(enchantmentKey);

                                    if (enchantmentOpt.isEmpty()) continue;
                                    Holder<Enchantment> enchantment = enchantmentOpt.get();

                                    // TODO: enchantment compatibility check

                                    float rollEnchantment = random.nextFloat();
                                    float chanceEnchantment = rollEffectChance * cfg.baseRollChance();

                                    if (rollEnchantment <= chanceEnchantment) {
                                        int levelEnchantment = Mth.nextInt(
                                                random,
                                                enchantment.value().getMinLevel(),
                                                enchantment.value().getMaxLevel()
                                        );

                                        stack.enchant(enchantment, 100);

                                        danshardermobs.LOGGER.info("gave enchantment {} lvl {} to {}", enchId, levelEnchantment, BuiltInRegistries.ITEM.getKey(chosenItem));
                                    }
                                }
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

                // --- current armor state ---
                ItemStack currentStack = mob.getItemBySlot(slot);
                int currentTier = HostileArmorData.getTier(currentStack);

                HostileArmorVariantConfig chosenVariant = null;

                // --- roll variants ---
                for (HostileArmorVariantConfig variant : stackConfig.variants()) {

                    if (variant.disabled()) continue;

                    float roll = random.nextFloat();
                    float chance = rollEffectChance * variant.baseRollChance();

                    if (roll > chance) continue;

                    // Tier gate: only replace if better
                    if (variant.tier() <= currentTier) continue;

                    // Prefer highest-tier variant
                    if (chosenVariant == null || variant.tier() > chosenVariant.tier()) {
                        chosenVariant = variant;
                        chosenItem = item;
                    }
                }

                // --- apply result ---
                if (chosenItem != null && chosenVariant != null) {

                    ItemStack newStack = new ItemStack(item);

                    mob.setItemSlot(slot, newStack);

                    // Optional: prevent vanilla drop randomness
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

                        var enchantmentRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                        for (var entryEnchantment : HostileEnchantmentData.getAll().entrySet()) {

                            ResourceLocation enchId = entryEnchantment.getKey();
                            HostileEnchantmentConfig cfg = entryEnchantment.getValue();

                            if (cfg.disabled()) continue;

                            var enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, enchId);
                            var enchantmentOpt = enchantmentRegistry.getHolder(enchantmentKey);

                            if (enchantmentOpt.isEmpty()) continue;
                            Holder<Enchantment> enchantment = enchantmentOpt.get();

                            // TODO: enchantment compatibility check
                            // TODO: put enchantment roll in separate function

                            float rollEnchantment = random.nextFloat();
                            float chanceEnchantment = rollEffectChance * cfg.baseRollChance();

                            if (rollEnchantment <= chanceEnchantment) {
                                int levelEnchantment = Mth.nextInt(
                                        random,
                                        enchantment.value().getMinLevel(),
                                        enchantment.value().getMaxLevel()
                                );

                                newStack.enchant(enchantment, 100);

                                danshardermobs.LOGGER.info("gave enchantment {} lvl {} to {}", enchId, levelEnchantment, BuiltInRegistries.ITEM.getKey(chosenItem));
                            }
                        }
                    }
                }
            }
        }

        // mark that mob has been modified by this mod
        var mobData = mob.getPersistentData();
        mobData.putBoolean("danshardermobs", true);
        mobData.putInt("level", playerLevel);

        danshardermobs.LOGGER.info("spawned hostile mob with: {}hp", mob.getMaxHealth());
    }

}
