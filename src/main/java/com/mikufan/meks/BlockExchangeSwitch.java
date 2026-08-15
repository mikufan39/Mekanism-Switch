package com.mikufan.meks;

import java.util.function.UnaryOperator;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockExchangeSwitch extends BlockTile<ExchangeSwitchTile, Machine<ExchangeSwitchTile>> {

    public BlockExchangeSwitch(Machine<ExchangeSwitchTile> type) {
        super(type, UnaryOperator.identity());
    }
}
