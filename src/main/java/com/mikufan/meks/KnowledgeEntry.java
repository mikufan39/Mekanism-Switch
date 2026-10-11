package com.mikufan.meks;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    // Cached per key. The cached ItemStack instances are read-only shared objects:
    // callers must never modify them (rendering and existing .copy() usages already comply).
    private static final Map<ResourceLocation, ItemStack> STACK_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, UUID> UUID_CACHE = new ConcurrentHashMap<>();

    @Override
    public HashedItem item() {
        return HashedItem.create(getInternalStack());
    }

    @Override
    public UUID itemUUID() {
        return UUID_CACHE.computeIfAbsent(key, k -> UUID.nameUUIDFromBytes(k.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public long count() {
        return 1;
    }

    @Override
    public ItemStack getInternalStack() {
        return STACK_CACHE.computeIfAbsent(key, k -> new ItemStack(BuiltInRegistries.ITEM.get(k)));
    }
}
