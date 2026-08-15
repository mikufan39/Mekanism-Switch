package com.mikufan.meks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class ChannelUpgradeItem extends Item {

    public ChannelUpgradeItem() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof ExchangeSwitchTile tile)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        if (tile.tryInstallChannelUpgrade(player)) {
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("gui.meks.channel_upgrade.installed"), true);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("gui.meks.channel_upgrade.already"), true);
        return InteractionResult.FAIL;
    }
}
