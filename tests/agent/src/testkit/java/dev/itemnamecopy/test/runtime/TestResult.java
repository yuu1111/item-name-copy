package dev.itemnamecopy.test.runtime;

final class TestResult {
    final String name;
    final String failure;

    TestResult(String name, String failure) {
        this.name = name;
        this.failure = failure;
    }
}
