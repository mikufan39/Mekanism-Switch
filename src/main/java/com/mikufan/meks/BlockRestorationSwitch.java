package com.mikufan.meks;

import java.util.function.UnaryOperator;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockRestorationSwitch extends BlockTile<RestorationSwitchTile, Machine<RestorationSwitchTile>> {

    public BlockRestorationSwitch(Machine<RestorationSwitchTile> type) {
        super(type, UnaryOperator.identity());
    }
}
