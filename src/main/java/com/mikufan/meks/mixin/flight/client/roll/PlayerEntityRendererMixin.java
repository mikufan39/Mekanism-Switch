package com.mikufan.meks.mixin.flight.client.roll;

import com.llamalad7.mixinextras.sugar.Local;
import com.mikufan.meks.flight.api.RollEntity;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @ModifyArg(
            method = "setupRotations",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 1,
                    remap = false
            ),
            index = 0,
            remap = false
    )
    private Quaternionf meksFlight$modifyRoll(Quaternionf original,
                                              @Local(argsOnly = true) AbstractClientPlayer player,
                                              @Local(argsOnly = true, ordinal = 2) float tickDelta) {
        var rollEntity = (RollEntity) player;

        if (rollEntity.meksFlight$isRolling()) {
            var roll = rollEntity.meksFlight$getRoll(tickDelta);
            return Axis.YP.rotationDegrees(roll);
        }

        return original;
    }
}
