package com.mikufan.meks;

import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class CreeperExplosionHandler {

    private CreeperExplosionHandler() {
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!Config.CREEPER_NO_BLOCK_DAMAGE.get()) {
            return;
        }
        if (event.getExplosion().getDirectSourceEntity() instanceof Creeper) {
            event.getAffectedBlocks().clear();
        }
    }
}
