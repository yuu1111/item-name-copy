package dev.itemnamecopy.test.runtime;

public interface ClientTestLifecycle {
    void initialize(Object client);

    boolean prepare();

    String describeState();

    void cleanup();

    void shutdown();
}
