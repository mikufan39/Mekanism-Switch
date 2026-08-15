package com.mikufan.meks;

import java.util.function.IntFunction;

public enum ExchangeOperation {
    NONE,
    UPLOAD,
    DOWNLOAD,
    FORGET;

    public static final IntFunction<ExchangeOperation> BY_ID = id -> values()[id];
}
