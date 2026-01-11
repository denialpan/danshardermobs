package com.danpan1232.danshardermobs;

import com.danpan1232.danshardermobs.event.ScaleEvents;
import com.danpan1232.danshardermobs.util.HostileEffectLoader;
import com.danpan1232.danshardermobs.util.HostileEntityLoader;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(danshardermobs.MODID)
public class danshardermobs {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "danshardermobs";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public danshardermobs(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new ScaleEvents());
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onReload(AddReloadListenerEvent event) {
        event.addListener(new HostileEntityLoader());
        event.addListener(new HostileEffectLoader());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
