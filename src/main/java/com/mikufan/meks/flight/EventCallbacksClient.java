package com.mikufan.meks.flight;

import com.mikufan.meks.flight.impl.key.InputContextImpl;
import net.minecraft.client.Minecraft;

public class EventCallbacksClient {
    public static void clientTick(Minecraft client) {
        InputContextImpl.getContexts().forEach(InputContextImpl::tick);

        if (!MeksFlightClient.isFallFlying()) {
            MeksFlightClient.clearValues();
        }
    }
}