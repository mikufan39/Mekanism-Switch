package com.mikufan.meks;

import com.mojang.brigadier.Command;
import java.util.ArrayList;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MeksCommands {

    private MeksCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mek")
              .then(Commands.literal("dev")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> runDev(context.getSource()))));
    }

    private static int runDev(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.meks.dev.requires_player"));
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        data.setSv(9_999_999_999L);
        data.getKnowledge().addAll(MeksValues.getMappedItems());
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        PacketDistributor.sendToPlayer(player, new MeksPayloads.SyncExchangePayload(
              new ArrayList<>(data.getKnowledge()), data.getSv()));
        source.sendSuccess(() -> Component.translatable("commands.meks.dev.success"), false);
        return Command.SINGLE_SUCCESS;
    }
}
