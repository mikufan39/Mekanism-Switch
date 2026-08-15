package com.mikufan.meks;

import com.mikufan.meks.client.GuiExchangeSwitch;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = MekanismSwitch.MODID, dist = Dist.CLIENT)
public class MekanismSwitchClient {

    public MekanismSwitchClient(IEventBus modEventBus) {
        modEventBus.addListener(MekanismSwitchClient::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MeksRegistries.EXCHANGE_SWITCH_CONTAINER.get(), GuiExchangeSwitch::new);
    }
}
