package com.mikufan.meks;

import mekanism.api.Upgrade;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.content.blocktype.Machine.MachineBuilder;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MeksRegistries {

    private MeksRegistries() {
    }

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MekanismSwitch.MODID);
    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(MekanismSwitch.MODID);
    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MekanismSwitch.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekanismSwitch.MODID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MekanismSwitch.MODID);
    public static final DeferredHolder<Item, ChannelUpgradeItem> CHANNEL_UPGRADE =
            ITEMS.register("channel_upgrade", ChannelUpgradeItem::new);
    public static final DeferredHolder<Item, PortableExchangeSwitchItem> PORTABLE_EXCHANGE_SWITCH =
            ITEMS.register("portable_exchange_switch", PortableExchangeSwitchItem::new);

    public static final BlockRegistryObject<BlockExchangeSwitch, BlockItem> EXCHANGE_SWITCH_BLOCK =
            BLOCKS.register("exchange_switch", () -> new BlockExchangeSwitch(getExchangeSwitchType()),
                  (block, properties) -> new BlockItem(block, properties
                        .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                        .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)));

    public static final TileEntityTypeRegistryObject<ExchangeSwitchTile> EXCHANGE_SWITCH_TILE =
            TILE_ENTITY_TYPES.mekBuilder(EXCHANGE_SWITCH_BLOCK, ExchangeSwitchTile::new)
                  .clientTicker(TileEntityMekanism::tickClient)
                  .serverTicker(TileEntityMekanism::tickServer)
                  .withSimple(Capabilities.CONFIG_CARD)
                  .build();

    public static final ContainerTypeRegistryObject<ExchangeSwitchContainer> EXCHANGE_SWITCH_CONTAINER = registerExchangeSwitchContainer();

    public static final BlockRegistryObject<BlockRestorationSwitch, BlockItem> RESTORATION_SWITCH_BLOCK =
            BLOCKS.register("restoration_switch", () -> new BlockRestorationSwitch(getRestorationSwitchType()),
                  (block, properties) -> new BlockItem(block, properties
                        .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                        .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)));

    public static final TileEntityTypeRegistryObject<RestorationSwitchTile> RESTORATION_SWITCH_TILE =
            TILE_ENTITY_TYPES.mekBuilder(RESTORATION_SWITCH_BLOCK, RestorationSwitchTile::new)
                  .clientTicker(TileEntityMekanism::tickClient)
                  .serverTicker(TileEntityMekanism::tickServer)
                  .withSimple(Capabilities.CONFIG_CARD)
                  .build();

    public static final ContainerTypeRegistryObject<RestorationSwitchContainer> RESTORATION_SWITCH_CONTAINER = registerRestorationSwitchContainer();

    public static final ContainerTypeRegistryObject<PortableExchangeSwitchContainer> PORTABLE_EXCHANGE_SWITCH_CONTAINER = registerPortableContainer();

    private static Machine<ExchangeSwitchTile> exchangeSwitchType;

    private static Machine<ExchangeSwitchTile> getExchangeSwitchType() {
        if (exchangeSwitchType == null) {
            exchangeSwitchType = MachineBuilder
                    .createMachine(() -> EXCHANGE_SWITCH_TILE, new MeksLang("block.meks.exchange_switch"))
                    .withGui(() -> EXCHANGE_SWITCH_CONTAINER)
                    .withEnergyConfig(() -> 100L, () -> 1_000_000L)
                    .with(AttributeSideConfig.ELECTRIC_MACHINE)
                    .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                    .build();
        }
        return exchangeSwitchType;
    }

    private static ContainerTypeRegistryObject<ExchangeSwitchContainer> registerExchangeSwitchContainer() {
        ContainerTypeRegistryObject<ExchangeSwitchContainer> registryObject =
                new ContainerTypeRegistryObject<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "exchange_switch"));
        CONTAINER_TYPES.registerMenu("exchange_switch", () -> MekanismContainerType.tile(
              ExchangeSwitchTile.class, (id, inv, tile) -> new ExchangeSwitchContainer(registryObject, id, inv, tile)));
        return registryObject;
    }

    private static Machine<RestorationSwitchTile> restorationSwitchType;

    private static Machine<RestorationSwitchTile> getRestorationSwitchType() {
        if (restorationSwitchType == null) {
            restorationSwitchType = MachineBuilder
                    .createMachine(() -> RESTORATION_SWITCH_TILE, new MeksLang("block.meks.restoration_switch"))
                    .withGui(() -> RESTORATION_SWITCH_CONTAINER)
                    .withEnergyConfig(() -> 100L, () -> 1_000_000L)
                    .with(AttributeSideConfig.ELECTRIC_MACHINE)
                    .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                    .build();
        }
        return restorationSwitchType;
    }

    private static ContainerTypeRegistryObject<RestorationSwitchContainer> registerRestorationSwitchContainer() {
        ContainerTypeRegistryObject<RestorationSwitchContainer> registryObject =
                new ContainerTypeRegistryObject<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "restoration_switch"));
        CONTAINER_TYPES.registerMenu("restoration_switch", () -> MekanismContainerType.tile(
              RestorationSwitchTile.class, (id, inv, tile) -> new RestorationSwitchContainer(registryObject, id, inv, tile)));
        return registryObject;
    }

    private static ContainerTypeRegistryObject<PortableExchangeSwitchContainer> registerPortableContainer() {
        ContainerTypeRegistryObject<PortableExchangeSwitchContainer> registryObject =
                new ContainerTypeRegistryObject<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "portable_exchange_switch"));
        CONTAINER_TYPES.register("portable_exchange_switch",
              (id, inv) -> new PortableExchangeSwitchContainer(registryObject, id, inv));
        return registryObject;
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.meks.main"))
            .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .icon(() -> EXCHANGE_SWITCH_BLOCK.asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXCHANGE_SWITCH_BLOCK.asItem());
                output.accept(CHANNEL_UPGRADE.get());
                output.accept(RESTORATION_SWITCH_BLOCK.asItem());
                output.accept(PORTABLE_EXCHANGE_SWITCH.get());
            })
            .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILE_ENTITY_TYPES.register(bus);
        CONTAINER_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        MeksAttachments.ATTACHMENT_TYPES.register(bus);
    }
}
