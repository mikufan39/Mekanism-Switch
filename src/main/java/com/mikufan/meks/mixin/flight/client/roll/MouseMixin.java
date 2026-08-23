package com.mikufan.meks.mixin.flight.client.roll;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mikufan.meks.flight.api.RollEntity;
import com.mikufan.meks.flight.api.RollMouse;
import com.mikufan.meks.flight.config.MeksFlightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin implements RollMouse {
    @Shadow(remap = false)
    @Final
    private Minecraft minecraft;

    @Unique
    private final Vector2d mouseTurnVec = new Vector2d();

    @Inject(
            method = "handleAccumulatedMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z",
                    ordinal = 0
            ),
            remap = false
    )
    private void meksFlight$maintainMouseMomentum(CallbackInfo ci, @Local(ordinal = 1) double e) {
        if (minecraft.player != null && !minecraft.isPaused()) {
            meksFlight$updateMouse(minecraft.player, 0, 0, e);
        }
    }

    @WrapWithCondition(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            ),
            remap = false
    )
    private boolean meksFlight$changeLookDirection(LocalPlayer player, double cursorDeltaX, double cursorDeltaY, @Local(argsOnly = true) double timeDelta) {
        return !meksFlight$updateMouse(player, cursorDeltaX, cursorDeltaY, timeDelta);
    }

    @Override
    public boolean meksFlight$updateMouse(LocalPlayer player, double cursorDeltaX, double cursorDeltaY, double mouseDelta) {
        var rollPlayer = (RollEntity) player;

        if (rollPlayer.meksFlight$isRolling()) {

            if (MeksFlightConfig.getMomentumBasedMouse()) {

                // add the mouse movement to the current vector and normalize if needed
                mouseTurnVec.add(new Vector2d(cursorDeltaX, cursorDeltaY).mul(1f / 300));
                if (mouseTurnVec.lengthSquared() > 1.0) {
                    mouseTurnVec.normalize();
                }
                var readyTurnVec = new Vector2d(mouseTurnVec);

                // check if the vector is within the deadzone
                double deadzone = MeksFlightConfig.getMomentumMouseDeadzone();
                if (readyTurnVec.lengthSquared() < deadzone * deadzone) readyTurnVec.zero();

                // enlarge the vector and apply it to the camera
                readyTurnVec.mul(1200 * (float) mouseDelta);
                rollPlayer.meksFlight$changeElytraLook(readyTurnVec.y, readyTurnVec.x, 0, MeksFlightConfig.getDesktopSensitivity(), mouseDelta);

            } else {

                // if we are not using a momentum based mouse, we can reset it and apply the values directly
                mouseTurnVec.zero();
                rollPlayer.meksFlight$changeElytraLook(cursorDeltaY, cursorDeltaX, 0, MeksFlightConfig.getDesktopSensitivity(), mouseDelta);
            }

            return true;
        }

        mouseTurnVec.zero();
        return false;
    }
}
