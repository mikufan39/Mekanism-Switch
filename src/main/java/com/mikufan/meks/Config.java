package com.mikufan.meks;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MEKA_SUIT_ENCHANTMENT = BUILDER
            .comment("Allow the MekaSuit enchanting table feature: only Protection V on the third slot, costing 30 levels.")
            .define("mekaSuitEnchantment", true);

    public static final ModConfigSpec.BooleanValue MEKA_SUIT_FLIGHT_CONTROLS = BUILDER
            .comment("Enable three-axis flight controls (pitch/yaw/roll) while elytra-flying with a MekaSuit chestplate. Drains energy from the chestplate.")
            .define("mekaSuitFlightControls", true);

    public static final ModConfigSpec.BooleanValue CREEPER_NO_BLOCK_DAMAGE = BUILDER
            .comment("Prevent creeper explosions from destroying blocks. Entity damage still applies.")
            .define("creeperNoBlockDamage", true);

    static {
        BUILDER.push("soulOut");
    }

    public static final ModConfigSpec.BooleanValue SOUL_OUT_ENABLED = BUILDER
            .comment("Enable the soul out-of-body feature: press the keybind while wearing a MekaSuit helmet.")
            .define("enabled", true);

    public static final ModConfigSpec.LongValue SOUL_BASE_COST_PER_TICK = BUILDER
            .comment("Starting energy drained from the MekaSuit helmet every tick while outside the body. Creative mode does not drain energy.")
            .defineInRange("baseCostPerTick", 500L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue SOUL_COST_DOUBLING_SECONDS = BUILDER
            .comment("Seconds for the per-tick energy cost to double. 0 disables growth and keeps the base cost constant.")
            .defineInRange("costDoublingSeconds", 45, 0, 3600);

    public static final ModConfigSpec.DoubleValue SOUL_HORIZONTAL_SPEED = BUILDER
            .comment("Horizontal movement speed of the soul camera.")
            .defineInRange("horizontalSpeed", 1.0, 0.01, 10.0);

    public static final ModConfigSpec.DoubleValue SOUL_VERTICAL_SPEED = BUILDER
            .comment("Vertical movement speed of the soul camera.")
            .defineInRange("verticalSpeed", 1.0, 0.01, 10.0);

    public static final ModConfigSpec.BooleanValue SOUL_FREEZE_BODY = BUILDER
            .comment("Freeze the player's body in place while the soul is outside. Client-side only; some servers may pull the body back.")
            .define("freezeBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_SHOW_BODY = BUILDER
            .comment("Render the player's body at its original position while the soul is outside.")
            .define("showBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_DISABLE_ON_DAMAGE = BUILDER
            .comment("Automatically return to the body when the player takes damage (survival/adventure only).")
            .define("disableOnDamage", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
