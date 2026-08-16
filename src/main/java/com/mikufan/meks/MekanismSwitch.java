package com.mikufan.meks;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(MekanismSwitch.MODID)
public class MekanismSwitch {

    public static final String MODID = "meks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MekanismSwitch(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, Config.COMMON_FILE);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, Config.CLIENT_FILE);
        MeksRegistries.register(modEventBus);
        modEventBus.addListener(MeksPayloads::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(MeksCommands::register);
        NeoForge.EVENT_BUS.addListener(MeksValues::onServerStarted);
        NeoForge.EVENT_BUS.addListener(MeksValues::onServerStopping);
        NeoForge.EVENT_BUS.addListener(MekaSuitEnchantmentHandler::onEnchantmentLevelSet);
        NeoForge.EVENT_BUS.addListener(CreeperExplosionHandler::onExplosionDetonate);
    }
}
