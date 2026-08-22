package com.mikufan.meks.flight;

import com.mikufan.meks.flight.api.event.RollContext;
import com.mikufan.meks.flight.api.rotation.RotationInstant;
import com.mikufan.meks.flight.config.MeksFlightConfig;
import com.mikufan.meks.flight.config.Sensitivity;
import com.mikufan.meks.flight.math.MagicNumbers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SmoothDouble;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RotationModifiers {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final double ROLL_REORIENT_CUTOFF = Math.sqrt(10.0 / 3.0);

    public static RollContext.ConfiguresRotation buttonControls(double power) {
        return (rotationInstant, context) -> {
            var delta = power * context.getRenderDelta();
            var pitch = 0.0;
            var yaw = 0.0;
            var roll = 0.0;

            if (MeksFlightKeybinds.PITCH_UP.isDown()) {
                pitch -= delta;
            }
            if (MeksFlightKeybinds.PITCH_DOWN.isDown()) {
                pitch += delta;
            }
            if (MeksFlightKeybinds.YAW_LEFT.isDown()) {
                yaw -= delta;
            }
            if (MeksFlightKeybinds.YAW_RIGHT.isDown()) {
                yaw += delta;
            }
            if (MeksFlightKeybinds.ROLL_LEFT.isDown()) {
                roll -= delta;
            }
            if (MeksFlightKeybinds.ROLL_RIGHT.isDown()) {
                roll += delta;
            }

            // Putting this in the roll value, since it'll be swapped later
            return rotationInstant.add(pitch, yaw, roll);
        };
    }

    public static RollContext.ConfiguresRotation smoothing(SmoothDouble pitchSmoother, SmoothDouble yawSmoother, SmoothDouble rollSmoother, Sensitivity smoothness) {
        return (rotationInstant, context) -> RotationInstant.of(
                smoothness.pitch == 0 ? rotationInstant.pitch() : pitchSmoother.getNewDeltaValue(rotationInstant.pitch(), 1 / smoothness.pitch * context.getRenderDelta()),
                smoothness.yaw == 0 ? rotationInstant.yaw() : yawSmoother.getNewDeltaValue(rotationInstant.yaw(), 1 / smoothness.yaw * context.getRenderDelta()),
                smoothness.roll == 0 ? rotationInstant.roll() : rollSmoother.getNewDeltaValue(rotationInstant.roll(), 1 / smoothness.roll * context.getRenderDelta())
        );
    }

    public static RotationInstant banking(RotationInstant rotationInstant, RollContext context) {
        var delta = context.getRenderDelta();
        var currentRotation = context.getCurrentRotation();
        var currentRoll = currentRotation.roll() * MagicNumbers.TORAD;

        var xExpression = MeksFlightConfig.getBankingXFormula().getCompiledOrDefaulting(0);
        var yExpression = MeksFlightConfig.getBankingYFormula().getCompiledOrDefaulting(0);

        var vars = getVars(context);
        vars.put("banking_strength", MeksFlightConfig.getBankingStrength());

        var dX = xExpression.eval(vars);
        var dY = yExpression.eval(vars);

        // check if we accidentally got NaN, for some reason this happens sometimes
        if (Double.isNaN(dX)) dX = 0;
        if (Double.isNaN(dY)) dY = 0;

        return rotationInstant.addAbsolute(dX * delta, dY * delta, currentRoll);
    }

    public static RotationInstant reorient(RotationInstant rotationInstant, RollContext context) {
        var delta = context.getRenderDelta();
        var currentRoll = context.getCurrentRotation().roll() * MagicNumbers.TORAD;
        var strength = 10 * MeksFlightConfig.getRightingStrength();

        var cutoff = ROLL_REORIENT_CUTOFF;
        double rollDelta = 0;
        if (-cutoff < currentRoll && currentRoll < cutoff) {
            rollDelta = -Math.pow(currentRoll, 3) / 3.0 + currentRoll; //0.1 * Math.pow(currentRoll, 5);
        }

        return rotationInstant.add(0, 0, -rollDelta * strength * delta);
    }

    public static RollContext.ConfiguresRotation fixNaN(String name) {
        return (rotationInstant, context) -> {
            if (Double.isNaN(rotationInstant.pitch())) {
                rotationInstant = RotationInstant.of(0, rotationInstant.yaw(), rotationInstant.roll());
                LOGGER.warn("NaN found in pitch for {}, setting to 0 as fallback", name);
            }
            if (Double.isNaN(rotationInstant.yaw())) {
                rotationInstant = RotationInstant.of(rotationInstant.pitch(), 0, rotationInstant.roll());
                LOGGER.warn("NaN found in yaw for {}, setting to 0 as fallback", name);
            }
            if (Double.isNaN(rotationInstant.roll())) {
                rotationInstant = RotationInstant.of(rotationInstant.pitch(), rotationInstant.yaw(), 0);
                LOGGER.warn("NaN found in roll for {}, setting to 0 as fallback", name);
            }
            return rotationInstant;
        };
    }

    public static RotationInstant applyControlSurfaceEfficacy(RotationInstant rotationInstant, RollContext context) {
        var elevatorExpression = MeksFlightConfig.getElevatorEfficacyFormula().getCompiledOrDefaulting(1);
        var aileronExpression = MeksFlightConfig.getAileronEfficacyFormula().getCompiledOrDefaulting(1);
        var rudderExpression = MeksFlightConfig.getRudderEfficacyFormula().getCompiledOrDefaulting(1);

        var vars = getVars(context);
        return rotationInstant.multiply(elevatorExpression.eval(vars), rudderExpression.eval(vars), aileronExpression.eval(vars));
    }

    private static Map<String, Double> getVars(RollContext context) {
        var player = Minecraft.getInstance().player;
        assert player != null;

        var currentRotation = context.getCurrentRotation();
        var rotationVector = player.getViewVector(1.0F);
        return new HashMap<>() {{
            put("pitch", currentRotation.pitch());
            put("yaw", currentRotation.yaw());
            put("roll", currentRotation.roll());
            put("velocity_length", player.getDeltaMovement().length());
            put("velocity_x", player.getDeltaMovement().x());
            put("velocity_y", player.getDeltaMovement().y());
            put("velocity_z", player.getDeltaMovement().z());
            put("look_x", rotationVector.x());
            put("look_y", rotationVector.y());
            put("look_z", rotationVector.z());
        }};
    }
}