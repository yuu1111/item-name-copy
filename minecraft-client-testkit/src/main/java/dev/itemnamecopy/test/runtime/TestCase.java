package dev.itemnamecopy.test.runtime;

public final class TestCase {
    private final String name;
    private final TestStep[] steps;

    public TestCase(String name, TestStep... steps) {
        this.name = name;
        this.steps = steps;
    }

    public String name() {
        return name;
    }

    TestStep[] steps() {
        return steps;
    }
}
