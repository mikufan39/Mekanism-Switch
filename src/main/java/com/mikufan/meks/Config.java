package com.mikufan.meks;

import com.mikufan.meks.flight.config.MeksFlightConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    /** Single config file for everything; registered as server-authoritative (ModConfig.Type.SERVER),
     *  the server's values are synced to connecting clients. */
    public static final String COMMON_FILE = "Mekanism/meks-common.toml";

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

    static {
        COMMON_BUILDER.push("server");
    }

    public static final ModConfigSpec.BooleanValue MEKA_SUIT_ENCHANTMENT = COMMON_BUILDER
            .comment("Allow the MekaSuit enchanting table feature: only Protection V on the third slot.")
            .define("mekaSuitEnchantment", false);

    public static final ModConfigSpec.BooleanValue CREEPER_NO_BLOCK_DAMAGE = COMMON_BUILDER
            .comment("Prevent creeper explosions from destroying blocks. Entity damage still applies.")
            .define("creeperNoBlockDamage", false);

    static {
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("machine");
        COMMON_BUILDER.push("exchangeSwitch");
    }

    public static final ModConfigSpec.LongValue EXCHANGE_UPLOAD_FE_PER_SV = COMMON_BUILDER
            .comment("Total energy in FE consumed per SV when uploading.")
            .defineInRange("uploadFePerSv", 2L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue EXCHANGE_DOWNLOAD_FE_PER_SV = COMMON_BUILDER
            .comment("Total energy in FE consumed per SV when downloading.")
            .defineInRange("downloadFePerSv", 2L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue EXCHANGE_UPLOAD_TICKS_PER_SV = COMMON_BUILDER
            .comment("Additional processing ticks per SV when uploading.")
            .defineInRange("uploadTicksPerSv", 0.1D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue EXCHANGE_DOWNLOAD_TICKS_PER_SV = COMMON_BUILDER
            .comment("Additional processing ticks per SV when downloading.")
            .defineInRange("downloadTicksPerSv", 0.1D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue EXCHANGE_MIN_TICKS = COMMON_BUILDER
            .comment("Minimum processing ticks for an exchange operation.")
            .defineInRange("minTicks", 20, 1, 10000);

    public static final ModConfigSpec.IntValue EXCHANGE_MAX_TICKS = COMMON_BUILDER
            .comment("Maximum processing ticks for an exchange operation.")
            .defineInRange("maxTicks", 600, 1, 10000);

    static {
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("restorationSwitch");
    }

    public static final ModConfigSpec.LongValue RESTORATION_FALLBACK_SV_COST = COMMON_BUILDER
            .comment("SV cost per repair point for items without a known SV value.")
            .defineInRange("fallbackSvCost", 616L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue RESTORATION_ENERGY_PER_SV = COMMON_BUILDER
            .comment("Total energy in FE consumed per SV when repairing.")
            .defineInRange("energyPerSv", 2L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue RESTORATION_TICKS_PER_SV = COMMON_BUILDER
            .comment("Additional processing ticks per SV when repairing.")
            .defineInRange("ticksPerSv", 0.1D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue RESTORATION_MIN_TICKS = COMMON_BUILDER
            .comment("Minimum processing ticks for a repair attempt.")
            .defineInRange("minTicks", 20, 1, 10000);

    public static final ModConfigSpec.IntValue RESTORATION_MAX_TICKS = COMMON_BUILDER
            .comment("Maximum processing ticks for a repair attempt.")
            .defineInRange("maxTicks", 600, 1, 10000);

    static {
        COMMON_BUILDER.pop();
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("flight");
    }

    // Flight controls. The behaviour is client-side, but because the mod is required on both the
    // server and the client, these settings live in the shared common config file instead of a
    // separate client-only flight config. The config is server-authoritative: clients use the
    // server's values.
    public static final ModConfigSpec.BooleanValue FLIGHT_ENABLED = COMMON_BUILDER
            .comment("Master switch for the flight controls (default off). Read from the config file only; there is no in-game toggle key.")
            .define("enabled", false);

    public static final ModConfigSpec.EnumValue<MeksFlightConfig.ActivationMode> FLIGHT_ACTIVATION_MODE = COMMON_BUILDER
            .comment("When flight controls are enabled: ELYTRA_UNIT (default) activates only while fall-flying with a MekaSuit chestplate (Elytra Unit module installed and enabled); GLOBAL activates for any fall-flying player (e.g. vanilla elytra).")
            .defineEnum("activationMode", MeksFlightConfig.ActivationMode.ELYTRA_UNIT);

    public static final ModConfigSpec.BooleanValue FLIGHT_SWITCH_ROLL_AND_YAW = COMMON_BUILDER
            .comment("Switch the roll and yaw mouse axes.")
            .define("switchRollAndYaw", false);

    public static final ModConfigSpec.BooleanValue FLIGHT_INVERT_PITCH = COMMON_BUILDER
            .comment("Invert the pitch mouse axis.")
            .define("invertPitch", false);

    public static final ModConfigSpec.BooleanValue FLIGHT_MOMENTUM_BASED_MOUSE = COMMON_BUILDER
            .comment("Use momentum-based camera movement instead of direct mouse control.")
            .define("momentumBasedMouse", false);

    public static final ModConfigSpec.DoubleValue FLIGHT_MOMENTUM_MOUSE_DEADZONE = COMMON_BUILDER
            .comment("Deadzone of the momentum-based mouse in blocks per second.")
            .defineInRange("momentumMouseDeadzone", 0.2D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue FLIGHT_DISABLE_WHEN_SUBMERGED = COMMON_BUILDER
            .comment("Disable flight controls when the player is submerged in water.")
            .define("disableWhenSubmerged", true);

    public static final ModConfigSpec.DoubleValue FLIGHT_SENSITIVITY_PITCH = COMMON_BUILDER
            .comment("Pitch sensitivity of the mouse controls.")
            .defineInRange("sensitivityPitch", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue FLIGHT_SENSITIVITY_YAW = COMMON_BUILDER
            .comment("Yaw sensitivity of the mouse controls.")
            .defineInRange("sensitivityYaw", 0.4D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue FLIGHT_SENSITIVITY_ROLL = COMMON_BUILDER
            .comment("Roll sensitivity of the mouse controls.")
            .defineInRange("sensitivityRoll", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue FLIGHT_SMOOTHING_PITCH = COMMON_BUILDER
            .comment("Smoothing applied to the pitch axis.")
            .defineInRange("smoothingPitch", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue FLIGHT_SMOOTHING_YAW = COMMON_BUILDER
            .comment("Smoothing applied to the yaw axis.")
            .defineInRange("smoothingYaw", 2.5D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue FLIGHT_SMOOTHING_ROLL = COMMON_BUILDER
            .comment("Smoothing applied to the roll axis.")
            .defineInRange("smoothingRoll", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue FLIGHT_ENABLE_BANKING = COMMON_BUILDER
            .comment("Enable banking (camera roll from turning).")
            .define("enableBanking", true);

    public static final ModConfigSpec.DoubleValue FLIGHT_BANKING_STRENGTH = COMMON_BUILDER
            .comment("How strongly the camera banks when turning.")
            .defineInRange("bankingStrength", 20.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue FLIGHT_SIMULATE_CONTROL_SURFACE_EFFICACY = COMMON_BUILDER
            .comment("Simulate control surface efficacy based on velocity relative to the look direction.")
            .define("simulateControlSurfaceEfficacy", false);

    public static final ModConfigSpec.BooleanValue FLIGHT_AUTOMATIC_RIGHTING = COMMON_BUILDER
            .comment("Automatically right the camera to level flight.")
            .define("automaticRighting", false);

    public static final ModConfigSpec.DoubleValue FLIGHT_RIGHTING_STRENGTH = COMMON_BUILDER
            .comment("How strongly the camera rights itself when automatic righting is enabled.")
            .defineInRange("rightingStrength", 50.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.ConfigValue<String> FLIGHT_BANKING_X_FORMULA = COMMON_BUILDER
            .comment("Formula for the banking camera offset on the X axis.")
            .define("bankingXFormula", "sin($roll * TO_RAD) * cos($pitch * TO_RAD) * 10 * $banking_strength");

    public static final ModConfigSpec.ConfigValue<String> FLIGHT_BANKING_Y_FORMULA = COMMON_BUILDER
            .comment("Formula for the banking camera offset on the Y axis.")
            .define("bankingYFormula", "(-1 + cos($roll * TO_RAD)) * cos($pitch * TO_RAD) * 10 * $banking_strength");

    public static final ModConfigSpec.ConfigValue<String> FLIGHT_ELEVATOR_EFFICACY_FORMULA = COMMON_BUILDER
            .comment("Formula for elevator (pitch) control surface efficacy.")
            .define("elevatorEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    public static final ModConfigSpec.ConfigValue<String> FLIGHT_AILERON_EFFICACY_FORMULA = COMMON_BUILDER
            .comment("Formula for aileron (roll) control surface efficacy.")
            .define("aileronEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    public static final ModConfigSpec.ConfigValue<String> FLIGHT_RUDDER_EFFICACY_FORMULA = COMMON_BUILDER
            .comment("Formula for rudder (yaw) control surface efficacy.")
            .define("rudderEfficacyFormula", "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z");

    static {
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("soulOut");
    }

    // Soul out-of-body. Pure client behaviour, but the mod is required on both the server and
    // the client and the config is server-authoritative, so these settings live in the shared
    // common config file; clients follow the server's values.
    public static final ModConfigSpec.BooleanValue SOUL_OUT_ENABLED = COMMON_BUILDER
            .comment("Enable the soul out-of-body feature: press the keybind while wearing a MekaSuit helmet.")
            .define("enabled", true);

    public static final ModConfigSpec.LongValue SOUL_BASE_COST_PER_TICK = COMMON_BUILDER
            .comment("Starting energy drained from the MekaSuit helmet every tick while outside the body. Creative mode does not drain energy.")
            .defineInRange("baseCostPerTick", 500L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue SOUL_COST_DOUBLING_SECONDS = COMMON_BUILDER
            .comment("Seconds for the per-tick energy cost to double. 0 disables growth and keeps the base cost constant.")
            .defineInRange("costDoublingSeconds", 45, 0, 3600);

    public static final ModConfigSpec.DoubleValue SOUL_HORIZONTAL_SPEED = COMMON_BUILDER
            .comment("Horizontal movement speed of the soul camera.")
            .defineInRange("horizontalSpeed", 1.0D, 0.01D, 10.0D);

    public static final ModConfigSpec.DoubleValue SOUL_VERTICAL_SPEED = COMMON_BUILDER
            .comment("Vertical movement speed of the soul camera.")
            .defineInRange("verticalSpeed", 1.0D, 0.01D, 10.0D);

    public static final ModConfigSpec.BooleanValue SOUL_FREEZE_BODY = COMMON_BUILDER
            .comment("Freeze the player's body in place while the soul is outside. Client-side only; some servers may pull the body back.")
            .define("freezeBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_SHOW_BODY = COMMON_BUILDER
            .comment("Render the player's body at its original position while the soul is outside.")
            .define("showBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_DISABLE_ON_DAMAGE = COMMON_BUILDER
            .comment("Automatically return to the body when the player takes damage (survival/adventure only).")
            .define("disableOnDamage", true);

    static {
        COMMON_BUILDER.pop();
    }

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    /**
     * Whether the config has been loaded. Server-authoritative (ModConfig.Type.SERVER) configs
     * are only loaded on the server (or synced to a connected client); on the client main menu
     * — before joining any server — {@link ModConfigSpec.ConfigValue#get()} must not be called,
     * it throws. Guard per-tick / pre-join reads with this.
     */
    public static boolean isLoaded() {
        return COMMON_SPEC.isLoaded();
    }

    private Config() {
    }
}
