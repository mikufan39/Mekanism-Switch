package com.mikufan.meks.soul;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SoulMotion {

    private static final double DIAGONAL_MULTIPLIER = Mth.sin((float) Math.toRadians(45));

    private SoulMotion() {
    }

    public static Vec3 compute(SoulCamera camera, double horizontalSpeed, double verticalSpeed) {
        var options = Minecraft.getInstance().options;
        float yaw = camera.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90);

        double velocityX = 0.0;
        double velocityY = 0.0;
        double velocityZ = 0.0;

        boolean straight = false;
        if (options.keyUp.isDown()) {
            velocityX += forward.x * horizontalSpeed;
            velocityZ += forward.z * horizontalSpeed;
            straight = true;
        }
        if (options.keyDown.isDown()) {
            velocityX -= forward.x * horizontalSpeed;
            velocityZ -= forward.z * horizontalSpeed;
            straight = true;
        }

        boolean strafing = false;
        if (options.keyRight.isDown()) {
            velocityX += side.x * horizontalSpeed;
            velocityZ += side.z * horizontalSpeed;
            strafing = true;
        }
        if (options.keyLeft.isDown()) {
            velocityX -= side.x * horizontalSpeed;
            velocityZ -= side.z * horizontalSpeed;
            strafing = true;
        }

        if (straight && strafing) {
            velocityX *= DIAGONAL_MULTIPLIER;
            velocityZ *= DIAGONAL_MULTIPLIER;
        }

        if (options.keyJump.isDown()) {
            velocityY += verticalSpeed;
        }
        if (options.keyShift.isDown()) {
            velocityY -= verticalSpeed;
        }

        return new Vec3(velocityX, velocityY, velocityZ);
    }
}
