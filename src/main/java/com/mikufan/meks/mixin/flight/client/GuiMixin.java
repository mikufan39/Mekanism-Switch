package com.mikufan.meks.mixin.flight.client;

import com.mikufan.meks.flight.EventCallbacksClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD"),
            remap = false
    )
    private void meksFlight$captureTickDelta(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        context.pose().pushPose();
        EventCallbacksClient.onRenderCrosshair(context, tickCounter, context.guiWidth(), context.guiHeight());
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("RETURN"),
            remap = false
    )
    private void meksFlight$renderCrosshairReturn(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        context.pose().popPose();
    }
}
