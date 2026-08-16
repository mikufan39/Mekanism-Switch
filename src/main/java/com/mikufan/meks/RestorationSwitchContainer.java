package com.mikufan.meks;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class RestorationSwitchContainer extends MekanismTileContainer<RestorationSwitchTile> {

    private long sv;

    public RestorationSwitchContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv, RestorationSwitchTile tile) {
        super(type, id, inv, tile);
    }

    public Player getPlayer() {
        return inv.player;
    }

    public long getSv() {
        return sv;
    }

    public void setSv(long value) {
        sv = value;
    }

    @Override
    protected int getInventoryYOffset() {
        return 124;
    }
}
