package com.mikufan.meks.mixin;

import com.mikufan.meks.Config;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Redirect(method = "onEnchantmentPerformed", remap = false,
          at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    private void meks$chargeMekaSuitEnchantCost(Player player, int levels, ItemStack enchantedItem, int levelCost) {
        if (Config.MEKA_SUIT_ENCHANTMENT.get() && enchantedItem.getItem() instanceof ItemMekaSuitArmor) {
            player.giveExperienceLevels(-Config.MEKA_SUIT_ENCHANT_COST.get());
        } else {
            player.giveExperienceLevels(levels);
        }
    }
}
