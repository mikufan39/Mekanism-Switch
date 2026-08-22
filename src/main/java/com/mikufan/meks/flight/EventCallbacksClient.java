package com.mikufan.meks.flight;

import com.mikufan.meks.flight.api.RollEntity;
import com.mikufan.meks.flight.api.RollMouse;
import com.mikufan.meks.flight.config.MeksFlightConfig;
import com.mikufan.meks.flight.impl.key.InputContextImpl;
import com.mikufan.meks.flight.render.HorizonLineWidget;
import com.mikufan.meks.flight.render.MomentumCrosshairWidget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2d;

public class EventCallbacksClient {
    public static void clientTick(Minecraft client) {
        InputContextImpl.getContexts().forEach(InputContextImpl::tick);

        if (!MeksFlightClient.isFallFlying()) {
            MeksFlightClient.clearValues();
        }

        MeksFlightKeybinds.clientTick(client);
    }

    public static void onRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, int scaledWidth, int scaledHeight) {
        if (!MeksFlightClient.isFallFlying()) return;
        var tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);

        var matrices = context.pose();
        var entity = Minecraft.getInstance().getCameraEntity();
        var rollEntity = ((RollEntity) entity);
        if (entity != null) {
            if (MeksFlightConfig.getShowHorizon()) {
                HorizonLineWidget.render(matrices, scaledWidth, scaledHeight,
                        rollEntity.meksFlight$getRoll(tickDelta), entity.getViewXRot(tickDelta));
            }

            if (MeksFlightConfig.getMomentumBasedMouse() && MeksFlightConfig.getShowMomentumWidget()) {
                var rollMouse = (RollMouse) Minecraft.getInstance().mouseHandler;

                MomentumCrosshairWidget.render(matrices, scaledWidth, scaledHeight, new Vector2d(rollMouse.meksFlight$getMouseTurnVec()));
            }
        }
    }
}