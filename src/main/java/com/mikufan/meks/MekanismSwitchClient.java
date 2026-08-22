package com.mikufan.meks;

import com.mikufan.meks.client.GuiExchangeSwitch;
import com.mikufan.meks.client.GuiRestorationSwitch;
import com.mikufan.meks.flight.EventCallbacksClient;
import com.mikufan.meks.flight.MeksFlightClient;
import com.mikufan.meks.flight.MeksFlightKeybinds;
import com.mikufan.meks.soul.SoulOutController;
import com.mikufan.meks.soul.SoulOutKeybinds;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import mekanism.common.util.text.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mod(value = MekanismSwitch.MODID, dist = Dist.CLIENT)
public class MekanismSwitchClient {

    public MekanismSwitchClient(IEventBus modEventBus) {
        modEventBus.addListener(SoulOutKeybinds::register);
        modEventBus.addListener(MeksFlightKeybinds::register);
        MeksFlightClient.init();
        NeoForge.EVENT_BUS.addListener(SoulOutController::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(SoulOutController::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(MekanismSwitchClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MekanismSwitchClient::onItemTooltip);
        modEventBus.addListener(MekanismSwitchClient::onRegisterScreens);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        EventCallbacksClient.clientTick(Minecraft.getInstance());
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MeksRegistries.EXCHANGE_SWITCH_CONTAINER.get(), GuiExchangeSwitch::new);
        event.register(MeksRegistries.RESTORATION_SWITCH_CONTAINER.get(), GuiRestorationSwitch::new);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        if (event.getContext().level() != null) {
            MeksValues.ensureInitialized(event.getContext().level());
        }
        long value = MeksValues.getValue(stack.getItem());
        if (value <= 0) {
            return;
        }
        event.getToolTip().add(Component.translatable("gui.meks.sv_value", TextUtils.format(value))
              .withStyle(ChatFormatting.GOLD));
    }
}
