package com.mikufan.meks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    public static final String COMMON_FILE = "Mekanism/meks-common.toml";
    public static final String CLIENT_FILE = "Mekanism/meks-client.toml";

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    static {
        COMMON_BUILDER.push("server");
    }

    public static final ModConfigSpec.BooleanValue MEKA_SUIT_ENCHANTMENT = COMMON_BUILDER
            .comment("Allow the MekaSuit enchanting table feature: only Protection V on the third slot.")
            .define("mekaSuitEnchantment", true);

    public static final ModConfigSpec.IntValue MEKA_SUIT_ENCHANT_COST = COMMON_BUILDER
            .comment("Experience levels charged when enchanting a MekaSuit item.")
            .defineInRange("mekaSuitEnchantCost", 30, 0, 100);

    public static final ModConfigSpec.BooleanValue CREEPER_NO_BLOCK_DAMAGE = COMMON_BUILDER
            .comment("Prevent creeper explosions from destroying blocks. Entity damage still applies.")
            .define("creeperNoBlockDamage", true);

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
            .defineInRange("downloadFePerSv", 4L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue EXCHANGE_UPLOAD_TICKS_PER_SV = COMMON_BUILDER
            .comment("Additional processing ticks per SV when uploading.")
            .defineInRange("uploadTicksPerSv", 0.1D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue EXCHANGE_DOWNLOAD_TICKS_PER_SV = COMMON_BUILDER
            .comment("Additional processing ticks per SV when downloading.")
            .defineInRange("downloadTicksPerSv", 0.2D, 0.0D, 100.0D);

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
    }

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    static {
        CLIENT_BUILDER.push("soulOut");
    }

    public static final ModConfigSpec.BooleanValue SOUL_OUT_ENABLED = CLIENT_BUILDER
            .comment("Enable the soul out-of-body feature: press the keybind while wearing a MekaSuit helmet.")
            .define("enabled", true);

    public static final ModConfigSpec.LongValue SOUL_BASE_COST_PER_TICK = CLIENT_BUILDER
            .comment("Starting energy drained from the MekaSuit helmet every tick while outside the body. Creative mode does not drain energy.")
            .defineInRange("baseCostPerTick", 500L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue SOUL_COST_DOUBLING_SECONDS = CLIENT_BUILDER
            .comment("Seconds for the per-tick energy cost to double. 0 disables growth and keeps the base cost constant.")
            .defineInRange("costDoublingSeconds", 45, 0, 3600);

    public static final ModConfigSpec.DoubleValue SOUL_HORIZONTAL_SPEED = CLIENT_BUILDER
            .comment("Horizontal movement speed of the soul camera.")
            .defineInRange("horizontalSpeed", 1.0D, 0.01D, 10.0D);

    public static final ModConfigSpec.DoubleValue SOUL_VERTICAL_SPEED = CLIENT_BUILDER
            .comment("Vertical movement speed of the soul camera.")
            .defineInRange("verticalSpeed", 1.0D, 0.01D, 10.0D);

    public static final ModConfigSpec.BooleanValue SOUL_FREEZE_BODY = CLIENT_BUILDER
            .comment("Freeze the player's body in place while the soul is outside. Client-side only; some servers may pull the body back.")
            .define("freezeBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_SHOW_BODY = CLIENT_BUILDER
            .comment("Render the player's body at its original position while the soul is outside.")
            .define("showBody", true);

    public static final ModConfigSpec.BooleanValue SOUL_DISABLE_ON_DAMAGE = CLIENT_BUILDER
            .comment("Automatically return to the body when the player takes damage (survival/adventure only).")
            .define("disableOnDamage", true);

    static {
        CLIENT_BUILDER.pop();
    }

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private Config() {
    }
}
