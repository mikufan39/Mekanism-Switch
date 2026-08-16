package com.mikufan.meks.mixin.client;

import com.mikufan.meks.flight.MeksFlightController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Redirect(method = "turnPlayer", remap = false, at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V", remap = false))
    private void meks$redirectFlightTurn(LocalPlayer player, double deltaYaw, double deltaPitch) {
        if (!MeksFlightController.handleMouse(player, deltaYaw, deltaPitch)) {
            player.turn(deltaYaw, deltaPitch);
        }
    }
}
