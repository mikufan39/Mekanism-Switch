package com.mikufan.meks.flight.api.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.mikufan.meks.flight.impl.key.InputContextImpl;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

public interface InputContext {
    static InputContext of(ResourceLocation id, Supplier<Boolean> activeCondition) {
        return new InputContextImpl(id, activeCondition);
    }

    ResourceLocation getId();

    boolean isActive();

    void addKeyBinding(KeyMapping keyMapping);

    List<KeyMapping> getKeyBindings();

    KeyMapping getKeyBinding(InputConstants.Key key);

    void updateKeysByCode();
}