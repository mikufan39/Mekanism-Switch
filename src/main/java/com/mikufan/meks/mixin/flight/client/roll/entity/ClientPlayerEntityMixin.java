package com.mikufan.meks.mixin.flight.client.roll.entity;

import com.mikufan.meks.flight.api.event.RollContext;
import com.mikufan.meks.flight.api.event.RollEvents;
import com.mikufan.meks.flight.api.rotation.RotationInstant;
import com.mikufan.meks.flight.config.Sensitivity;
import com.mikufan.meks.flight.MeksFlightClient;
import com.mikufan.meks.flight.RotationModifiers;
import com.mikufan.meks.flight.math.MagicNumbers;
import com.mikufan.meks.flight.net.FlightNetworking;
import com.mikufan.meks.mixin.flight.roll.entity.PlayerEntityMixin;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntityMixin {
    @Shadow(remap = false)
    public float yBob;
    @Shadow(remap = false)
    public float yBobO;

    @Unique
    private boolean lastSentIsRolling;
    @Unique
    private float lastSentRoll;

    @Inject(
            method = "sendPosition",
            at = @At("TAIL"),
            remap = false
    )
    private void meksFlight$sendRollPacket(CallbackInfo ci) {
        var isRolling = meksFlight$isRolling();
        var rollDiff = meksFlight$getRoll() - lastSentRoll;
        if (isRolling != lastSentIsRolling || rollDiff != 0.0f) {
            FlightNetworking.sendRollUpdateToServer(isRolling, meksFlight$getRoll());

            lastSentIsRolling = isRolling;
            lastSentRoll = meksFlight$getRoll();
        }
    }

    @Override
    @Unique
    protected void meksFlight$baseTickTail2() {
        // Evaluate the flight state (including the per-tick energy drain) before the roll check,
        // so the controls deactivate the moment the chestplate runs out of energy.
        var canFly = MeksFlightClient.updateFlightState();
        meksFlight$setRolling(canFly && RollEvents.shouldRoll());
    }

    @Override
    public void meksFlight$changeElytraLook(double pitch, double yaw, double roll, Sensitivity sensitivity, double mouseDelta) {
        var rotDelta = RotationInstant.of(pitch, yaw, roll);
        var currentRoll = meksFlight$getRoll();
        var currentRotation = RotationInstant.of(
                getXRot(),
                getYRot(),
                currentRoll
        );
        var context = RollContext.of(currentRotation, rotDelta, mouseDelta);

        context.useModifier(RotationModifiers.fixNaN("INPUT"));
        RollEvents.earlyCameraModifiers(context);
        context.useModifier(RotationModifiers.fixNaN("EARLY_CAMERA_MODIFIERS"));
        context.useModifier((rotation, ctx) -> rotation.applySensitivity(sensitivity));
        context.useModifier(RotationModifiers.fixNaN("SENSITIVITY"));
        RollEvents.lateCameraModifiers(context);
        context.useModifier(RotationModifiers.fixNaN("LATE_CAMERA_MODIFIERS"));

        rotDelta = context.getRotationDelta();

        meksFlight$changeElytraLook((float) rotDelta.pitch(), (float) rotDelta.yaw(), (float) rotDelta.roll());
    }

    @Override
    public void meksFlight$changeElytraLook(float pitch, float yaw, float roll) {
        var currentPitch = getXRot();
        var currentYaw = getYRot();
        var currentRoll = meksFlight$getRoll();

        // Convert pitch, yaw, and roll to a facing and left vector
        var facing = new Vector3d(getViewVector(1.0F).toVector3f());
        var left = new Vector3d(1, 0, 0);
        left.rotateZ(-currentRoll * MagicNumbers.TORAD);
        left.rotateX(-currentPitch * MagicNumbers.TORAD);
        left.rotateY(-(currentYaw + 180) * MagicNumbers.TORAD);

        // Apply pitch
        facing.rotateAxis(-0.15 * pitch * MagicNumbers.TORAD, left.x, left.y, left.z);

        // Apply yaw
        var up = facing.cross(left, new Vector3d());
        facing.rotateAxis(0.15 * yaw * MagicNumbers.TORAD, up.x, up.y, up.z);
        left.rotateAxis(0.15 * yaw * MagicNumbers.TORAD, up.x, up.y, up.z);

        // Apply roll
        left.rotateAxis(0.15 * roll * MagicNumbers.TORAD, facing.x, facing.y, facing.z);

        // Extract new pitch, yaw, and roll
        double newPitch = -Math.asin(facing.y) * MagicNumbers.TODEG;
        double newYaw = -Math.atan2(facing.x, facing.z) * MagicNumbers.TODEG;

        var normalLeft = new Vector3d(1, 0, 0).rotateY(-(newYaw + 180) * MagicNumbers.TORAD);
        double newRoll = -Math.atan2(left.cross(normalLeft, new Vector3d()).dot(facing), left.dot(normalLeft)) * MagicNumbers.TODEG;

        // Calculate deltas
        double deltaY = newPitch - currentPitch;
        double deltaX = newYaw - currentYaw;
        double deltaRoll = newRoll - currentRoll;

        // Apply vanilla pitch and yaw
        turn(deltaX / 0.15, deltaY / 0.15);

        // Apply roll
        this.roll += (float) deltaRoll;
        this.prevRoll += (float) deltaRoll;

        // Fix hand spasm when wrapping yaw value
        if (getYRot() < -90 && yBob > 90) {
            yBob -= 360;
            yBobO -= 360;
        } else if (getYRot() > 90 && yBob < -90) {
            yBob += 360;
            yBobO += 360;
        }
    }
}
