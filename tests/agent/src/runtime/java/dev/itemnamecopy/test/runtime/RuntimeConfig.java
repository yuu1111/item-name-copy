package dev.itemnamecopy.test.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class RuntimeConfig {
    public static final String TARGET = System.getProperty("itemnamecopy.test.target", "unknown");
    public static final Path REPORT = Paths.get(System.getProperty("itemnamecopy.test.report", "client-test-results.json"));
    public static final boolean LEGACY = TARGET.startsWith("1.12.2-");

    private RuntimeConfig() {
    }

    public static void log(String value) {
        System.out.println("CLIENT_TEST " + TARGET + " " + value);
    }
}
