package dev.itemnamecopy.test.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ClientTestController {
    private final SyntheticInput input;
    private final MinecraftClientDriver client;
    private final List<TestResult> results = new ArrayList<TestResult>();
    private List<TestCase> tests = new ArrayList<TestCase>();
    private String originalClipboard;
    private String worldName;
    private String bootScreen;
    private int stage;
    private int delay;
    private int cycleCount;
    private int testIndex;
    private int stepIndex;
    private long deadline;
    private boolean initialized;
    private boolean busy;
    private volatile boolean finished;

    ClientTestController(SyntheticInput input) {
        this.input = input;
        client = new MinecraftClientDriver(input);
    }

    void tick(Object minecraft) {
        if (finished || busy) return;
        busy = true;
        try {
            if (!initialized) initialize(minecraft);
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out at stage " + stage + ": " + client.describeScreen());
            }
            if (delay-- > 0) return;
            delay = 4;
            if (stage < 20) prepareWorld();
            else runNextStep();
        } catch (Pending ignored) {
        } catch (Throwable error) {
            failure("setup", error);
            finish();
        } finally {
            busy = false;
        }
    }

    private void initialize(Object minecraft) {
        initialized = true;
        client.initialize(minecraft);
        deadline = System.currentTimeMillis() + 180_000L;
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(240_000L);
            } catch (InterruptedException ignored) {
                return;
            }
            if (!finished) {
                System.err.println("CLIENT_TEST TIMEOUT " + RuntimeConfig.TARGET);
                Runtime.getRuntime().halt(124);
            }
        }, "itemnamecopy-test-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        RuntimeConfig.log("starting");
    }

    private void prepareWorld() {
        Object screen = client.screen();
        String name = screen == null ? "" : screen.getClass().getSimpleName();
        if (stage == 0) prepareMainMenu(name);
        else if (stage == 1) {
            client.waitForReload();
            client.press(client.findButton("Singleplayer", false));
            nextStage();
        } else if (stage == 2) {
            if (name.contains("CreateWorld")) {
                nextStage();
                return;
            }
            client.press(client.findButton("Create New World", false));
            nextStage();
        } else if (stage == 3) nameWorld(name, screen);
        else if (stage == 4) selectCreativeMode();
        else if (stage == 5) openWorldOptions();
        else if (stage == 6) selectSuperflat();
        else if (stage == 7) configureWorld(screen);
        else if (stage == 8) {
            client.press(client.findButton("Create New World", false));
            nextStage();
        } else if (stage == 9) enterWorld(name);
        else if (stage == 10) {
            client.gameRuleCommand("doMobSpawning", "spawn_mobs");
            nextStage();
        } else if (stage == 11) {
            client.gameRuleCommand("doDaylightCycle", "advance_time");
            nextStage();
        } else if (stage == 12) {
            client.gameRuleCommand("doWeatherCycle", "advance_weather");
            nextStage();
        } else if (stage == 13) {
            client.command("gamemode survival");
            nextStage();
        } else if (stage == 14) switchToJapanese();
        else if (stage == 15) startTests();
    }

    private void prepareMainMenu(String screenName) {
        if (!screenName.equals(bootScreen)) {
            bootScreen = screenName;
            RuntimeConfig.log("startup-screen " + client.describeScreen());
        }
        if (screenName.contains("AccessibilityOnboarding")) {
            client.press(client.findButton("Continue", false));
            return;
        }
        if (!screenName.equals("TitleScreen") && !screenName.equals("MainMenuScreen")
            && !screenName.equals("GuiMainMenu")) throw new Pending();
        Object minecraft = client.minecraft();
        Object overlay = Reflect.has(minecraft, "getOverlay", 0)
            ? Reflect.call(minecraft, "getOverlay") : Reflect.optionalGet(minecraft, "overlay", "loadingGui");
        if (overlay != null) throw new Pending();
        originalClipboard = client.clipboard();
        Object options = Reflect.get(minecraft, "options", "gameSettings");
        Reflect.set(options, false, "pauseOnLostFocus");
        client.language("en_us");
        nextStage();
    }

    private void nameWorld(String screenName, Object screen) {
        if (!screenName.contains("CreateWorld")) throw new Pending();
        worldName = "ItemNameCopy Test " + UUID.randomUUID().toString().substring(0, 8);
        client.setText(client.firstInput(screen), worldName);
        nextStage();
    }

    private void selectCreativeMode() {
        Object mode = client.findButton("Game Mode", true);
        String label = client.label(mode);
        if (label.toLowerCase(java.util.Locale.ROOT).contains("creative")) {
            cycleCount = 0;
            nextStage();
        } else {
            if (++cycleCount > 4) throw new AssertionError("Could not select Creative: " + label);
            client.press(mode);
        }
    }

    private void openWorldOptions() {
        Object worldTab = client.optionalButton("World", false);
        if (worldTab == null) worldTab = client.optionalButton("More World Options", true);
        if (worldTab != null) client.press(worldTab);
        nextStage();
    }

    private void selectSuperflat() {
        Object type = client.findButton("World Type", true);
        String label = client.label(type);
        if (label.toLowerCase(java.util.Locale.ROOT).contains("superflat")) {
            cycleCount = 0;
            nextStage();
        } else {
            if (++cycleCount > 12) throw new AssertionError("Could not select Superflat: " + label);
            client.press(type);
        }
    }

    private void configureWorld(Object screen) {
        client.setText(client.firstInput(screen), "1");
        Object structures = client.optionalButton("Generate Structures", true);
        if (structures == null) structures = client.optionalButton("Map Features", true);
        if (structures != null && !client.label(structures).toLowerCase(java.util.Locale.ROOT).contains("off")) {
            client.press(structures);
        }
        Object done = client.optionalButton("Done", false);
        if (done != null) client.press(done);
        nextStage();
    }

    private void enterWorld(String screenName) {
        if (screenName.equals("ConfirmScreen")) {
            client.press(client.findButton("Yes", false));
            return;
        }
        if (client.player() == null || client.screen() != null) throw new Pending();
        TestAssertions.require(Reflect.call(client.minecraft(),
            "getSingleplayerServer|getIntegratedServer|integratedServer") != null,
            "Expected a local integrated server");
        client.command("difficulty peaceful");
        nextStage();
    }

    private void switchToJapanese() {
        if (client.creative()) throw new Pending();
        client.language("ja_jp");
        nextStage();
    }

    private void startTests() {
        client.waitForReload();
        tests = new ClientTestSuite(client).define();
        stage = 20;
        deadline = System.currentTimeMillis() + 120_000L;
        RuntimeConfig.log("world-ready " + worldName);
    }

    private void runNextStep() {
        if (testIndex == tests.size()) {
            finish();
            return;
        }
        TestCase test = tests.get(testIndex);
        try {
            test.steps[stepIndex].run();
            if (++stepIndex == test.steps.length) {
                results.add(new TestResult(test.name, null));
                RuntimeConfig.log("PASS " + test.name);
                testIndex++;
                stepIndex = 0;
            }
        } catch (Pending ignored) {
        } catch (Throwable error) {
            failure(test.name, error);
            input.endKeyEvent();
            testIndex++;
            stepIndex = 0;
        }
    }

    private void nextStage() {
        stage++;
        RuntimeConfig.log("stage " + stage);
    }

    private void failure(String name, Throwable error) {
        results.add(new TestResult(name, error.toString()));
        RuntimeConfig.log("FAIL " + name + ": " + error);
        error.printStackTrace();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        input.reset();
        try {
            if (originalClipboard != null) client.restoreClipboard(originalClipboard);
        } catch (Throwable error) {
            failure("clipboard-restore", error);
        }
        try {
            TestReportWriter.write(results, tests.size());
        } catch (Throwable error) {
            error.printStackTrace();
        }
        try {
            client.stop();
        } catch (Throwable error) {
            error.printStackTrace();
            Runtime.getRuntime().halt(2);
        }
    }
}
