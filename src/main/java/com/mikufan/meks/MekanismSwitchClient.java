package com.mikufan.meks;

import com.mikufan.meks.client.GuiExchangeSwitch;
import com.mikufan.meks.client.GuiRestorationSwitch;
import com.mikufan.meks.soul.SoulOutController;
import com.mikufan.meks.soul.SoulOutKeybinds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MekanismSwitch.MODID, dist = Dist.CLIENT)
public class MekanismSwitchClient {

    public MekanismSwitchClient(IEventBus modEventBus) {
        modEventBus.addListener(SoulOutKeybinds::register);
        NeoForge.EVENT_BUS.addListener(SoulOutController::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(SoulOutController::onClientTickPost);
        modEventBus.addListener(MekanismSwitchClient::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MeksRegistries.EXCHANGE_SWITCH_CONTAINER.get(), GuiExchangeSwitch::new);
        event.register(MeksRegistries.RESTORATION_SWITCH_CONTAINER.get(), GuiRestorationSwitch::new);
    }
}
