package com.mikufan.meks.mixin.flight;

import com.mikufan.meks.flight.api.RollEntity;
import com.mikufan.meks.flight.net.FlightNetworking;
import net.minecraft.server.level.ServerEntity;
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

    @Inject(
            method = "sendChanges",
            at = @At("TAIL"),
            remap = false
    )
    private void meksFlight$syncRollS2C(CallbackInfo ci) {
        var rollEntity = (RollEntity) entity;
        var isRolling = rollEntity.meksFlight$isRolling();
        var roll = rollEntity.meksFlight$getRoll();

        if (isRolling != lastIsRolling || roll != lastRoll) {
            FlightNetworking.broadcastRollUpdate(entity, isRolling, roll);

            lastIsRolling = isRolling;
            lastRoll = roll;
        }
    }
}
