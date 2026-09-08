package dev.itemnamecopy.test.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientTestRunner {
    private final SyntheticInput input;
    private final ClientTestLifecycle lifecycle;
    private final TestSuite suite;
    private final ClientTestOptions options;
    private final List<TestResult> results = new ArrayList<TestResult>();
    private List<TestCase> tests = Collections.emptyList();
    private int delay;
    private int testIndex;
    private int stepIndex;
    private long deadline;
    private boolean initialized;
    private boolean ready;
    private boolean busy;
    private volatile boolean finished;

    public ClientTestRunner(
        SyntheticInput input,
        ClientTestLifecycle lifecycle,
        TestSuite suite,
        ClientTestOptions options
    ) {
        this.input = input;
        this.lifecycle = lifecycle;
        this.suite = suite;
        this.options = options;
    }

    public void tick(Object client) {
        if (finished || busy) return;
        busy = true;
        try {
            if (!initialized) initialize(client);
            enforceDeadline();
            if (delay-- > 0) return;
            delay = 4;
            if (!ready) prepare();
            else runNextStep();
        } catch (Pending ignored) {
        } catch (Throwable error) {
            failure("setup", error);
            finish();
        } finally {
            busy = false;
        }
    }

    private void initialize(Object client) {
        initialized = true;
        lifecycle.initialize(client);
        deadline = System.currentTimeMillis() + options.preparationTimeoutMillis();
        startWatchdog();
        options.log("starting");
    }

    private void prepare() {
        if (!lifecycle.prepare()) return;
        tests = suite.defineTests();
        ready = true;
        deadline = System.currentTimeMillis() + options.testTimeoutMillis();
        options.log("ready");
    }

    private void runNextStep() {
        if (testIndex == tests.size()) {
            finish();
            return;
        }
        TestCase test = tests.get(testIndex);
        try {
            TestStep[] steps = test.steps();
            steps[stepIndex].run();
            if (++stepIndex == steps.length) pass(test);
        } catch (Pending ignored) {
        } catch (Throwable error) {
            failure(test.name(), error);
            input.endKeyEvent();
            advanceTest();
        }
    }

    private void pass(TestCase test) {
        results.add(new TestResult(test.name(), null));
        options.log("PASS " + test.name());
        advanceTest();
    }

    private void advanceTest() {
        testIndex++;
        stepIndex = 0;
    }

    private void enforceDeadline() {
        if (System.currentTimeMillis() > deadline) {
            throw new AssertionError("Timed out at " + lifecycle.describeState());
        }
    }

    private void startWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(options.watchdogTimeoutMillis());
            } catch (InterruptedException ignored) {
                return;
            }
            if (!finished) {
                System.err.println("CLIENT_TEST TIMEOUT " + options.target());
                Runtime.getRuntime().halt(124);
            }
        }, "client-test-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void failure(String name, Throwable error) {
        results.add(new TestResult(name, error.toString()));
        options.log("FAIL " + name + ": " + error);
        error.printStackTrace();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        input.reset();
        try {
            lifecycle.cleanup();
        } catch (Throwable error) {
            failure("cleanup", error);
        }
        try {
            TestReportWriter.write(options, results, tests.size(), suite.getClass().getName());
        } catch (Throwable error) {
            error.printStackTrace();
        }
        try {
            lifecycle.shutdown();
        } catch (Throwable error) {
            error.printStackTrace();
            Runtime.getRuntime().halt(2);
        }
    }
}
