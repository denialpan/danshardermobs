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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.danpan1232.danshardermobs.danshardermobs.MODID;
import static com.danpan1232.danshardermobs.scale.ScaleFactor.refreshMobEffects;
import static com.danpan1232.danshardermobs.util.ModTags.*;

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
            ItemStack newStack = mob.getItemBySlot(slot);
            if (newStack.isEmpty()) continue;

            // item damage
            if (newStack.isDamageableItem()) {
                int damage = (int) (newStack.getMaxDamage() * newStack.getOrDefault(ModDataComponents.DAMAGE_PERCENT, 0.0f));
                newStack.setDamageValue(Mth.nextInt(random, damage, newStack.getMaxDamage() - 1));
            }

            // drop chance
            float chance = random.nextFloat();
            if (chance > newStack.getOrDefault(ModDataComponents.DROP_RATE, 0.0f)) continue;


            ItemEntity drop = new ItemEntity(
                    mob.level(),
                    mob.getX(), mob.getY(), mob.getZ(),
                    newStack.copy()
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
    public void onMobTick(EntityTickEvent.Post event) {

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

        applyDebuggingText();

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

        double weakerMultiplier;

        if (random.nextFloat() < Config.DANSHARDERMOBS_MOB_RANDOMLY_WEAKER_CHANCE.get()) {
            weakerMultiplier = Mth.nextDouble(random, 1 - Config.DANSHARDERMOBS_MOB_RANDOMLY_WEAKER_AMOUNT.get(), 1);
            danshardermobs.LOGGER.info("weaker %: {}", weakerMultiplier);
        } else {
            weakerMultiplier = 1;
        }

        float playerPercentProgression = (float) (((float) playerLevel / (float) Config.DANSHARDERMOBS_PLAYER_LEVEL_CAP.get()) * weakerMultiplier);

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
                if (hostileEntityConfig.isBoss() && bossChance <= hostileEntityConfig.bossChance()) {
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
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
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

            ChosenWeapon chosenWeapon = null;

            if (TaczCompat.isLoaded()) {
                TaczCompat.SupportConfiguredGun supportGun = TaczCompat.getGuardVillagersSupportGun(id, random);
                if (supportGun != null) {
                    HostileWeaponVariantConfig variant = new HostileWeaponVariantConfig(
                            false,
                            supportGun.spawnChance(),
                            supportGun.dropChance(),
                            0.0f,
                            false,
                            -1,
                            -1,
                            -1,
                            -1,
                            false,
                            Set.of(),
                            HostileWeaponData.defaults().tier()
                    );

                    float mobRoll = random.nextFloat();
                    float chance = playerPercentProgression * variant.baseRollChance();
                    danshardermobs.LOGGER.info(
                            "guardvillagerstaczsupport TACZ roll for {} gun {}: roll={}, chance={} (playerPercentProgression={}, baseRollChance={}, dropRate={})",
                            id,
                            supportGun.gunId(),
                            mobRoll,
                            chance,
                            playerPercentProgression,
                            variant.baseRollChance(),
                            variant.dropRate()
                    );

                    if (mobRoll <= chance) {
                        chosenWeapon = chooseHigherTierWeapon(chosenWeapon, new ChosenWeapon(null, supportGun.gunId(), variant));
                        danshardermobs.LOGGER.info(
                                "guardvillagerstaczsupport TACZ weapon candidate accepted for {}: {}",
                                id,
                                supportGun.gunId()
                        );
                    }
                }
            }

            if (chosenWeapon == null) {
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

                        chosenWeapon = chooseHigherTierWeapon(chosenWeapon, new ChosenWeapon(item, null, weaponVariantConfig));

                    }
                }
            }

            if (chosenWeapon != null) {

                HostileWeaponVariantConfig chosenVariant = chosenWeapon.variant();
                ItemStack newStack = chosenWeapon.item() != null
                        ? new ItemStack(chosenWeapon.item())
                        : TaczCompat.createGunStack(chosenWeapon.taczGunId(), mob.registryAccess());

                if (newStack.isEmpty()) {
                    danshardermobs.LOGGER.warn("Skipped empty weapon stack for mob {}", mob.getType().toShortString());
                } else {
                    newStack.set(ModDataComponents.DAMAGE_PERCENT, chosenVariant.damagePercentage());
                    newStack.set(ModDataComponents.DROP_RATE, chosenVariant.dropRate());

                    if (chosenWeapon.item() != null && Config.DANSHARDERMOBS_MOB_ENCHANTMENTS.get() && chosenVariant.enchantable()) {
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

                    danshardermobs.LOGGER.info("equipped mob {} with tier {} weapon {}", mob.getType().toShortString(), chosenVariant.tier(), chosenWeapon.id());
                    if (chosenWeapon.taczGunId() != null) {
                        danshardermobs.LOGGER.info(
                                "spawned {} with guardvillagerstaczsupport TACZ weapon {}",
                                mob.getType().toShortString(),
                                chosenWeapon.taczGunId()
                        );
                    }
                }

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
                newStack.set(ModDataComponents.DAMAGE_PERCENT, chosenVariant.damagePercentage());
                newStack.set(ModDataComponents.DROP_RATE, chosenVariant.dropRate());

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

    private record ChosenWeapon(
            Item item,
            ResourceLocation taczGunId,
            HostileWeaponVariantConfig variant
    ) {
        private ResourceLocation id() {
            return item != null ? BuiltInRegistries.ITEM.getKey(item) : taczGunId;
        }
    }

    private ChosenWeapon chooseHigherTierWeapon(ChosenWeapon current, ChosenWeapon candidate) {
        if (current == null || candidate.variant().tier() > current.variant().tier()) {
            return candidate;
        }

        return current;
    }

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
                        int levelEnchantment = (int) (Mth.nextInt(random, minEnchantmentLevel, maxEnchantmentLevel) * rollEffectChance);

                        stack.enchant(enchantment, levelEnchantment);

                        danshardermobs.LOGGER.info("gave enchantment {} lvl {} min: {}, max: {}", enchId, levelEnchantment, minEnchantmentLevel, maxEnchantmentLevel);
                    }
                }
            }
        }
    }

    public static void applyDebuggingText() {
        Configurator.setLevel(
                "com.danpan1232.danshardermobs",
                Config.DANSHARDERMOBS_DEBUG.get() ? Level.INFO : Level.OFF
        );
    }

}
