package com.mikufan.meks.mixin.flight.client.key;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mikufan.meks.flight.util.key.ContextualKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// DABR targeted yarn ControlsListWidget.KeyBindingEntry (method "update", KeyBinding.equals(KeyBinding));
// on NeoForge 21.1.x Minecraft that list was replaced by KeyBindsList with a KeyEntry inner class whose
// conflict check is KeyMapping.same(KeyMapping) inside refreshEntry(), so the targets are mapped to those.
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindingEntryMixin {
    @Shadow(remap = false)
    @Final
    private KeyMapping key;

    @ModifyExpressionValue(
            method = "refreshEntry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;same(Lnet/minecraft/client/KeyMapping;)Z"
            ),
            remap = false
    )
    private boolean meksFlight$ignoreCertainKeyBindingConflicts(boolean original, @Local KeyMapping otherBinding) {
        var firstContexts = ((ContextualKeyBinding) key).meksFlight$getContexts();
        var secondContexts = ((ContextualKeyBinding) otherBinding).meksFlight$getContexts();

        // none + none -> original
        // none + has -> false
        // has + none -> false
        // has + has ->
        //   same context -> original
        //   different context -> false

        if (firstContexts.isEmpty() && secondContexts.isEmpty()) return original;
        if (firstContexts.isEmpty() || secondContexts.isEmpty()) return false;
        if (firstContexts.stream().anyMatch(secondContexts::contains)) return original;
        return false;
    }
}