package com.mikufan.meks;

import java.util.ArrayList;
import java.util.List;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Portable Exchange Switch. Unlike the block variant there are no
 * machine slots, no energy, no side configuration and no upgrades: the whole
 * library is accessed through the scrollable list and SV is kept on the
 * player's {@link PlayerExchangeData} attachment (server authoritative).
 */
public class PortableExchangeSwitchContainer extends MekanismContainer {

    private final List<KnowledgeEntry> knowledgeList = new ArrayList<>();
    private long sv;

    public PortableExchangeSwitchContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv) {
        super(type, id, inv);
        addSlotsAndOpen();
    }

    @Override
    protected int getInventoryYOffset() {
        return 136;
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

    /** Uploads 1 item (or the whole slot stack when shift-clicked) to the library. */
    public void startUpload(int slot, int count) {
        MeksPayloads.sendPortableUpload(slot, count);
    }

    /** Downloads a library entry: deducts SV and spawns the real item. */
    public void startDownload(ResourceLocation key, int count) {
        startDownload(key, count, false);
    }

    /**
     * Downloads a library entry; when {@code forget} is true the knowledge is
     * removed after the download succeeds (the server only forgets on success,
     * so a failed download never deletes knowledge).
     */
    public void startDownload(ResourceLocation key, int count, boolean forget) {
        MeksPayloads.sendPortableDownload(key, count, forget);
    }

    public void requestSync() {
        MeksPayloads.sendPortableRequestSync();
    }

    @Override
    public boolean canPlayerAccess(Player player) {
        return stillValid(player);
    }

    @Override
    public boolean stillValid(Player player) {
        // The portable switch must still exist somewhere in the player's inventory
        // for the GUI to stay open (mirrors other handheld-item containers).
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PortableExchangeSwitchItem) {
                return true;
            }
        }
        return false;
    }
}