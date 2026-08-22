package com.mikufan.meks.mixin.flight.client.roll;

import com.mikufan.meks.flight.api.RollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {
    @Shadow(remap = false)
    @Final
    private Minecraft minecraft;

    @ModifyArg(
            method = "getGameInformation",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    ordinal = 7
            ),
            index = 1,
            require = 0,
            remap = false
    )
    private String meksFlight$modifyDebugHudText(String format) {
        var cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) return null;

        // Carefully insert a new number format specifier into the facing string
        var firstHalf = format.substring(0, format.length() - 1);
        var secondHalf = format.substring(format.length() - 1);
        return firstHalf + " / %.1f" + secondHalf;
    }

    @ModifyArg(
            method = "getGameInformation",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    ordinal = 7
            ),
            index = 2,
            require = 0,
            remap = false
    )
    private Object[] meksFlight$modifyDebugHudText2(Object[] args) {
        var cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) return args;

        // Add the roll value to the format arguments
        var roll = ((RollEntity) cameraEntity).meksFlight$getRoll();
        var newFmtArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, newFmtArgs, 0, args.length);
        newFmtArgs[args.length] = roll;
        return newFmtArgs;
    }
}
