package com.mikufan.meks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Portable Exchange Switch — a handheld variant of the block Exchange Switch.
 * Right-clicking opens a simplified GUI where items are uploaded to the
 * library instantly (no power, no time) and library entries are downloaded by
 * simply deducting SV and generating the real item into the player's
 * inventory (no ghost items).
 */
public class PortableExchangeSwitchItem extends Item {

    public PortableExchangeSwitchItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.meks.portable_exchange_switch.desc"));
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                  (id, inv, p) -> new PortableExchangeSwitchContainer(MeksRegistries.PORTABLE_EXCHANGE_SWITCH_CONTAINER, id, inv),
                  Component.translatable("gui.meks.portable_exchange_switch.title", player.getDisplayName()));
            serverPlayer.openMenu(provider);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}