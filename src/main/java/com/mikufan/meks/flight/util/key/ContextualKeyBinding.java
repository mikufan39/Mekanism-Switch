package com.mikufan.meks.flight.util.key;

import com.mikufan.meks.flight.api.key.InputContext;

import java.util.List;

public interface ContextualKeyBinding {
    void meksFlight$addToContext(InputContext context);

    List<InputContext> meksFlight$getContexts();
}