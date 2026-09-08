package com.github.yuu1111.minecraft.clienttest;

public interface ClientTestLifecycle {
    void initialize(Object client);

    boolean prepare();

    String describeState();

    void cleanup();

    void shutdown();
}
