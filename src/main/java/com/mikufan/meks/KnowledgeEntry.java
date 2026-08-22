package com.mikufan.meks;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import mekanism.common.inventory.ISlotClickHandler.IScrollableSlot;
import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One entry of the scrollable knowledge library shown by
 * {@link mekanism.client.gui.element.scroll.GuiScrollBar} based GUIs.
 * Shared by the block Exchange Switch and the Portable Exchange Switch.
 */
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