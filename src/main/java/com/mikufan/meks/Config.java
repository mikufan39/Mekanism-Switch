package com.mikufan.meks;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MEKA_SUIT_ENCHANTMENT = BUILDER
            .comment("Allow the MekaSuit enchanting table feature: only Protection V on the third slot, costing 60 levels.")
            .define("mekaSuitEnchantment", true);

    public static final ModConfigSpec.BooleanValue CREEPER_NO_BLOCK_DAMAGE = BUILDER
            .comment("Prevent creeper explosions from destroying blocks. Entity damage still applies.")
            .define("creeperNoBlockDamage", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
