package com.mikufan.meks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import mekanism.common.util.text.TextUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MeksCommands {

    private static final int MAX_LIST_ITEMS = 50;

    private MeksCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandBuildContext buildContext = event.getBuildContext();
        event.getDispatcher().register(Commands.literal("mek")
              .then(svCommands(buildContext))
              .then(knowledgeCommands(buildContext)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> svCommands(CommandBuildContext buildContext) {
        return Commands.literal("sv")
              .then(Commands.literal("get")
                    .executes(context -> svGet(context.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                          .requires(source -> source.hasPermission(2))
                          .executes(context -> svGet(context.getSource(), EntityArgument.getPlayer(context, "player")))))
              .then(Commands.literal("value")
                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                          .executes(context -> svValue(context.getSource(), ItemArgument.getItem(context, "item")))))
              .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", LongArgumentType.longArg(0L, Long.MAX_VALUE))
                          .executes(context -> svSet(context.getSource(), LongArgumentType.getLong(context, "amount"), null))
                          .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> svSet(context.getSource(), LongArgumentType.getLong(context, "amount"),
                                      EntityArgument.getPlayer(context, "player"))))))
              .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", LongArgumentType.longArg(Long.MIN_VALUE, Long.MAX_VALUE))
                          .executes(context -> svAdd(context.getSource(), LongArgumentType.getLong(context, "amount"), null))
                          .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> svAdd(context.getSource(), LongArgumentType.getLong(context, "amount"),
                                      EntityArgument.getPlayer(context, "player"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> knowledgeCommands(CommandBuildContext buildContext) {
        return Commands.literal("knowledge")
              .then(Commands.literal("list")
                    .executes(context -> knowledgeList(context.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                          .requires(source -> source.hasPermission(2))
                          .executes(context -> knowledgeList(context.getSource(), EntityArgument.getPlayer(context, "player")))))
              .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                          .executes(context -> knowledgeAdd(context.getSource(), ItemArgument.getItem(context, "item"), null))
                          .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> knowledgeAdd(context.getSource(), ItemArgument.getItem(context, "item"),
                                      EntityArgument.getPlayer(context, "player"))))))
              .then(Commands.literal("remove")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                          .executes(context -> knowledgeRemove(context.getSource(), ItemArgument.getItem(context, "item"), null))
                          .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> knowledgeRemove(context.getSource(), ItemArgument.getItem(context, "item"),
                                      EntityArgument.getPlayer(context, "player"))))))
              .then(Commands.literal("clear")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> knowledgeClear(context.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                          .executes(context -> knowledgeClear(context.getSource(), EntityArgument.getPlayer(context, "player")))))
              .then(Commands.literal("unlock-all")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> knowledgeUnlockAll(context.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                          .executes(context -> knowledgeUnlockAll(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }

    private static int svGet(CommandSourceStack source, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        source.sendSuccess(() -> Component.translatable("commands.meks.sv.get", player.getDisplayName(), TextUtils.format(data.getSv())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int svValue(CommandSourceStack source, ItemInput input) {
        Item item = input.getItem();
        long value = MeksValues.getValue(item);
        ItemStack stack = new ItemStack(item);
        source.sendSuccess(() -> Component.translatable("commands.meks.sv.value", stack.getHoverName(), TextUtils.format(value)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int svSet(CommandSourceStack source, long amount, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        data.setSv(amount);
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        source.sendSuccess(() -> Component.translatable("commands.meks.sv.set.success", player.getDisplayName(), TextUtils.format(amount)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int svAdd(CommandSourceStack source, long amount, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        long result;
        try {
            result = Math.addExact(data.getSv(), amount);
        } catch (ArithmeticException e) {
            source.sendFailure(Component.translatable("commands.meks.sv.add.overflow"));
            return 0;
        }
        if (result < 0) {
            source.sendFailure(Component.translatable("commands.meks.sv.add.negative", TextUtils.format(data.getSv())));
            return 0;
        }
        data.setSv(result);
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        source.sendSuccess(() -> Component.translatable("commands.meks.sv.add.success", player.getDisplayName(), TextUtils.format(result)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int knowledgeList(CommandSourceStack source, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        source.sendSuccess(() -> buildKnowledgeList(player, data), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int knowledgeAdd(CommandSourceStack source, ItemInput input, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        Item item = input.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        ItemStack stack = new ItemStack(item);
        if (!MeksValues.hasValue(item)) {
            source.sendFailure(Component.translatable("commands.meks.knowledge.add.no_value", stack.getHoverName()));
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        boolean added = data.learn(key);
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        Component message = added
              ? Component.translatable("commands.meks.knowledge.add.success", player.getDisplayName(), stack.getHoverName())
              : Component.translatable("commands.meks.knowledge.add.already", player.getDisplayName(), stack.getHoverName());
        source.sendSuccess(() -> message, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int knowledgeRemove(CommandSourceStack source, ItemInput input, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(input.getItem());
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(key));
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        if (!data.hasKnowledge(key)) {
            source.sendFailure(Component.translatable("commands.meks.knowledge.remove.missing", player.getDisplayName(), stack.getHoverName()));
            return 0;
        }
        data.forget(key);
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        source.sendSuccess(() -> Component.translatable("commands.meks.knowledge.remove.success", player.getDisplayName(), stack.getHoverName()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int knowledgeClear(CommandSourceStack source, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        data.getKnowledge().clear();
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        source.sendSuccess(() -> Component.translatable("commands.meks.knowledge.clear.success", player.getDisplayName()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int knowledgeUnlockAll(CommandSourceStack source, ServerPlayer explicitTarget) {
        ServerPlayer player = resolvePlayer(source, explicitTarget);
        if (player == null) {
            return 0;
        }
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        data.getKnowledge().addAll(MeksValues.getMappedItems());
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
        syncToPlayer(player);
        source.sendSuccess(() -> Component.translatable("commands.meks.knowledge.unlock_all.success", player.getDisplayName()), false);
        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("SameParameterValue")
    private static ServerPlayer resolvePlayer(CommandSourceStack source, ServerPlayer explicitTarget) {
        if (explicitTarget != null) {
            return explicitTarget;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        source.sendFailure(Component.translatable("commands.meks.requires_player"));
        return null;
    }

    private static Component buildKnowledgeList(ServerPlayer player, PlayerExchangeData data) {
        Set<ResourceLocation> knowledge = data.getKnowledge();
        if (knowledge.isEmpty()) {
            return Component.translatable("commands.meks.knowledge.list.empty", player.getDisplayName());
        }
        int total = knowledge.size();
        List<Component> shown = knowledge.stream()
              .sorted(Comparator.naturalOrder())
              .limit(MAX_LIST_ITEMS)
              .map(MeksCommands::itemName)
              .toList();
        String names = shown.stream().map(Component::getString).collect(Collectors.joining(", "));
        Component message = Component.translatable("commands.meks.knowledge.list", player.getDisplayName(), total, names);
        if (total > MAX_LIST_ITEMS) {
            message = message.copy().append(Component.translatable("commands.meks.knowledge.list.more", total - MAX_LIST_ITEMS));
        }
        return message;
    }

    private static Component itemName(ResourceLocation key) {
        return new ItemStack(BuiltInRegistries.ITEM.get(key)).getHoverName();
    }

    private static void syncToPlayer(ServerPlayer player) {
        PlayerExchangeData data = player.getData(MeksAttachments.EXCHANGE_DATA);
        PacketDistributor.sendToPlayer(player,
              new MeksPayloads.SyncExchangePayload(new ArrayList<>(data.getKnowledge()), data.getSv()));
    }
}
