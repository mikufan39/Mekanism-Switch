package com.mikufan.meks;

import mekanism.api.text.ILangEntry;

public record MeksLang(String key) implements ILangEntry {

    @Override
    public String getTranslationKey() {
        return key;
    }
}
