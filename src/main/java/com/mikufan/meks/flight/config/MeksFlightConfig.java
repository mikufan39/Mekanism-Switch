package com.mikufan.meks.flight.config;

import com.mikufan.meks.flight.api.event.RollContext;
import com.mikufan.meks.flight.api.rotation.RotationInstant;
import com.mikufan.meks.flight.math.ExpressionParser;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MeksFlightConfig {

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    static {
        CLIENT_BUILDER.push("flight");
    }

    public static final ModConfigSpec.BooleanValue ENABLED = CLIENT_BUILDER
            .comment("Master switch for the MekaSuit flight controls.")
            .define("enabled", true);

    public static final ModConfigSpec.LongValue FLIGHT_ENERGY_PER_TICK = CLIENT_BUILDER
            .comment("Energy in J drained from the MekaSuit chestplate every tick while flight controls are active.")
            .defineInRange("flightEnergyPerTick", 100L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SWITCH_ROLL_AND_YAW = CLIENT_BUILDER
            .comment("Switch the roll and yaw mouse axes.")
            .define("switchRollAndYaw", false);

    public static final ModConfigSpec.BooleanValue INVERT_PITCH = CLIENT_BUILDER
            .comment("Invert the pitch mouse axis.")
            .define("invertPitch", false);

    public static final ModConfigSpec.BooleanValue MOMENTUM_BASED_MOUSE = CLIENT_BUILDER
            .comment("Use momentum-based camera movement instead of direct mouse control.")
            .define("momentumBasedMouse", false);

    public static final ModConfigSpec.DoubleValue MOMENTUM_MOUSE_DEADZONE = CLIENT_BUILDER
            .comment("Deadzone of the momentum-based mouse in blocks per second.")
            .defineInRange("momentumMouseDeadzone", 0.2D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue SHOW_MOMENTUM_WIDGET = CLIENT_BUILDER
            .comment("Show the momentum widget in the HUD.")
            .define("showMomentumWidget", true);

    public static final ModConfigSpec.BooleanValue DISABLE_WHEN_SUBMERGED = CLIENT_BUILDER
            .comment("Disable flight controls when the player is submerged in water.")
            .define("disableWhenSubmerged", true);

    public static final ModConfigSpec.DoubleValue SENSITIVITY_PITCH = CLIENT_BUILDER
            .comment("Pitch sensitivity of the mouse controls.")
            .defineInRange("sensitivityPitch", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue SENSITIVITY_YAW = CLIENT_BUILDER
            .comment("Yaw sensitivity of the mouse controls.")
            .defineInRange("sensitivityYaw", 0.4D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue SENSITIVITY_ROLL = CLIENT_BUILDER
            .comment("Roll sensitivity of the mouse controls.")
            .defineInRange("sensitivityRoll", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue SMOOTHING_PITCH = CLIENT_BUILDER
            .comment("Smoothing applied to the pitch axis.")
            .defineInRange("smoothingPitch", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue SMOOTHING_YAW = CLIENT_BUILDER
            .comment("Smoothing applied to the yaw axis.")
            .defineInRange("smoothingYaw", 2.5D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue SMOOTHING_ROLL = CLIENT_BUILDER
            .comment("Smoothing applied to the roll axis.")
            .defineInRange("smoothingRoll", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue ENABLE_BANKING = CLIENT_BUILDER
            .comment("Enable banking (camera roll from turning).")
            .define("enableBanking", true);

    public static final ModConfigSpec.DoubleValue BANKING_STRENGTH = CLIENT_BUILDER
            .comment("How strongly the camera banks when turning.")
            .defineInRange("bankingStrength", 20.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue SIMULATE_CONTROL_SURFACE_EFFICACY = CLIENT_BUILDER
            .comment("Simulate control surface efficacy based on velocity relative to the look direction.")
            .define("simulateControlSurfaceEfficacy", false);

    public static final ModConfigSpec.BooleanValue AUTOMATIC_RIGHTING = CLIENT_BUILDER
            .comment("Automatically right the camera to level flight.")
            .define("automaticRighting", false);

    public static final ModConfigSpec.DoubleValue RIGHTING_STRENGTH = CLIENT_BUILDER
            .comment("How strongly the camera rights itself when automatic righting is enabled.")
            .defineInRange("rightingStrength", 50.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue SHOW_HORIZON = CLIENT_BUILDER
            .comment("Show a horizon indicator in the HUD.")
            .define("showHorizon", false);

    public static final ModConfigSpec.ConfigValue<String> BANKING_X_FORMULA = CLIENT_BUILDER
            .comment("Formula for the banking camera offset on the X axis.")
            .define("bankingXFormula", "sin($roll * TO_RAD) * cos($pitch * TO_RAD) * 10 * $banking_strength");

    public static final ModConfigSpec.ConfigValue<String> BANKING_Y_FORMULA = CLIENT_BUILDER
            .comment("Formula for the banking camera offset on the Y axis.")
            .define("bankingYFormula", "(-1 + cos($roll * TO_RAD)) * cos($pitch * TO_RAD) * 10 * $banking_strength");

    public static final ModConfigSpec.ConfigValue<String> ELEVATOR_EFFICACY_FORMULA = CLIENT_BUILDER
            .comment("Formula for elevator (pitch) control surface efficacy.")
            .define("elevatorEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    public static final ModConfigSpec.ConfigValue<String> AILERON_EFFICACY_FORMULA = CLIENT_BUILDER
            .comment("Formula for aileron (roll) control surface efficacy.")
            .define("aileronEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    public static final ModConfigSpec.ConfigValue<String> RUDDER_EFFICACY_FORMULA = CLIENT_BUILDER
            .comment("Formula for rudder (yaw) control surface efficacy.")
            .define("rudderEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    static {
        CLIENT_BUILDER.pop();
    }

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private MeksFlightConfig() {
    }

    public static boolean getModEnabled() {
        return ENABLED.get();
    }

    public static boolean getSwitchRollAndYaw() {
        return SWITCH_ROLL_AND_YAW.get();
    }

    public static boolean getInvertPitch() {
        return INVERT_PITCH.get();
    }

    public static boolean getMomentumBasedMouse() {
        return MOMENTUM_BASED_MOUSE.get();
    }

    public static double getMomentumMouseDeadzone() {
        return MOMENTUM_MOUSE_DEADZONE.get();
    }

    public static boolean getShowMomentumWidget() {
        return SHOW_MOMENTUM_WIDGET.get();
    }

    public static boolean getDisableWhenSubmerged() {
        return DISABLE_WHEN_SUBMERGED.get();
    }

    public static boolean getShowHorizon() {
        return SHOW_HORIZON.get();
    }

    public static boolean getEnableBanking() {
        return ENABLE_BANKING.get();
    }

    public static double getBankingStrength() {
        return BANKING_STRENGTH.get();
    }

    public static boolean getSimulateControlSurfaceEfficacy() {
        return SIMULATE_CONTROL_SURFACE_EFFICACY.get();
    }

    public static boolean getAutomaticRighting() {
        return AUTOMATIC_RIGHTING.get();
    }

    public static double getRightingStrength() {
        return RIGHTING_STRENGTH.get();
    }

    public static long getFlightEnergyPerTick() {
        return FLIGHT_ENERGY_PER_TICK.get();
    }

    public static double getSensitivityPitch() {
        return SENSITIVITY_PITCH.get();
    }

    public static double getSensitivityYaw() {
        return SENSITIVITY_YAW.get();
    }

    public static double getSensitivityRoll() {
        return SENSITIVITY_ROLL.get();
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
        return SMOOTHING_PITCH.get();
    }

    public static double getSmoothingYaw() {
        return SMOOTHING_YAW.get();
    }

    public static double getSmoothingRoll() {
        return SMOOTHING_ROLL.get();
    }

    public static Sensitivity getSmoothing() {
        return new Sensitivity(getSmoothingPitch(), getSmoothingYaw(), getSmoothingRoll());
    }

    // ExpressionParser construction is lazy (parsing happens on first build()/eval), so creating one from
    // a config string can never fail here; invalid formulas surface as eval-time errors inside ExpressionParser.
    public static ExpressionParser getBankingXFormula() {
        return new ExpressionParser(BANKING_X_FORMULA.get());
    }

    public static ExpressionParser getBankingYFormula() {
        return new ExpressionParser(BANKING_Y_FORMULA.get());
    }

    public static ExpressionParser getElevatorEfficacyFormula() {
        return new ExpressionParser(ELEVATOR_EFFICACY_FORMULA.get());
    }

    public static ExpressionParser getAileronEfficacyFormula() {
        return new ExpressionParser(AILERON_EFFICACY_FORMULA.get());
    }

    public static ExpressionParser getRudderEfficacyFormula() {
        return new ExpressionParser(RUDDER_EFFICACY_FORMULA.get());
    }

    public static void setModEnabled(boolean enabled) {
        ENABLED.set(enabled);
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