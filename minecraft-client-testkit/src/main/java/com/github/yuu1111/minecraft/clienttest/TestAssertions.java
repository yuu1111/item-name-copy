package com.github.yuu1111.minecraft.clienttest;

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
