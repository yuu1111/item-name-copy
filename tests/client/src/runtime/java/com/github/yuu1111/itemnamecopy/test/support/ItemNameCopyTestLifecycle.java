package com.github.yuu1111.itemnamecopy.test.support;

import com.github.yuu1111.minecraft.clienttest.ClientTestLifecycle;
import com.github.yuu1111.minecraft.clienttest.ClientTestOptions;
import com.github.yuu1111.minecraft.clienttest.Pending;
import com.github.yuu1111.minecraft.clienttest.Reflect;
import com.github.yuu1111.minecraft.clienttest.TestAssertions;

import java.util.Locale;
import java.util.UUID;

public final class ItemNameCopyTestLifecycle implements ClientTestLifecycle {
    private final ItemNameCopyClientDriver client;
    private final ClientTestOptions options;
    private String originalClipboard;
    private String worldName;
    private String bootScreen;
    private PreparationStage stage = PreparationStage.WAIT_FOR_MAIN_MENU;
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
        switch (stage) {
            case WAIT_FOR_MAIN_MENU: prepareMainMenu(screenName); return false;
            case OPEN_WORLD_SELECTION: openWorldSelection(); return false;
            case OPEN_WORLD_CREATION: openWorldCreation(screenName); return false;
            case NAME_WORLD: nameWorld(screenName, screen); return false;
            case SELECT_CREATIVE_MODE: selectCreativeMode(); return false;
            case OPEN_WORLD_OPTIONS: openWorldOptions(); return false;
            case SELECT_SUPERFLAT: selectSuperflat(); return false;
            case CONFIGURE_WORLD: configureWorld(screen); return false;
            case CREATE_WORLD: createWorld(); return false;
            case ENTER_WORLD: enterWorld(screenName); return false;
            case DISABLE_MOB_SPAWNING: disableGameRule("doMobSpawning", "spawn_mobs"); return false;
            case DISABLE_DAYLIGHT_CYCLE: disableGameRule("doDaylightCycle", "advance_time"); return false;
            case DISABLE_WEATHER_CYCLE: disableGameRule("doWeatherCycle", "advance_weather"); return false;
            case SWITCH_TO_SURVIVAL: switchToSurvival(); return false;
            case SWITCH_TO_JAPANESE: switchToJapanese(); return false;
            case FINISH: return finishPreparation();
            default: throw new AssertionError("Unhandled preparation stage: " + stage);
        }
    }

    @Override
    public String describeState() {
        return "preparation stage " + stage.description + ": " + client.describeScreen();
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
        if (screenName.equals("LoadingErrorScreen")) {
            throw new AssertionError("Mod loading failed: " + client.describeScreen());
        }
        if (!screenName.equals(bootScreen)) {
            bootScreen = screenName;
            options.log("startup-screen " + client.describeScreen());
        }
        if (screenName.contains("AccessibilityOnboarding")) {
            client.press(client.requireButton("Continue"));
            return;
        }
        if (!screenName.equals("TitleScreen") && !screenName.equals("MainMenuScreen")
                && !screenName.equals("GuiMainMenu")) throw new Pending();
        Object minecraft = client.minecraft();
        Object overlay = Reflect.has(minecraft, "getOverlay", 0)
                ? Reflect.call(minecraft, "getOverlay") : Reflect.optionalGet(minecraft, "overlay", "loadingGui");
        if (overlay != null) throw new Pending();
        originalClipboard = client.clipboardText();
        Reflect.set(Reflect.get(minecraft, "options", "gameSettings"), false, "pauseOnLostFocus");
        client.selectLanguage("en_us");
        advance();
    }

    private void openWorldSelection() {
        client.waitForReload();
        client.press(client.requireButton("Singleplayer"));
        advance();
    }

    private void openWorldCreation(String screenName) {
        if (screenName.contains("CreateWorld")) {
            advance();
            return;
        }
        client.press(client.requireButton("Create New World"));
        advance();
    }

    private void nameWorld(String screenName, Object screen) {
        if (!screenName.contains("CreateWorld")) throw new Pending();
        worldName = "ItemNameCopy Test " + UUID.randomUUID().toString().substring(0, 8);
        client.setText(client.firstTextInput(screen), worldName);
        advance();
    }

    private void selectCreativeMode() {
        Object mode = client.requireButtonStartingWith("Game Mode");
        String label = client.label(mode);
        if (label.toLowerCase(Locale.ROOT).contains("creative")) {
            cycleCount = 0;
            advance();
            return;
        }
        if (++cycleCount > 4) throw new AssertionError("Could not select Creative: " + label);
        client.press(mode);
    }

    private void openWorldOptions() {
        Object worldTab = client.findButton("World");
        if (worldTab == null) worldTab = client.findButtonStartingWith("More World Options");
        if (worldTab != null) client.press(worldTab);
        advance();
    }

    private void selectSuperflat() {
        Object type = client.requireButtonStartingWith("World Type");
        String label = client.label(type);
        if (label.toLowerCase(Locale.ROOT).contains("superflat")) {
            cycleCount = 0;
            advance();
            return;
        }
        if (++cycleCount > 12) throw new AssertionError("Could not select Superflat: " + label);
        client.press(type);
    }

    private void configureWorld(Object screen) {
        client.setText(client.firstTextInput(screen), "1");
        Object structures = client.findButtonStartingWith("Generate Structures");
        if (structures == null) structures = client.findButtonStartingWith("Map Features");
        if (structures != null && !client.label(structures).toLowerCase(Locale.ROOT).contains("off")) {
            client.press(structures);
        }
        Object done = client.findButton("Done");
        if (done != null) client.press(done);
        advance();
    }

    private void createWorld() {
        client.press(client.requireButton("Create New World"));
        advance();
    }

    private void enterWorld(String screenName) {
        if (screenName.equals("ConfirmScreen")) {
            client.press(client.requireButton("Yes"));
            return;
        }
        if (client.player() == null || client.screen() != null) throw new Pending();
        TestAssertions.require(Reflect.call(client.minecraft(),
                        "getSingleplayerServer|getIntegratedServer|integratedServer") != null,
                "Expected a local integrated server");
        client.sendCommand("difficulty peaceful");
        advance();
    }

    private void disableGameRule(String legacyName, String modernName) {
        client.disableGameRule(legacyName, modernName);
        advance();
    }

    private void switchToSurvival() {
        client.sendCommand("gamemode survival");
        advance();
    }

    private void switchToJapanese() {
        if (client.isCreative()) throw new Pending();
        client.selectLanguage("ja_jp");
        advance();
    }

    private boolean finishPreparation() {
        client.waitForReload();
        options.log("world-ready " + worldName);
        return true;
    }

    private void advance() {
        stage = stage.next();
        options.log("stage " + stage.description);
    }

    private enum PreparationStage {
        WAIT_FOR_MAIN_MENU("wait-for-main-menu"),
        OPEN_WORLD_SELECTION("open-world-selection"),
        OPEN_WORLD_CREATION("open-world-creation"),
        NAME_WORLD("name-world"),
        SELECT_CREATIVE_MODE("select-creative-mode"),
        OPEN_WORLD_OPTIONS("open-world-options"),
        SELECT_SUPERFLAT("select-superflat"),
        CONFIGURE_WORLD("configure-world"),
        CREATE_WORLD("create-world"),
        ENTER_WORLD("enter-world"),
        DISABLE_MOB_SPAWNING("disable-mob-spawning"),
        DISABLE_DAYLIGHT_CYCLE("disable-daylight-cycle"),
        DISABLE_WEATHER_CYCLE("disable-weather-cycle"),
        SWITCH_TO_SURVIVAL("switch-to-survival"),
        SWITCH_TO_JAPANESE("switch-to-japanese"),
        FINISH("finish");

        private final String description;

        PreparationStage(String description) {
            this.description = description;
        }

        private PreparationStage next() {
            PreparationStage[] stages = values();
            int next = ordinal() + 1;
            if (next >= stages.length) throw new IllegalStateException("Already at final preparation stage");
            return stages[next];
        }
    }
}
