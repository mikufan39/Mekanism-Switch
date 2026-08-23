package com.mikufan.meks.flight.config;

import com.mikufan.meks.Config;
import com.mikufan.meks.flight.api.event.RollContext;
import com.mikufan.meks.flight.api.rotation.RotationInstant;
import com.mikufan.meks.flight.math.ExpressionParser;

/**
 * Accessors for the flight controls configuration.
 *
 * <p>All options are defined in {@link Config} inside the {@code [flight]} section of
 * {@code config/Mekanism/meks-common.toml}. The flight behaviour is client-side, but because
 * the mod is required on both the server and the client, a single shared config file
 * is sufficient — there is no separate client-only flight config. The config is registered as
 * server-authoritative (ModConfig.Type.SERVER): clients follow the server's values.
 */
public final class MeksFlightConfig {

    /** How widely the flight controls activate. */
    public enum ActivationMode {
        /** Only while fall-flying with a MekaSuit chestplate (Elytra Unit module installed and enabled). */
        ELYTRA_UNIT,
        /** For any fall-flying player, regardless of equipment. */
        GLOBAL
    }

    private MeksFlightConfig() {
    }

    public static boolean getModEnabled() {
        // The config is server-authoritative and not loaded on the client main menu (no server
        // yet); treat as disabled until the server's synced values are available.
        return Config.isLoaded() && Config.FLIGHT_ENABLED.get();
    }

    public static ActivationMode getActivationMode() {
        return Config.FLIGHT_ACTIVATION_MODE.get();
    }

    public static boolean getSwitchRollAndYaw() {
        return Config.FLIGHT_SWITCH_ROLL_AND_YAW.get();
    }

    public static boolean getInvertPitch() {
        return Config.FLIGHT_INVERT_PITCH.get();
    }

    public static boolean getMomentumBasedMouse() {
        return Config.FLIGHT_MOMENTUM_BASED_MOUSE.get();
    }

    public static double getMomentumMouseDeadzone() {
        return Config.FLIGHT_MOMENTUM_MOUSE_DEADZONE.get();
    }

    public static boolean getDisableWhenSubmerged() {
        return Config.FLIGHT_DISABLE_WHEN_SUBMERGED.get();
    }

    public static boolean getEnableBanking() {
        return Config.FLIGHT_ENABLE_BANKING.get();
    }

    public static double getBankingStrength() {
        return Config.FLIGHT_BANKING_STRENGTH.get();
    }

    public static boolean getSimulateControlSurfaceEfficacy() {
        return Config.FLIGHT_SIMULATE_CONTROL_SURFACE_EFFICACY.get();
    }

    public static boolean getAutomaticRighting() {
        return Config.FLIGHT_AUTOMATIC_RIGHTING.get();
    }

    public static double getRightingStrength() {
        return Config.FLIGHT_RIGHTING_STRENGTH.get();
    }

    public static double getSensitivityPitch() {
        return Config.FLIGHT_SENSITIVITY_PITCH.get();
    }

    public static double getSensitivityYaw() {
        return Config.FLIGHT_SENSITIVITY_YAW.get();
    }

    public static double getSensitivityRoll() {
        return Config.FLIGHT_SENSITIVITY_ROLL.get();
    }

    public static double getDesktopPitch() {
        return getSensitivityPitch();
    }

    public static double getDesktopYaw() {
        return getSensitivityYaw();
    }

    public static double getDesktopRoll() {
        return getSensitivityRoll();
    }

    public static Sensitivity getDesktopSensitivity() {
        return new Sensitivity(getDesktopPitch(), getDesktopYaw(), getDesktopRoll());
    }

    public static double getSmoothingPitch() {
        return Config.FLIGHT_SMOOTHING_PITCH.get();
    }

    public static double getSmoothingYaw() {
        return Config.FLIGHT_SMOOTHING_YAW.get();
    }

    public static double getSmoothingRoll() {
        return Config.FLIGHT_SMOOTHING_ROLL.get();
    }

    public static Sensitivity getSmoothing() {
        return new Sensitivity(getSmoothingPitch(), getSmoothingYaw(), getSmoothingRoll());
    }

    // ExpressionParser construction is lazy (parsing happens on first build()/eval), so creating one from
    // a config string can never fail here; invalid formulas surface as eval-time errors inside ExpressionParser.
    public static ExpressionParser getBankingXFormula() {
        return new ExpressionParser(Config.FLIGHT_BANKING_X_FORMULA.get());
    }

    public static ExpressionParser getBankingYFormula() {
        return new ExpressionParser(Config.FLIGHT_BANKING_Y_FORMULA.get());
    }

    public static ExpressionParser getElevatorEfficacyFormula() {
        return new ExpressionParser(Config.FLIGHT_ELEVATOR_EFFICACY_FORMULA.get());
    }

    public static ExpressionParser getAileronEfficacyFormula() {
        return new ExpressionParser(Config.FLIGHT_AILERON_EFFICACY_FORMULA.get());
    }

    public static ExpressionParser getRudderEfficacyFormula() {
        return new ExpressionParser(Config.FLIGHT_RUDDER_EFFICACY_FORMULA.get());
    }

    // Copied from DABR ModConfig.configureRotation. The context parameter is unused there and kept
    // only for signature compatibility with RollContext.ConfiguresRotation.
    public static RotationInstant configureRotation(RotationInstant rotationInstant, RollContext context) {
        var pitch = rotationInstant.pitch();
        var yaw = rotationInstant.yaw();
        var roll = rotationInstant.roll();

        if (!getSwitchRollAndYaw()) {
            var temp = yaw;
            yaw = roll;
            roll = temp;
        }
        if (getInvertPitch()) {
            pitch = -pitch;
        }

        return RotationInstant.of(pitch, yaw, roll);
    }
}