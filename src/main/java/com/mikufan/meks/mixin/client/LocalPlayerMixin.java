package com.mikufan.meks.mixin.client;

import com.mikufan.meks.flight.MeksFlightController;
import com.mikufan.meks.flight.MeksRollState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements MeksRollState {

    @Unique
    private float meks$roll;

    @Unique
    private float meks$prevRoll;

    @Unique
    private float meks$targetRoll;

    @Unique
    private boolean meks$rolling;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void meks$tickFlightControls(CallbackInfo ci) {
        MeksFlightController.tick((LocalPlayer) (Object) this);
    }

    @Override
    public float meks$getRoll() {
        return this.meks$roll;
    }

    @Override
    public float meks$getPrevRoll() {
        return this.meks$prevRoll;
    }

    @Override
    public float meks$getRoll(float partialTick) {
        return Mth.lerp(partialTick, this.meks$prevRoll, this.meks$roll);
    }

    @Override
    public void meks$setRoll(float roll) {
        this.meks$roll = roll;
    }

    @Override
    public void meks$setPrevRoll(float prevRoll) {
        this.meks$prevRoll = prevRoll;
    }

    @Override
    public float meks$getTargetRoll() {
        return this.meks$targetRoll;
    }

    @Override
    public void meks$setTargetRoll(float targetRoll) {
        this.meks$targetRoll = targetRoll;
    }

    @Override
    public boolean meks$isRolling() {
        return this.meks$rolling;
    }

    @Override
    public void meks$setRolling(boolean rolling) {
        this.meks$rolling = rolling;
    }
}
