package com.mikufan.meks.mixin;

import com.mikufan.meks.Config;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.item.gear.ItemSpecialArmor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemSpecialArmor.class)
public abstract class ItemSpecialArmorMixin {

    @Inject(method = "isEnchantable(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$allowMekaSuitEnchanting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Config.MEKA_SUIT_ENCHANTMENT.get() && (Object) this instanceof ItemMekaSuitArmor) {
            cir.setReturnValue(true);
        }
    }
}
