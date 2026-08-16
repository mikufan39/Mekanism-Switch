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

    static final ModConfigSpec SPEC = BUILDER.build();
}
