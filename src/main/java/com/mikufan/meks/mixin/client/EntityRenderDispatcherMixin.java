package com.mikufan.meks.mixin.client;

import com.mikufan.meks.Config;
import com.mikufan.meks.soul.SoulCamera;
import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, remap = false)
    private <E extends Entity> void meks$soulShouldRender(E entity, Frustum frustum, double camX, double camY,
                                                          double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof SoulCamera) {
            cir.setReturnValue(false);
        } else if (entity == Minecraft.getInstance().player && SoulOutController.isActive() && !Config.SOUL_SHOW_BODY.get()) {
            cir.setReturnValue(false);
        }
    }
}
