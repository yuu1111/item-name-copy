package com.github.yuu1111.minecraft.clienttest;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ClientTestOptions {
    private static final long DEFAULT_PREPARATION_TIMEOUT = 180_000L;
    private static final long DEFAULT_TEST_TIMEOUT = 120_000L;
    private static final long DEFAULT_WATCHDOG_TIMEOUT = 240_000L;

    private final String target;
    private final String source;
    private final Path report;
    private final String mode;

    private ClientTestOptions(String target, String source, Path report, String mode) {
        this.target = target;
        this.source = source;
        this.report = report;
        this.mode = mode;
    }

    public static ClientTestOptions fromSystemProperties(String prefix, String mode) {
        String target = System.getProperty(prefix + ".target", "unknown");
        String source = System.getProperty(prefix + ".source", "unknown");
        Path report = Paths.get(System.getProperty(prefix + ".report", "client-test-results.json"));
        return new ClientTestOptions(target, source, report, mode);
    }

    public String target() {
        return target;
    }

    public String source() {
        return source;
    }

    public Path report() {
        return report;
    }

    public String mode() {
        return mode;
    }

    long preparationTimeoutMillis() {
        return DEFAULT_PREPARATION_TIMEOUT;
    }

    long testTimeoutMillis() {
        return DEFAULT_TEST_TIMEOUT;
    }

    long watchdogTimeoutMillis() {
        return DEFAULT_WATCHDOG_TIMEOUT;
    }

    public void log(String value) {
        System.out.println("CLIENT_TEST " + target + " " + value);
    }
}
