package com.mikufan.meks.mixin.flight.client.key;

import com.mikufan.meks.flight.api.key.InputContext;
import com.mikufan.meks.flight.util.key.ContextualKeyBinding;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

/**
 * Marks every {@link KeyMapping} as a {@link ContextualKeyBinding} so flight keys can be grouped
 * into the fall-flying {@link InputContext}.
 *
 * <p>Unlike the reference mod's Fabric build, no injection handlers are needed here: NeoForge
 * patches {@code KeyMapping} to route a key press through {@code KeyMappingLookup.getAll(...)},
 * which delivers the press to every mapping bound to that key. The reference mod gates those
 * handlers behind its Fabric build for the same reason.
 */
@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin implements ContextualKeyBinding {
    @Unique
    private final ArrayList<InputContext> contexts = new ArrayList<>();

    @Override
    public List<InputContext> meksFlight$getContexts() {
        return contexts;
    }

    @Override
    public void meksFlight$addToContext(InputContext context) {
        contexts.add(context);
    }
}
