package com.mikufan.meks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mekanism.common.inventory.ISlotClickHandler.IScrollableSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ExchangeSwitchContainer extends MekanismTileContainer<ExchangeSwitchTile> {

    private final List<KnowledgeEntry> knowledgeList = new ArrayList<>();
    private long sv;

    public ExchangeSwitchContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv, ExchangeSwitchTile tile) {
        super(type, id, inv, tile);
    }

    @Override
    protected int getInventoryYOffset() {
        return 148;
    }

    public List<KnowledgeEntry> getKnowledgeList() {
        return knowledgeList;
    }

    public long getSv() {
        return sv;
    }

    public void receiveSync(List<ResourceLocation> knowledge, long sv) {
        this.sv = sv;
        knowledgeList.clear();
        for (ResourceLocation key : knowledge) {
            knowledgeList.add(new KnowledgeEntry(key));
        }
    }

    public void startExchange(ResourceLocation key, int count, boolean forget, int slot) {
        MeksPayloads.sendStartExchange(tile.getBlockPos(), key, count, forget, slot);
    }

    public void requestSync() {
        MeksPayloads.sendRequestSync(tile.getBlockPos());
    }

    public void cancelExchange(int slot) {
        MeksPayloads.sendCancelExchange(tile.getBlockPos(), slot);
    }

    public record KnowledgeEntry(ResourceLocation key) implements IScrollableSlot {

        @Override
        public HashedItem item() {
            return HashedItem.create(getInternalStack());
        }

        @Override
        public UUID itemUUID() {
            return UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public long count() {
            return 1;
        }

        @Override
        public ItemStack getInternalStack() {
            return new ItemStack(BuiltInRegistries.ITEM.get(key));
        }
    }
}
