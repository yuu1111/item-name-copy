package dev.itemnamecopy.test.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public final class ClientTestRuntime {
    private static Object minecraft;
    private static Object testScreen;
    private static Object testSlot;
    private static Object search;
    private static Object before;
    private static String expected;
    private static String originalClipboard;
    private static String worldName;
    private static String bootScreen;
    private static int stage;
    private static int delay;
    private static int cycleCount;
    private static long deadline;
    private static boolean busy;
    private static volatile boolean finished;
    private static Future<?> reload;
    private static final List<TestCase> tests = new ArrayList<TestCase>();
    private static final List<Result> results = new ArrayList<Result>();
    private static int testIndex;
    private static int stepIndex;
    private static int simulatedModifiers = -1;
    private static int simulatedAction;
    private static int pointerX = -1;
    private static int pointerY = -1;
    private static final String TARGET = System.getProperty("itemnamecopy.test.target", "unknown");
    private static final Path REPORT = Paths.get(System.getProperty("itemnamecopy.test.report", "client-test-results.json"));
    private static final boolean LEGACY = TARGET.startsWith("1.12.2-");

    private ClientTestRuntime() {}

    public static int controlState() { return simulatedModifiers < 0 ? -1 : (simulatedModifiers & 2) == 0 ? 0 : 1; }
    public static int eventKey() { return simulatedModifiers < 0 ? -1 : 46; }
    public static int eventCharacter() { return simulatedModifiers < 0 ? -1 : 'c'; }
    public static int eventKeyState() { return simulatedModifiers < 0 ? -1 : simulatedAction == 0 ? 0 : 1; }
    public static int repeatState() { return simulatedModifiers < 0 ? -1 : simulatedAction == 2 ? 1 : 0; }
    public static int mouseX() { return pointerX; }
    public static int mouseY() { return pointerY; }
    public static int keyDown(int key) {
        if (simulatedModifiers < 0) return -1;
        if (key == 29 || key == 157) return controlState();
        if (key == 42 || key == 54) return (simulatedModifiers & 1) == 0 ? 0 : 1;
        if (key == 56 || key == 184) return (simulatedModifiers & 4) == 0 ? 0 : 1;
        if (key == 219 || key == 220) return (simulatedModifiers & 8) == 0 ? 0 : 1;
        return key == 46 && simulatedAction != 0 ? 1 : 0;
    }

    public static void tick(Object client) {
        if (finished || busy) return;
        busy = true;
        try {
            if (minecraft == null) {
                minecraft = client;
                Reflect.loader = client.getClass().getClassLoader();
                deadline = System.currentTimeMillis() + 180_000L;
                Thread watchdog = new Thread(() -> {
                    try { Thread.sleep(240_000L); } catch (InterruptedException ignored) { return; }
                    if (!finished) {
                        System.err.println("CLIENT_TEST TIMEOUT " + TARGET);
                        Runtime.getRuntime().halt(124);
                    }
                }, "itemnamecopy-test-watchdog");
                watchdog.setDaemon(true);
                watchdog.start();
                log("starting");
            }
            if (System.currentTimeMillis() > deadline) throw new AssertionError("Timed out at stage " + stage + ": " + describeScreen());
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

    private static void prepareWorld() {
        Object screen = screen();
        String name = screen == null ? "" : screen.getClass().getSimpleName();
        if (stage == 0) {
            if (!name.equals(bootScreen)) {
                bootScreen = name;
                log("startup-screen " + describeScreen());
            }
            if (name.contains("AccessibilityOnboarding")) {
                press(findButton("Continue", false));
                return;
            }
            if (!name.equals("TitleScreen") && !name.equals("MainMenuScreen") && !name.equals("GuiMainMenu")) throw new Pending();
            Object overlay = Reflect.has(minecraft, "getOverlay", 0) ? Reflect.call(minecraft, "getOverlay") : Reflect.optionalGet(minecraft, "overlay", "loadingGui");
            if (overlay != null) throw new Pending();
            originalClipboard = clipboard();
            Object options = Reflect.get(minecraft, "options", "gameSettings");
            Reflect.set(options, false, "pauseOnLostFocus");
            language("en_us");
            nextStage();
        } else if (stage == 1) {
            waitForReload();
            press(findButton("Singleplayer", false));
            nextStage();
        } else if (stage == 2) {
            if (name.contains("CreateWorld")) { nextStage(); return; }
            press(findButton("Create New World", false));
            nextStage();
        } else if (stage == 3) {
            if (!name.contains("CreateWorld")) throw new Pending();
            Object input = firstInput(screen);
            worldName = "ItemNameCopy Test " + UUID.randomUUID().toString().substring(0, 8);
            setText(input, worldName);
            nextStage();
        } else if (stage == 4) {
            Object mode = findButton("Game Mode", true);
            String label = label(mode);
            if (label.toLowerCase(java.util.Locale.ROOT).contains("creative")) { cycleCount = 0; nextStage(); }
            else {
                if (++cycleCount > 4) throw new AssertionError("Could not select Creative: " + label);
                press(mode);
            }
        } else if (stage == 5) {
            Object worldTab = optionalButton("World", false);
            if (worldTab == null) worldTab = optionalButton("More World Options", true);
            if (worldTab != null) press(worldTab);
            nextStage();
        } else if (stage == 6) {
            Object type = findButton("World Type", true);
            String label = label(type);
            if (label.toLowerCase(java.util.Locale.ROOT).contains("superflat")) { cycleCount = 0; nextStage(); }
            else {
                if (++cycleCount > 12) throw new AssertionError("Could not select Superflat: " + label);
                press(type);
            }
        } else if (stage == 7) {
            setText(firstInput(screen), "1");
            Object structures = optionalButton("Generate Structures", true);
            if (structures == null) structures = optionalButton("Map Features", true);
            if (structures != null && !label(structures).toLowerCase(java.util.Locale.ROOT).contains("off")) press(structures);
            Object done = optionalButton("Done", false);
            if (done != null) press(done);
            nextStage();
        } else if (stage == 8) {
            press(findButton("Create New World", false));
            nextStage();
        } else if (stage == 9) {
            if (name.equals("ConfirmScreen")) {
                press(findButton("Yes", false));
                return;
            }
            if (player() == null || screen() != null) throw new Pending();
            require(Reflect.call(minecraft, "getSingleplayerServer|getIntegratedServer|integratedServer") != null, "Expected a local integrated server");
            command("difficulty peaceful");
            nextStage();
        } else if (stage == 10) {
            command("gamerule doMobSpawning false");
            nextStage();
        } else if (stage == 11) {
            command("gamerule doDaylightCycle false");
            nextStage();
        } else if (stage == 12) {
            command("gamerule doWeatherCycle false");
            nextStage();
        } else if (stage == 13) {
            command("gamemode survival");
            nextStage();
        } else if (stage == 14) {
            if (creative()) throw new Pending();
            language("ja_jp");
            nextStage();
        } else if (stage == 15) {
            waitForReload();
            defineTests();
            stage = 20;
            deadline = System.currentTimeMillis() + 120_000L;
            log("world-ready " + worldName);
        }
    }

    private static void defineTests() {
        test("world-settings",
            () -> {
                Object level = Reflect.get(minecraft, "level", "world");
                equal("PEACEFUL", String.valueOf(Reflect.call(level, "getDifficulty")));
                Object server = Reflect.call(minecraft, "getSingleplayerServer|getIntegratedServer|integratedServer");
                Object data;
                if (Reflect.has(server, "getWorldData", 0)) data = Reflect.call(server, "getWorldData");
                else {
                    Object serverLevel;
                    if (LEGACY) serverLevel = Reflect.call(server, "getWorld", 0);
                    else {
                        Object dimension = Reflect.get(Reflect.type("net.minecraft.world.level.dimension.DimensionType",
                            "net.minecraft.world.dimension.DimensionType"), "OVERWORLD");
                        serverLevel = Reflect.call(server, "getLevel|getWorld", dimension);
                    }
                    data = Reflect.call(serverLevel, "getLevelData|getWorldInfo");
                }
                Object settings = Reflect.has(data, "getLevelSettings", 0) ? Reflect.call(data, "getLevelSettings") : data;
                boolean cheats = (Boolean) Reflect.call(settings, "allowCommands|isAllowCommands|getAllowCommands|areCommandsAllowed");
                require(cheats, "Commands are disabled");
                if (Reflect.has(data, "isFlatWorld", 0)) require((Boolean) Reflect.call(data, "isFlatWorld"), "World is not flat");
                else if (Reflect.has(data, "worldGenSettings", 0)) {
                    require((Boolean) Reflect.call(Reflect.call(data, "worldGenSettings"), "isFlatWorld"), "World is not flat");
                }
                else {
                    Object worldType = Reflect.call(data, "getGeneratorType|getTerrainType|getGenerator");
                    String type = String.valueOf(Reflect.call(worldType, "getName"));
                    require(type.toLowerCase(java.util.Locale.ROOT).contains("flat"), "World is not flat: " + type);
                }
            });
        test("survival-copy-and-item-unchanged",
            () -> inventory(false), () -> seed("survival-sentinel"),
            () -> { hover(36); before = Reflect.call(item(testSlot), "copy"); },
            () -> copy(1, 2), () -> copy(0, 2),
            () -> { equal("オークの原木", clipboard()); equal(String.valueOf(before), String.valueOf(item(testSlot))); });
        test("localized-success-notification",
            () -> inventory(false), () -> seed("notification-sentinel"), () -> hover(36),
            () -> copy(1, 2), () -> copy(0, 2),
            () -> equal("コピーしました: オークの原木", overlayText()));
        test("empty-slot-preserves-clipboard",
            () -> inventory(false), () -> seed("empty-sentinel"), () -> hover(37),
            () -> copy(1, 2), () -> copy(0, 2), () -> equal("empty-sentinel", clipboard()));
        test("custom-name-exact-text",
            () -> inventory(true), () -> seed("custom-sentinel"), () -> hover(36),
            () -> copy(1, 2), () -> copy(0, 2), () -> equal("  名付けた剣 ✨  ", clipboard()));
        test("held-key-does-not-repeat",
            () -> inventory(false), () -> seed("held-sentinel"), () -> hover(36),
            () -> copy(1, 2), () -> equal("オークの原木", clipboard()),
            () -> seed("repeat-sentinel"), () -> copy(2, 2), () -> equal("repeat-sentinel", clipboard()),
            () -> copy(0, 2), () -> copy(1, 2), () -> copy(0, 2), () -> equal("オークの原木", clipboard()));
        for (final int flags : new int[] {0, 3, 6, 10}) {
            test("ignored-modifiers-" + flags,
                () -> inventory(false), () -> seed("modifier-sentinel"), () -> hover(36),
                () -> copy(1, flags), () -> copy(0, flags), () -> equal("modifier-sentinel", clipboard()));
        }
        test("recipe-search-priority-and-return-to-copy",
            () -> inventory(false),
            () -> {
                Object book = recipeBook();
                if (!(Boolean) Reflect.call(book, "isVisible")) toggleRecipe();
                search = Reflect.get(book, "searchBox", "searchBar", "searchField");
                if (!LEGACY) Reflect.call(testScreen, "setFocused", book);
                select(search, "recipe-copy-test");
            },
            () -> seed("recipe-sentinel"), () -> hover(36), () -> copy(1, 2), () -> copy(0, 2),
            () -> equal("recipe-copy-test", clipboard()),
            () -> toggleRecipe(), () -> hover(36),
            () -> copy(1, 2), () -> copy(0, 2), () -> equal("オークの原木", clipboard()));
        test("creative-copy",
            () -> { show(null); command("gamemode creative"); },
            () -> { if (!creative()) throw new Pending(); openCreative(); },
            () -> seed("creative-sentinel"),
            () -> {
                int index = firstOccupiedSlot();
                hover(index);
                expected = text(Reflect.call(item(testSlot), "getHoverName|getDisplayName"));
            },
            () -> copy(1, 2), () -> copy(0, 2), () -> equal(expected, clipboard()));
        test("creative-search-priority",
            () -> openCreative(), () -> selectCreativeSearch(), () -> seed("creative-search-sentinel"),
            () -> hover(firstOccupiedSlot()), () -> copy(1, 2), () -> copy(0, 2), () -> equal("トウヒ", clipboard()));
    }

    private static void runNextStep() {
        if (testIndex == tests.size()) { finish(); return; }
        TestCase test = tests.get(testIndex);
        try {
            test.steps[stepIndex].run();
            if (++stepIndex == test.steps.length) {
                results.add(new Result(test.name, null));
                log("PASS " + test.name);
                testIndex++;
                stepIndex = 0;
            }
        } catch (Pending ignored) {
        } catch (Throwable error) {
            failure(test.name, error);
            simulatedModifiers = -1;
            testIndex++;
            stepIndex = 0;
        }
    }

    private static void inventory(boolean custom) {
        show(null);
        Object stack;
        Class<?> itemStack = Reflect.type("net.minecraft.world.item.ItemStack", "net.minecraft.item.ItemStack");
        if (LEGACY) {
            Object block = Reflect.get(Reflect.type("net.minecraft.init.Blocks"), "LOG");
            stack = Reflect.make(itemStack, block, 3);
        } else {
            Object item = Reflect.get(Reflect.type("net.minecraft.world.item.Items", "net.minecraft.item.Items"), custom ? "DIAMOND_SWORD" : "OAK_LOG");
            stack = Reflect.make(itemStack, item, 3);
        }
        if (custom) {
            String value = "  名付けた剣 ✨  ";
            if (LEGACY) Reflect.call(stack, "setStackDisplayName", value);
            else if (Reflect.has(stack, "setHoverName|setDisplayName", 1)) Reflect.call(stack, "setHoverName|setDisplayName", literal(value));
            else {
                Object customName = Reflect.get(Reflect.type("net.minecraft.core.component.DataComponents"), "CUSTOM_NAME");
                Reflect.call(stack, "set", customName, literal(value));
            }
        }
        Object inventory = Reflect.has(player(), "getInventory", 0) ? Reflect.call(player(), "getInventory") : Reflect.get(player(), "inventory");
        Reflect.call(inventory, "setItem|setInventorySlotContents", 0, stack);
        testScreen = Reflect.make(Reflect.type("net.minecraft.client.gui.screens.inventory.InventoryScreen",
            "net.minecraft.client.gui.screen.inventory.InventoryScreen", "net.minecraft.client.gui.inventory.GuiInventory"), player());
        show(testScreen);
        Object book = recipeBook();
        if ((Boolean) Reflect.call(book, "isVisible")) toggleRecipe();
    }

    private static void openCreative() {
        Object level = Reflect.get(minecraft, "level", "world");
        Class<?> type = Reflect.type("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
            "net.minecraft.client.gui.screen.inventory.CreativeScreen", "net.minecraft.client.gui.inventory.GuiContainerCreative");
        if (Reflect.has(level, "enabledFeatures", 0)) testScreen = Reflect.make(type, player(), Reflect.call(level, "enabledFeatures"), true);
        else testScreen = Reflect.make(type, player());
        show(testScreen);
    }

    private static Object recipeBook() {
        if (!LEGACY) return Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "recipeBook", testScreen);
        return Reflect.get(testScreen, "recipeBookGui");
    }

    private static void toggleRecipe() {
        for (Object widget : widgets(testScreen)) {
            String name = widget.getClass().getSimpleName();
            if (name.equals("ImageButton") || name.equals("GuiButtonImage")) {
                press(widget);
                return;
            }
        }
        throw new IllegalStateException("Recipe button missing: " + describeScreen());
    }

    private static void selectCreativeSearch() {
        Class<?> tabs = Reflect.type("net.minecraft.world.item.CreativeModeTabs", "net.minecraft.world.item.CreativeModeTab",
            "net.minecraft.item.ItemGroup", "net.minecraft.creativetab.CreativeTabs");
        Object tab = Reflect.has(tabs, "searchTab", 0) ? Reflect.call(tabs, "searchTab") : Reflect.get(tabs, "TAB_SEARCH", "SEARCH");
        Reflect.call(testScreen, "selectTab|setCurrentCreativeTab", tab);
        search = Reflect.get(testScreen, "searchBox", "searchField");
        select(search, "トウヒ");
    }

    private static int firstOccupiedSlot() {
        List<?> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            if ((Boolean) Reflect.call(slots.get(i), "hasItem|getHasStack")) return i;
        }
        throw new AssertionError("No occupied slot");
    }

    private static List<?> slots() {
        Object menu = Reflect.has(testScreen, "getMenu|getContainer", 0) ? Reflect.call(testScreen, "getMenu|getContainer")
            : Reflect.get(testScreen, "menu", "container", "inventorySlots");
        return (List<?>) Reflect.get(menu, "slots", "inventorySlots");
    }

    private static Object item(Object slot) { return Reflect.call(slot, "getItem|getStack"); }

    private static void hover(int index) {
        testSlot = slots().get(index);
        int x = ((Number) Reflect.get(testScreen, "leftPos", "guiLeft")).intValue()
            + ((Number) Reflect.get(testSlot, "x", "xPos")).intValue() + 8;
        int y = ((Number) Reflect.get(testScreen, "topPos", "guiTop")).intValue()
            + ((Number) Reflect.get(testSlot, "y", "yPos")).intValue() + 8;
        int guiWidth = ((Number) Reflect.get(testScreen, "width")).intValue();
        int guiHeight = ((Number) Reflect.get(testScreen, "height")).intValue();
        if (LEGACY) {
            int width = ((Number) Reflect.get(minecraft, "displayWidth")).intValue();
            int height = ((Number) Reflect.get(minecraft, "displayHeight")).intValue();
            pointerX = x * width / guiWidth;
            pointerY = height - y * height / guiHeight - 1;
        } else {
            Object window = window();
            int width = ((Number) Reflect.call(window, "getScreenWidth|getWidth")).intValue();
            int height = ((Number) Reflect.call(window, "getScreenHeight|getHeight")).intValue();
            Object mouse = Reflect.get(minecraft, "mouseHandler", "mouseHelper");
            Reflect.call(mouse, "onMove|cursorPosCallback", handle(), (double) x * width / guiWidth, (double) y * height / guiHeight);
        }
    }

    private static void copy(int action, int flags) {
        if (action != 0) {
            Object hovered = LEGACY ? Reflect.get(testScreen, "hoveredSlot")
                : Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "hoveredSlot", testScreen);
            if (hovered != testSlot) {
                hover(slots().indexOf(testSlot));
                throw new Pending();
            }
        }
        simulatedModifiers = flags;
        simulatedAction = action;
        try {
            if (LEGACY) {
                Class<?> eventType = Reflect.type("net.minecraftforge.client.event.GuiScreenEvent$KeyboardInputEvent$Pre");
                Object event = Reflect.make(eventType, testScreen);
                Object bus = Reflect.get(Reflect.type("net.minecraftforge.common.MinecraftForge"), "EVENT_BUS");
                boolean consumed = (Boolean) Reflect.call(bus, "post", event);
                if (!consumed) Reflect.call(testScreen, "handleKeyboardInput");
            } else {
                Object keyboard = Reflect.get(minecraft, "keyboardHandler", "keyboardListener");
                if (Reflect.has(keyboard, "keyPress|onKeyEvent", 5)) Reflect.call(keyboard, "keyPress|onKeyEvent", handle(), 67, 0, action, flags);
                else {
                    Object event = Reflect.make(Reflect.type("net.minecraft.client.input.KeyEvent"), 67, 0, flags);
                    Reflect.call(keyboard, "keyPress", handle(), action, event);
                }
            }
        } finally { simulatedModifiers = -1; }
    }

    private static void command(String value) {
        Object connection = Reflect.optionalGet(player(), "connection");
        if (Reflect.has(connection, "sendCommand", 1)) Reflect.call(connection, "sendCommand", value);
        else Reflect.call(player(), "chat|sendChatMessage", "/" + value);
    }

    private static boolean creative() {
        Object controller = Reflect.get(minecraft, "gameMode", "playerController");
        if (Reflect.has(controller, "getPlayerMode|getCurrentGameType", 0)) {
            Object mode = Reflect.call(controller, "getPlayerMode|getCurrentGameType");
            return (Boolean) Reflect.call(mode, "isCreative");
        }
        return (Boolean) Reflect.call(controller, "hasInfiniteItems|isInCreativeMode");
    }

    private static Object player() { return Reflect.optionalGet(minecraft, "player"); }
    private static Object window() { return Reflect.has(minecraft, "getWindow|getMainWindow", 0) ? Reflect.call(minecraft, "getWindow|getMainWindow") : Reflect.get(minecraft, "window", "mainWindow"); }
    private static long handle() { return ((Number) Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "windowHandle")).longValue(); }

    private static Object screen() {
        if (LEGACY) return Reflect.optionalGet(minecraft, "currentScreen");
        try { return Reflect.get(minecraft, "screen", "currentScreen"); }
        catch (IllegalStateException missing) { return Reflect.call(Reflect.get(minecraft, "gui"), "screen"); }
    }

    private static void show(Object screen) {
        if (Reflect.has(minecraft, "setScreen|displayGuiScreen", 1)) Reflect.call(minecraft, "setScreen|displayGuiScreen", screen);
        else Reflect.call(Reflect.get(minecraft, "gui"), "setScreen", screen);
    }

    private static String clipboard() {
        if (LEGACY) return (String) Reflect.call(Reflect.type("net.minecraft.client.gui.GuiScreen"), "getClipboardString");
        return (String) Reflect.call(Reflect.get(minecraft, "keyboardHandler", "keyboardListener"), "getClipboard|getClipboardString");
    }

    private static void seed(String value) {
        if (LEGACY) Reflect.call(Reflect.type("net.minecraft.client.gui.GuiScreen"), "setClipboardString", value);
        else Reflect.call(Reflect.get(minecraft, "keyboardHandler", "keyboardListener"), "setClipboard|setClipboardString", value);
        try { Thread.sleep(150L); } catch (InterruptedException error) { throw new IllegalStateException(error); }
        equal(value, clipboard());
    }

    private static Object literal(String value) {
        Class<?> component = Reflect.type("net.minecraft.network.chat.Component", "net.minecraft.util.text.ITextComponent");
        if (Reflect.has(component, "literal", 1)) return Reflect.call(component, "literal", value);
        return Reflect.make(Reflect.type("net.minecraft.network.chat.TextComponent", "net.minecraft.util.text.StringTextComponent"), value);
    }

    private static String overlayText() {
        Object gui = Reflect.get(minecraft, "gui", "ingameGUI");
        Object hud = Reflect.optionalGet(gui, "hud");
        if (hud != null) gui = hud;
        Object message = Reflect.get(gui, "overlayMessageString", "overlayMessage", "recordPlaying");
        if (LEGACY) return (String) Reflect.call(Reflect.type("net.minecraft.util.text.TextFormatting"),
            "getTextWithoutFormattingCodes", text(message));
        return text(message);
    }

    private static void language(String code) {
        Object options = Reflect.get(minecraft, "options", "gameSettings");
        Reflect.set(options, code, "languageCode", "language");
        Object manager = Reflect.call(minecraft, "getLanguageManager");
        try { Reflect.call(manager, "setSelected", code); }
        catch (IllegalStateException missing) {
            Object language = Reflect.call(manager, "getLanguage", code);
            Reflect.call(manager, "setSelected|setCurrentLanguage", language);
        }
        Object result = Reflect.call(minecraft, "reloadResourcePacks|reloadResources|refreshResources");
        reload = result instanceof Future ? (Future<?>) result : null;
    }

    private static void waitForReload() {
        if (reload == null) return;
        if (!reload.isDone()) throw new Pending();
        try { reload.get(); } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private static Object firstInput(Object owner) {
        for (Object value : widgets(owner)) {
            if (Boolean.FALSE.equals(Reflect.optionalGet(value, "visible"))) continue;
            if (isTextInput(value)) return value;
        }
        for (Object value : Reflect.values(owner)) {
            if (isTextInput(value) && !Boolean.FALSE.equals(Reflect.optionalGet(value, "visible"))) return value;
        }
        throw new IllegalStateException("No text field: " + describeScreen());
    }

    private static boolean isTextInput(Object value) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getSimpleName();
            if (name.equals("EditBox") || name.equals("TextFieldWidget") || name.equals("GuiTextField")) return true;
        }
        return false;
    }

    private static void setText(Object input, String value) { Reflect.call(input, "setValue|setText", value); }
    private static void select(Object input, String value) {
        setText(input, value);
        Reflect.call(input, "setFocused", true);
        Reflect.call(input, "setCursorPosition", 0);
        Reflect.call(input, "setHighlightPos|setSelectionPos", value.length());
    }

    private static List<Object> widgets(Object owner) {
        List<Object> found = new ArrayList<Object>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        collectWidgets(owner, found, seen, 0);
        return found;
    }

    private static void collectWidgets(Object owner, List<Object> found, Set<Object> seen, int depth) {
        if (owner == null || !seen.add(owner) || depth > 8) return;
        found.add(owner);
        Object children = Reflect.has(owner, "children|getEventListeners", 0) ? Reflect.call(owner, "children|getEventListeners")
            : Reflect.optionalGet(owner, "buttonList");
        if (children instanceof Iterable) {
            for (Object child : (Iterable<?>) children) collectWidgets(child, found, seen, depth + 1);
        }
    }

    private static String label(Object widget) {
        if (Reflect.has(widget, "getMessage", 0)) return text(Reflect.call(widget, "getMessage"));
        Object value = Reflect.optionalGet(widget, "displayString");
        return value == null ? "" : String.valueOf(value);
    }

    private static String text(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        return String.valueOf(Reflect.call(value, "getString|getUnformattedText"));
    }

    private static Object optionalButton(String text, boolean prefix) {
        List<Object> widgets = widgets(screen());
        for (int i = 0; i < widgets.size(); i++) {
            Object widget = widgets.get(i);
            Object visible = Reflect.optionalGet(widget, "visible");
            if (Boolean.FALSE.equals(visible)) continue;
            String label = label(widget);
            if (!(prefix ? label.toLowerCase(java.util.Locale.ROOT).startsWith(text.toLowerCase(java.util.Locale.ROOT)) : label.equalsIgnoreCase(text))) continue;
            if (isButton(widget)) return widget;
            for (int j = i + 1; j < widgets.size() && j < i + 4; j++) {
                Object adjacent = widgets.get(j);
                if (isButton(adjacent) && !Boolean.FALSE.equals(Reflect.optionalGet(adjacent, "visible"))) return adjacent;
            }
        }
        return null;
    }

    private static boolean isButton(Object widget) {
        for (Class<?> type = widget.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().contains("Button")) return true;
        }
        return false;
    }

    private static Object findButton(String label, boolean prefix) {
        Object button = optionalButton(label, prefix);
        if (button == null) throw new IllegalStateException("Button missing: " + label + "; " + describeScreen());
        return button;
    }

    private static void press(Object button) {
        log("press " + label(button));
        if (button.getClass().getSimpleName().equals("TabButton")) {
            Reflect.call(Reflect.get(button, "tabManager"), "setCurrentTab", Reflect.call(button, "tab"), true);
        }
        else if (Reflect.has(button, "onPress", 0)) Reflect.call(button, "onPress");
        else if (Reflect.has(button, "onPress", 1)) {
            Object key = Reflect.make(Reflect.type("net.minecraft.client.input.KeyEvent"), 257, 0, 0);
            Reflect.call(button, "onPress", key);
        }
        else if (!LEGACY) {
            int x = ((Number) Reflect.call(button, "getX")).intValue();
            int y = ((Number) Reflect.call(button, "getY")).intValue();
            if (Reflect.has(screen(), "mouseClicked", 3)) Reflect.call(screen(), "mouseClicked", (double) x + 2, (double) y + 2, 0);
            else {
                Object info = Reflect.make(Reflect.type("net.minecraft.client.input.MouseButtonInfo"), 0, 0);
                Object event = Reflect.make(Reflect.type("net.minecraft.client.input.MouseButtonEvent"), (double) x + 2, (double) y + 2, info);
                Reflect.call(screen(), "mouseClicked", event, false);
            }
        }
        else if (LEGACY) Reflect.call(screen(), "actionPerformed", button);
        else throw new IllegalStateException("No button action: " + button.getClass().getName());
    }

    private static String describeScreen() {
        Object screen = screen();
        if (screen == null) return "no screen";
        List<String> labels = new ArrayList<String>();
        for (Object widget : widgets(screen)) { String text = label(widget); if (!text.isEmpty()) labels.add(text); }
        return screen.getClass().getName() + " " + labels;
    }

    private static void nextStage() { stage++; log("stage " + stage); }
    private static void log(String value) { System.out.println("CLIENT_TEST " + TARGET + " " + value); }
    private static void test(String name, Step... steps) { tests.add(new TestCase(name, steps)); }
    private static void equal(Object expected, Object actual) { require(java.util.Objects.equals(expected, actual), "Expected <" + expected + "> but was <" + actual + ">"); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static void failure(String name, Throwable error) {
        results.add(new Result(name, error.toString()));
        log("FAIL " + name + ": " + error);
        error.printStackTrace();
    }

    private static void finish() {
        if (finished) return;
        finished = true;
        pointerX = -1;
        pointerY = -1;
        simulatedModifiers = -1;
        try {
            if (originalClipboard != null) {
                if (LEGACY) {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(originalClipboard), null);
                } else {
                    Object clipboard = Reflect.make(Reflect.type("com.mojang.blaze3d.platform.ClipboardManager", "net.minecraft.client.ClipboardHelper"));
                    Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "writeClipboard", clipboard, originalClipboard);
                }
            }
        } catch (Throwable error) { failure("clipboard-restore", error); }
        try { writeReport(); } catch (Throwable error) { error.printStackTrace(); }
        try { Reflect.call(minecraft, "stop|shutdown"); }
        catch (Throwable error) { error.printStackTrace(); Runtime.getRuntime().halt(2); }
    }

    private static void writeReport() throws IOException {
        int failed = 0;
        for (Result result : results) if (result.failure != null) failed++;
        StringBuilder json = new StringBuilder("{\n  \"target\": ").append(quote(TARGET))
            .append(",\n  \"source\": ").append(quote(System.getProperty("itemnamecopy.test.source", "unknown")))
            .append(",\n  \"mode\": \"real client, synthetic input callbacks, OS clipboard\",")
            .append("\n  \"passed\": ").append(results.size() - failed).append(",\n  \"failed\": ").append(failed)
            .append(",\n  \"expectedTests\": ").append(tests.size()).append(",\n  \"tests\": [");
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuite name=\"")
            .append(TARGET).append("\" tests=\"").append(results.size()).append("\" failures=\"").append(failed).append("\">");
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (i > 0) json.append(',');
            json.append("\n    {\"name\": ").append(quote(result.name)).append(", \"status\": ")
                .append(quote(result.failure == null ? "passed" : "failed"));
            if (result.failure != null) json.append(", \"message\": ").append(quote(result.failure));
            json.append('}');
            xml.append("<testcase classname=\"ClientTestRuntime\" name=\"").append(result.name).append("\">");
            if (result.failure != null) xml.append("<failure message=\"").append(escapeXml(result.failure)).append("\"/>");
            xml.append("</testcase>");
        }
        json.append("\n  ]\n}\n");
        xml.append("</testsuite>");
        Files.createDirectories(REPORT.toAbsolutePath().getParent());
        Files.write(REPORT, json.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(REPORT.resolveSibling("TEST-client.xml"), xml.toString().getBytes(StandardCharsets.UTF_8));
        log("RESULT " + (results.size() - failed) + " passed, " + failed + " failed");
    }

    private static String quote(String text) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '"' || character == '\\') escaped.append('\\').append(character);
            else if (character < 32) escaped.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) character));
            else escaped.append(character);
        }
        return escaped.append('"').toString();
    }

    private static String escapeXml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private interface Step { void run(); }
    private static final class Pending extends RuntimeException { }
    private static final class TestCase {
        final String name;
        final Step[] steps;
        TestCase(String name, Step[] steps) { this.name = name; this.steps = steps; }
    }
    private static final class Result {
        final String name;
        final String failure;
        Result(String name, String failure) { this.name = name; this.failure = failure; }
    }
}
