package dev.itemnamecopy.test.support;

import dev.itemnamecopy.test.runtime.ClientTestLifecycle;
import dev.itemnamecopy.test.runtime.ClientTestOptions;
import dev.itemnamecopy.test.runtime.Pending;
import dev.itemnamecopy.test.runtime.Reflect;
import dev.itemnamecopy.test.runtime.TestAssertions;

import java.util.UUID;

public final class ItemNameCopyTestLifecycle implements ClientTestLifecycle {
    private final ItemNameCopyClientDriver client;
    private final ClientTestOptions options;
    private String originalClipboard;
    private String worldName;
    private String bootScreen;
    private int stage;
    private int cycleCount;

    public ItemNameCopyTestLifecycle(ItemNameCopyClientDriver client, ClientTestOptions options) {
        this.client = client;
        this.options = options;
    }

    @Override
    public void initialize(Object minecraft) {
        client.initialize(minecraft);
    }

    @Override
    public boolean prepare() {
        Object screen = client.screen();
        String screenName = screen == null ? "" : screen.getClass().getSimpleName();
        if (stage == 0) prepareMainMenu(screenName);
        else if (stage == 1) openWorldSelection();
        else if (stage == 2) openWorldCreation(screenName);
        else if (stage == 3) nameWorld(screenName, screen);
        else if (stage == 4) selectCreativeMode();
        else if (stage == 5) openWorldOptions();
        else if (stage == 6) selectSuperflat();
        else if (stage == 7) configureWorld(screen);
        else if (stage == 8) createWorld();
        else if (stage == 9) enterWorld(screenName);
        else if (stage == 10) setGameRule("doMobSpawning", "spawn_mobs");
        else if (stage == 11) setGameRule("doDaylightCycle", "advance_time");
        else if (stage == 12) setGameRule("doWeatherCycle", "advance_weather");
        else if (stage == 13) switchToSurvival();
        else if (stage == 14) switchToJapanese();
        else if (stage == 15) return finishPreparation();
        return false;
    }

    @Override
    public String describeState() {
        return "preparation stage " + stage + ": " + client.describeScreen();
    }

    @Override
    public void cleanup() {
        if (originalClipboard != null) client.restoreClipboard(originalClipboard);
    }

    @Override
    public void shutdown() {
        client.stop();
    }

    private void prepareMainMenu(String screenName) {
        if (!screenName.equals(bootScreen)) {
            bootScreen = screenName;
            options.log("startup-screen " + client.describeScreen());
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
        Reflect.set(Reflect.get(minecraft, "options", "gameSettings"), false, "pauseOnLostFocus");
        client.language("en_us");
        nextStage();
    }

    private void openWorldSelection() {
        client.waitForReload();
        client.press(client.findButton("Singleplayer", false));
        nextStage();
    }

    private void openWorldCreation(String screenName) {
        if (screenName.contains("CreateWorld")) {
            nextStage();
            return;
        }
        client.press(client.findButton("Create New World", false));
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
            return;
        }
        if (++cycleCount > 4) throw new AssertionError("Could not select Creative: " + label);
        client.press(mode);
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
            return;
        }
        if (++cycleCount > 12) throw new AssertionError("Could not select Superflat: " + label);
        client.press(type);
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

    private void createWorld() {
        client.press(client.findButton("Create New World", false));
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

    private void setGameRule(String legacyName, String modernName) {
        client.gameRuleCommand(legacyName, modernName);
        nextStage();
    }

    private void switchToSurvival() {
        client.command("gamemode survival");
        nextStage();
    }

    private void switchToJapanese() {
        if (client.creative()) throw new Pending();
        client.language("ja_jp");
        nextStage();
    }

    private boolean finishPreparation() {
        client.waitForReload();
        options.log("world-ready " + worldName);
        return true;
    }

    private void nextStage() {
        stage++;
        options.log("stage " + stage);
    }
}
