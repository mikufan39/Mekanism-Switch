package com.mikufan.meks.mixin.flight;

import com.mikufan.meks.flight.api.RollEntity;
import com.mikufan.meks.flight.config.MeksFlightConfig;
import com.mikufan.meks.flight.net.FlightNetworking;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow(remap = false)
    @Final
    private Entity entity;

    @Unique
    private boolean lastIsRolling;
    @Unique
    private float lastRoll;

    /**
     * Server-side master switch for roll relay, read from the server's own
     * {@code [flight] enabled} in {@code meks-common.toml}. Client-side flight behaviour is
     * still governed by each client's own config; this only controls accept/relay on the
     * server.
     */
    @Unique
    private boolean meksFlight$relayEnabled() {
        return MeksFlightConfig.getModEnabled();
    }

    @Inject(
            method = "sendChanges",
            at = @At("TAIL"),
            remap = false
    )
    private void meksFlight$syncRollS2C(CallbackInfo ci) {
        if (!meksFlight$relayEnabled()) {
            lastIsRolling = false;
            lastRoll = 0.0f;
            return;
        }
        var rollEntity = (RollEntity) entity;
        var isRolling = rollEntity.meksFlight$isRolling();
        var roll = rollEntity.meksFlight$getRoll();

        if (isRolling != lastIsRolling || roll != lastRoll) {
            FlightNetworking.broadcastRollUpdate(entity, isRolling, roll);

            lastIsRolling = isRolling;
            lastRoll = roll;
        }
    }

    /**
     * First-frame push: when a player starts tracking this entity (addPairing runs once per
     * new tracker), send the current roll state directly to them, so players joining
     * mid-flight immediately see an already-steady roll angle instead of waiting for the
     * next change.
     */
    @Inject(
            method = "addPairing",
            at = @At("TAIL"),
            remap = false
    )
    private void meksFlight$syncRollS2COnPair(ServerPlayer player, CallbackInfo ci) {
        if (!meksFlight$relayEnabled()) {
            return;
        }
        var rollEntity = (RollEntity) entity;
        var isRolling = rollEntity.meksFlight$isRolling();
        var roll = rollEntity.meksFlight$getRoll();
        if (isRolling || roll != 0.0f) {
            FlightNetworking.sendRollUpdateToPlayer(player, entity, isRolling, roll);
        }
    }
}