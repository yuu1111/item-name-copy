package dev.itemnamecopy.test.runtime;

import java.util.Objects;

interface TestStep {
    void run();
}

final class Pending extends RuntimeException {
    private static final long serialVersionUID = 1L;
}

final class TestCase {
    final String name;
    final TestStep[] steps;

    TestCase(String name, TestStep[] steps) {
        this.name = name;
        this.steps = steps;
    }
}

final class TestResult {
    final String name;
    final String failure;

    TestResult(String name, String failure) {
        this.name = name;
        this.failure = failure;
    }
}

final class TestAssertions {
    private TestAssertions() {
    }

    static void equal(Object expected, Object actual) {
        require(Objects.equals(expected, actual), "Expected <" + expected + "> but was <" + actual + ">");
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
