package dev.itemnamecopy.test.runtime;

import java.util.Objects;

public final class TestAssertions {
    private TestAssertions() {
    }

    public static void equal(Object expected, Object actual) {
        require(Objects.equals(expected, actual), "Expected <" + expected + "> but was <" + actual + ">");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
