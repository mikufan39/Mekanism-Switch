package com.mikufan.meks.mixin;

import com.mikufan.meks.Config;
import java.util.List;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    private static final int MEKA_SUIT_ENCHANT_ROW = 2;
    private static final int MEKA_SUIT_PROTECTION_LEVEL = 5;

    @Inject(method = "getEnchantmentList", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$forceProtectionV(RegistryAccess registryAccess, ItemStack stack, int slot, int cost,
          CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (Config.MEKA_SUIT_ENCHANTMENT.get() && stack.getItem() instanceof ItemMekaSuitArmor) {
            if (slot == MEKA_SUIT_ENCHANT_ROW) {
                Holder<Enchantment> protection = registryAccess.registryOrThrow(Registries.ENCHANTMENT)
                      .getHolderOrThrow(Enchantments.PROTECTION);
                cir.setReturnValue(List.of(new EnchantmentInstance(protection, MEKA_SUIT_PROTECTION_LEVEL)));
            } else {
                cir.setReturnValue(List.of());
            }
        }
    }
}
