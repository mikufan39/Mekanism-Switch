package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleLogin", at = @At("HEAD"), remap = false)
    private void meks$soulLogin(CallbackInfo ci) {
        // Proxy network (BungeeCord/Velocity) server switches rebuild the connection without a
        // disconnect, so soul-out state would otherwise leak into the new server and lock up
        // all interactions until the player manually toggles it off.
        SoulOutController.onRespawn();
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"), remap = false)
    private void meks$soulRespawn(CallbackInfo ci) {
        SoulOutController.onRespawn();
    }
}
