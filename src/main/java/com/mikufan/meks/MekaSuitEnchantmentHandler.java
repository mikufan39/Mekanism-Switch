package com.mikufan.meks;

import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;

public final class MekaSuitEnchantmentHandler {

    public static final int MEKA_SUIT_ENCHANT_ROW = 2;

    /** Fixed experience level cost for enchanting MekaSuit items (previously configurable via mekaSuitEnchantCost). */
    public static final int MEKA_SUIT_ENCHANT_COST = 30;

    private MekaSuitEnchantmentHandler() {
    }

    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        if (Config.MEKA_SUIT_ENCHANTMENT.get() && event.getItem().getItem() instanceof ItemMekaSuitArmor) {
            event.setEnchantLevel(event.getEnchantRow() == MEKA_SUIT_ENCHANT_ROW ? MEKA_SUIT_ENCHANT_COST : 0);
        }
    }
}
