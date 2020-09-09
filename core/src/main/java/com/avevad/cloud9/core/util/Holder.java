package com.avevad.cloud9.core.util;

public final class Holder<T> {
    public T value;

    public Holder(T value) {
        this.value = value;
    }

    public Holder() {
        this(null);
    }
}
