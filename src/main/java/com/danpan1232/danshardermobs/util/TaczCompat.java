package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.danshardermobs;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class TaczCompat {

    private static Boolean loaded;
    private static Boolean guardVillagersSupportLoaded;

    private TaczCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = classExists("com.tacz.guns.api.item.builder.GunItemBuilder")
                    && classExists("com.tacz.guns.api.item.IGun");
        }

        return loaded;
    }

    public static boolean isGuardVillagersSupportLoaded() {
        if (guardVillagersSupportLoaded == null) {
            guardVillagersSupportLoaded = classExists("com.ddd.guardvillagerstaczsupport.Config");
        }

        return guardVillagersSupportLoaded;
    }

    public static SupportConfiguredGun getGuardVillagersSupportGun(ResourceLocation entityTypeId, RandomSource random) {
        if (!isLoaded() || !isGuardVillagersSupportLoaded()) {
            return null;
        }

        String prefix = supportConfigPrefix(entityTypeId);
        if (prefix == null) {
            return null;
        }

        try {
            double spawnChance = getSupportDouble(prefix + "_TACZ_WEAPON_SPAWN_CHANCE");
            List<ResourceLocation> gunIds = getSupportGunIds(prefix + "_TACZ_WEAPON_IDS");
            if (gunIds.isEmpty()) {
                return null;
            }

            ResourceLocation gunId = gunIds.get(random.nextInt(gunIds.size()));
            double dropChance = getSupportDouble(prefix + "_TACZ_WEAPON_DROP_CHANCE");

            return new SupportConfiguredGun(gunId, (float) spawnChance, (float) dropChance);
        } catch (ReflectiveOperationException | LinkageError exception) {
            danshardermobs.LOGGER.warn("Could not read guardvillagerstaczsupport TACZ config", exception);
            return null;
        }
    }

    public static ItemStack createGunStack(ResourceLocation gunId, RegistryAccess registryAccess) {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }

        try {
            Class<?> builderClass = Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder");
            Object builder = builderClass.getMethod("create").invoke(null);

            builderClass.getMethod("setId", ResourceLocation.class).invoke(builder, gunId);
            builderClass.getMethod("setCount", int.class).invoke(builder, 1);
            builderClass.getMethod("setAmmoCount", int.class).invoke(builder, 0);
            builderClass.getMethod("setAmmoInBarrel", boolean.class).invoke(builder, false);

            Method build = findCompatibleBuildMethod(builderClass, "build", registryAccess);
            if (build == null) {
                build = findCompatibleBuildMethod(builderClass, "forceBuild", registryAccess);
            }

            if (build == null) {
                danshardermobs.LOGGER.warn("Could not find compatible TACZ gun builder method for '{}'", gunId);
                return ItemStack.EMPTY;
            }

            Object stack = build.invoke(builder, registryAccess);
            if (!(stack instanceof ItemStack itemStack)) {
                return ItemStack.EMPTY;
            }

            return isGun(itemStack) ? itemStack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | LinkageError exception) {
            danshardermobs.LOGGER.warn("Could not create TACZ gun '{}'", gunId, exception);
            return ItemStack.EMPTY;
        }
    }

    private static Method findCompatibleBuildMethod(Class<?> builderClass, String methodName, RegistryAccess registryAccess) {
        for (Method method : builderClass.getMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].isInstance(registryAccess)) {
                return method;
            }
        }

        return null;
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty() || !isLoaded()) {
            return false;
        }

        try {
            Class<?> gunClass = Class.forName("com.tacz.guns.api.item.IGun");
            Method getGun = gunClass.getMethod("getIGunOrNull", ItemStack.class);
            return getGun.invoke(null, stack) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static String supportConfigPrefix(ResourceLocation entityTypeId) {
        if (ResourceLocation.fromNamespaceAndPath("minecraft", "zombie").equals(entityTypeId)) {
            return "ZOMBIE";
        }

        if (ResourceLocation.fromNamespaceAndPath("minecraft", "pillager").equals(entityTypeId)) {
            return "PILLAGER";
        }

        return null;
    }

    private static double getSupportDouble(String fieldName) throws ReflectiveOperationException {
        Object value = getSupportConfigField(fieldName);
        if (value instanceof ModConfigSpec.DoubleValue doubleValue) {
            return doubleValue.get();
        }

        throw new NoSuchFieldException("Expected DoubleValue field " + fieldName);
    }

    private static List<ResourceLocation> getSupportGunIds(String fieldName) throws ReflectiveOperationException {
        Object value = getSupportConfigField(fieldName);
        if (!(value instanceof ModConfigSpec.ConfigValue<?> configValue)) {
            throw new NoSuchFieldException("Expected ConfigValue field " + fieldName);
        }

        List<ResourceLocation> gunIds = new ArrayList<>();
        Object configured = configValue.get();
        if (!(configured instanceof List<?> entries)) {
            return gunIds;
        }

        for (Object entry : entries) {
            if (!(entry instanceof String idString)) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(idString);
            if (id != null) {
                gunIds.add(id);
            } else {
                danshardermobs.LOGGER.warn("Ignoring invalid guardvillagerstaczsupport TACZ gun id '{}'", idString);
            }
        }

        return gunIds;
    }

    private static Object getSupportConfigField(String fieldName) throws ReflectiveOperationException {
        Class<?> configClass = Class.forName("com.ddd.guardvillagerstaczsupport.Config");
        Field field = configClass.getField(fieldName);
        return field.get(null);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, TaczCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    public record SupportConfiguredGun(
            ResourceLocation gunId,
            float spawnChance,
            float dropChance
    ) {}
}
