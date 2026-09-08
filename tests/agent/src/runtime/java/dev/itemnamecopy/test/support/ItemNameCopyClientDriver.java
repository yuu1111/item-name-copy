package dev.itemnamecopy.test.support;

import dev.itemnamecopy.test.runtime.Pending;
import dev.itemnamecopy.test.runtime.Reflect;
import dev.itemnamecopy.test.runtime.RuntimeConfig;
import dev.itemnamecopy.test.runtime.SyntheticInput;
import dev.itemnamecopy.test.runtime.TestAssertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public final class ItemNameCopyClientDriver {
    private static final boolean LEGACY = RuntimeConfig.TARGET.startsWith("1.12.2-");

    private final SyntheticInput input;
    private Object minecraft;
    private Object testScreen;
    private Object testSlot;
    private Future<?> reload;

    public ItemNameCopyClientDriver(SyntheticInput input) {
        this.input = input;
    }

    public void initialize(Object client) {
        minecraft = client;
        Reflect.initialize(client.getClass().getClassLoader());
    }

    public Object minecraft() {
        return minecraft;
    }

    public Object testScreen() {
        return testScreen;
    }

    public Object testSlot() {
        return testSlot;
    }

    public boolean isLegacy() {
        return LEGACY;
    }

    public void inventory(boolean custom) {
        show(null);
        Object stack;
        Class<?> itemStack = Reflect.type("net.minecraft.world.item.ItemStack", "net.minecraft.item.ItemStack");
        if (LEGACY) {
            Object block = Reflect.get(Reflect.type("net.minecraft.init.Blocks"), "LOG");
            stack = Reflect.make(itemStack, block, 3);
        } else {
            Object item = Reflect.get(Reflect.type("net.minecraft.world.item.Items", "net.minecraft.item.Items"),
                    custom ? "DIAMOND_SWORD" : "OAK_LOG");
            stack = Reflect.make(itemStack, item, 3);
        }
        if (custom) {
            String value = "  名付けた剣 ✨  ";
            if (LEGACY) Reflect.call(stack, "setStackDisplayName", value);
            else if (Reflect.has(stack, "setHoverName|setDisplayName", 1)) {
                Reflect.call(stack, "setHoverName|setDisplayName", literal(value));
            } else {
                Object customName = Reflect.get(Reflect.type("net.minecraft.core.component.DataComponents"), "CUSTOM_NAME");
                Reflect.call(stack, "set", customName, literal(value));
            }
        }
        Object inventory = Reflect.has(player(), "getInventory", 0)
                ? Reflect.call(player(), "getInventory") : Reflect.get(player(), "inventory");
        Reflect.call(inventory, "setItem|setInventorySlotContents", 0, stack);
        testScreen = Reflect.make(Reflect.type("net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "net.minecraft.client.gui.screen.inventory.InventoryScreen", "net.minecraft.client.gui.inventory.GuiInventory"), player());
        show(testScreen);
        Object book = recipeBook();
        if ((Boolean) Reflect.call(book, "isVisible")) toggleRecipe();
    }

    public void openCreative() {
        Object level = Reflect.get(minecraft, "level", "world");
        Class<?> type = Reflect.type("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                "net.minecraft.client.gui.screen.inventory.CreativeScreen", "net.minecraft.client.gui.inventory.GuiContainerCreative");
        if (Reflect.has(level, "enabledFeatures", 0)) {
            testScreen = Reflect.make(type, player(), Reflect.call(level, "enabledFeatures"), true);
        } else testScreen = Reflect.make(type, player());
        show(testScreen);
    }

    public Object recipeBook() {
        if (!LEGACY) {
            return Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "recipeBook", testScreen);
        }
        return Reflect.get(testScreen, "recipeBookGui");
    }

    public void toggleRecipe() {
        for (Object widget : widgets(testScreen)) {
            String name = widget.getClass().getSimpleName();
            if (name.equals("ImageButton") || name.equals("GuiButtonImage")) {
                press(widget);
                return;
            }
        }
        throw new IllegalStateException("Recipe button missing: " + describeScreen());
    }

    public void selectRecipeSearch(String value) {
        Object book = recipeBook();
        if (!(Boolean) Reflect.call(book, "isVisible")) toggleRecipe();
        Object search = Reflect.get(book, "searchBox", "searchBar", "searchField");
        if (!LEGACY) Reflect.call(testScreen, "setFocused", book);
        select(search, value);
    }

    public void selectCreativeSearch() {
        Class<?> tabs = Reflect.type("net.minecraft.world.item.CreativeModeTabs", "net.minecraft.world.item.CreativeModeTab",
                "net.minecraft.item.ItemGroup", "net.minecraft.creativetab.CreativeTabs");
        Object tab = Reflect.has(tabs, "searchTab", 0)
                ? Reflect.call(tabs, "searchTab") : Reflect.get(tabs, "TAB_SEARCH", "SEARCH");
        Reflect.call(testScreen, "selectTab|setCurrentCreativeTab", tab);
        select(Reflect.get(testScreen, "searchBox", "searchField"), "トウヒ");
    }

    public int firstOccupiedSlot() {
        List<?> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            if ((Boolean) Reflect.call(slots.get(i), "hasItem|getHasStack")) return i;
        }
        throw new AssertionError("No occupied slot");
    }

    public List<?> slots() {
        Object menu = Reflect.has(testScreen, "getMenu|getContainer", 0)
                ? Reflect.call(testScreen, "getMenu|getContainer")
                : Reflect.get(testScreen, "menu", "container", "inventorySlots");
        return (List<?>) Reflect.get(menu, "slots", "inventorySlots");
    }

    public Object item(Object slot) {
        return Reflect.call(slot, "getItem|getStack");
    }

    public void hover(int index) {
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
            input.movePointer(x * width / guiWidth, height - y * height / guiHeight - 1);
        } else {
            Object window = window();
            int width = ((Number) Reflect.call(window, "getScreenWidth|getWidth")).intValue();
            int height = ((Number) Reflect.call(window, "getScreenHeight|getHeight")).intValue();
            Object mouse = Reflect.get(minecraft, "mouseHandler", "mouseHelper");
            Reflect.call(mouse, "onMove|cursorPosCallback", handle(),
                    (double) x * width / guiWidth, (double) y * height / guiHeight);
        }
    }

    public void copy(int action, int flags) {
        if (action != 0) {
            Object hovered = LEGACY ? Reflect.get(testScreen, "hoveredSlot")
                    : Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "hoveredSlot", testScreen);
            if (hovered != testSlot) {
                hover(slots().indexOf(testSlot));
                throw new Pending();
            }
        }
        input.beginKeyEvent(action, flags);
        try {
            if (LEGACY) {
                Class<?> eventType = Reflect.type("net.minecraftforge.client.event.GuiScreenEvent$KeyboardInputEvent$Pre");
                Object event = Reflect.make(eventType, testScreen);
                Object bus = Reflect.get(Reflect.type("net.minecraftforge.common.MinecraftForge"), "EVENT_BUS");
                boolean consumed = (Boolean) Reflect.call(bus, "post", event);
                if (!consumed) Reflect.call(testScreen, "handleKeyboardInput");
            } else {
                Object keyboard = Reflect.get(minecraft, "keyboardHandler", "keyboardListener");
                if (Reflect.has(keyboard, "keyPress|onKeyEvent", 5)) {
                    Reflect.call(keyboard, "keyPress|onKeyEvent", handle(), 67, 0, action, flags);
                } else {
                    Object event = Reflect.make(Reflect.type("net.minecraft.client.input.KeyEvent"), 67, 0, flags);
                    Reflect.call(keyboard, "keyPress", handle(), action, event);
                }
            }
        } finally {
            input.endKeyEvent();
        }
    }

    public void command(String value) {
        Object connection = Reflect.optionalGet(player(), "connection");
        if (Reflect.has(connection, "sendCommand", 1)) Reflect.call(connection, "sendCommand", value);
        else Reflect.call(player(), "chat|sendChatMessage", "/" + value);
    }

    public void gameRuleCommand(String legacy, String modern) {
        command("gamerule " + (modernGameRules() ? "minecraft:" + modern : legacy) + " false");
    }

    public void verifyGameRules(Object server) {
        Object rules;
        if (Reflect.has(server, "getGameRules", 0)) rules = Reflect.call(server, "getGameRules");
        else {
            Object level;
            if (LEGACY) level = Reflect.call(server, "getWorld", 0);
            else if (Reflect.has(server, "overworld", 0)) level = Reflect.call(server, "overworld");
            else level = Reflect.call(server, "getLevel|getWorld", Reflect.get(Reflect.type(
                        "net.minecraft.world.level.dimension.DimensionType", "net.minecraft.world.dimension.DimensionType"), "OVERWORLD"));
            rules = Reflect.call(level, "getGameRules");
        }
        String[] oldNames = {"doMobSpawning", "doDaylightCycle", "doWeatherCycle"};
        String[] newNames = {"SPAWN_MOBS", "ADVANCE_TIME", "ADVANCE_WEATHER"};
        String[] keyNames = {"RULE_DOMOBSPAWNING", "RULE_DAYLIGHT", "RULE_WEATHER_CYCLE"};
        for (int i = 0; i < oldNames.length; i++) {
            Object value;
            if (modernGameRules()) value = Reflect.call(rules, "get", Reflect.get(
                    Reflect.type("net.minecraft.world.level.gamerules.GameRules"), newNames[i]));
            else if (Reflect.optionalGet(rules.getClass(), keyNames[i]) != null) {
                value = Reflect.call(rules, "getBoolean", Reflect.get(rules.getClass(), keyNames[i]));
            } else value = Reflect.call(rules, "getBoolean|func_82766_b|method_8355", oldNames[i]);
            TestAssertions.equal(Boolean.FALSE, value);
        }
    }

    public boolean creative() {
        Object controller = Reflect.get(minecraft, "gameMode", "playerController");
        if (Reflect.has(controller, "getPlayerMode|getCurrentGameType", 0)) {
            Object mode = Reflect.call(controller, "getPlayerMode|getCurrentGameType");
            return (Boolean) Reflect.call(mode, "isCreative");
        }
        return (Boolean) Reflect.call(controller, "hasInfiniteItems|isInCreativeMode");
    }

    public Object player() {
        return Reflect.optionalGet(minecraft, "player");
    }

    public Object screen() {
        if (LEGACY) return Reflect.optionalGet(minecraft, "currentScreen");
        try {
            return Reflect.get(minecraft, "screen", "currentScreen");
        } catch (IllegalStateException missing) {
            return Reflect.call(Reflect.get(minecraft, "gui"), "screen");
        }
    }

    public void show(Object screen) {
        if (Reflect.has(minecraft, "setScreen|displayGuiScreen", 1)) {
            Reflect.call(minecraft, "setScreen|displayGuiScreen", screen);
        } else Reflect.call(Reflect.get(minecraft, "gui"), "setScreen", screen);
    }

    public String clipboard() {
        if (LEGACY) {
            return (String) Reflect.call(Reflect.type("net.minecraft.client.gui.GuiScreen"), "getClipboardString");
        }
        return (String) Reflect.call(Reflect.get(minecraft, "keyboardHandler", "keyboardListener"),
                "getClipboard|getClipboardString");
    }

    public void seedClipboard(String value) {
        if (LEGACY) {
            Reflect.call(Reflect.type("net.minecraft.client.gui.GuiScreen"), "setClipboardString", value);
        } else {
            Reflect.call(Reflect.get(minecraft, "keyboardHandler", "keyboardListener"),
                    "setClipboard|setClipboardString", value);
        }
        try {
            Thread.sleep(150L);
        } catch (InterruptedException error) {
            throw new IllegalStateException(error);
        }
        TestAssertions.equal(value, clipboard());
    }

    public void restoreClipboard(String value) {
        if (LEGACY) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(value), null);
        } else {
            Object clipboard = Reflect.make(Reflect.type(
                    "com.mojang.blaze3d.platform.ClipboardManager", "net.minecraft.client.ClipboardHelper"));
            Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "writeClipboard", clipboard, value);
        }
    }

    public String overlayText() {
        Object gui = Reflect.get(minecraft, "gui", "ingameGUI");
        Object hud = Reflect.optionalGet(gui, "hud");
        if (hud != null) gui = hud;
        Object message = Reflect.get(gui, "overlayMessageString", "overlayMessage", "recordPlaying");
        if (LEGACY) {
            return (String) Reflect.call(Reflect.type("net.minecraft.util.text.TextFormatting"),
                    "getTextWithoutFormattingCodes", text(message));
        }
        return text(message);
    }

    public void language(String code) {
        Object options = Reflect.get(minecraft, "options", "gameSettings");
        Reflect.set(options, code, "languageCode", "language");
        Object manager = Reflect.call(minecraft, "getLanguageManager");
        try {
            Reflect.call(manager, "setSelected", code);
        } catch (IllegalStateException missing) {
            Object language = Reflect.call(manager, "getLanguage", code);
            Reflect.call(manager, "setSelected|setCurrentLanguage", language);
        }
        Object result = Reflect.call(minecraft, "reloadResourcePacks|reloadResources|refreshResources");
        reload = result instanceof Future ? (Future<?>) result : null;
    }

    public void waitForReload() {
        if (reload == null) return;
        if (!reload.isDone()) throw new Pending();
        try {
            reload.get();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    public Object firstInput(Object owner) {
        for (Object value : widgets(owner)) {
            if (Boolean.FALSE.equals(Reflect.optionalGet(value, "visible"))) continue;
            if (isTextInput(value)) return value;
        }
        for (Object value : Reflect.values(owner)) {
            if (isTextInput(value) && !Boolean.FALSE.equals(Reflect.optionalGet(value, "visible"))) return value;
        }
        throw new IllegalStateException("No text field: " + describeScreen());
    }

    public void setText(Object textInput, String value) {
        Reflect.call(textInput, "setValue|setText", value);
    }

    public List<Object> widgets(Object owner) {
        List<Object> found = new ArrayList<Object>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        collectWidgets(owner, found, seen, 0);
        return found;
    }

    public String label(Object widget) {
        if (Reflect.has(widget, "getMessage", 0)) return text(Reflect.call(widget, "getMessage"));
        Object value = Reflect.optionalGet(widget, "displayString");
        return value == null ? "" : String.valueOf(value);
    }

    public String text(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        return String.valueOf(Reflect.call(value, "getString|getUnformattedText"));
    }

    public Object optionalButton(String text, boolean prefix) {
        List<Object> widgets = widgets(screen());
        for (int i = 0; i < widgets.size(); i++) {
            Object widget = widgets.get(i);
            Object visible = Reflect.optionalGet(widget, "visible");
            if (Boolean.FALSE.equals(visible)) continue;
            String label = label(widget);
            boolean matches = prefix
                    ? label.toLowerCase(java.util.Locale.ROOT).startsWith(text.toLowerCase(java.util.Locale.ROOT))
                    : label.equalsIgnoreCase(text);
            if (!matches) continue;
            if (isButton(widget)) return widget;
            for (int j = i + 1; j < widgets.size() && j < i + 4; j++) {
                Object adjacent = widgets.get(j);
                if (isButton(adjacent) && !Boolean.FALSE.equals(Reflect.optionalGet(adjacent, "visible")))
                    return adjacent;
            }
        }
        return null;
    }

    public Object findButton(String label, boolean prefix) {
        Object button = optionalButton(label, prefix);
        if (button == null) throw new IllegalStateException("Button missing: " + label + "; " + describeScreen());
        return button;
    }

    public void press(Object button) {
        RuntimeConfig.log("press " + label(button));
        if (button.getClass().getSimpleName().equals("TabButton")) {
            Reflect.call(Reflect.get(button, "tabManager"), "setCurrentTab", Reflect.call(button, "tab"), true);
        } else if (Reflect.has(button, "onPress", 0)) Reflect.call(button, "onPress");
        else if (Reflect.has(button, "onPress", 1)) {
            Object key = Reflect.make(Reflect.type("net.minecraft.client.input.KeyEvent"), 257, 0, 0);
            Reflect.call(button, "onPress", key);
        } else if (!LEGACY) {
            int x = ((Number) Reflect.call(button, "getX")).intValue();
            int y = ((Number) Reflect.call(button, "getY")).intValue();
            if (Reflect.has(screen(), "mouseClicked", 3)) {
                Reflect.call(screen(), "mouseClicked", (double) x + 2, (double) y + 2, 0);
            } else {
                Object info = Reflect.make(Reflect.type("net.minecraft.client.input.MouseButtonInfo"), 0, 0);
                Object event = Reflect.make(Reflect.type("net.minecraft.client.input.MouseButtonEvent"),
                        (double) x + 2, (double) y + 2, info);
                Reflect.call(screen(), "mouseClicked", event, false);
            }
        } else Reflect.call(screen(), "actionPerformed", button);
    }

    public String describeScreen() {
        Object screen = screen();
        if (screen == null) return "no screen";
        List<String> labels = new ArrayList<String>();
        for (Object widget : widgets(screen)) {
            String text = label(widget);
            if (!text.isEmpty()) labels.add(text);
        }
        return screen.getClass().getName() + " " + labels;
    }

    public void stop() {
        Reflect.call(minecraft, "stop|shutdown");
    }

    private boolean modernGameRules() {
        try {
            Reflect.type("net.minecraft.world.level.gamerules.GameRules");
            return true;
        } catch (IllegalStateException missing) {
            return false;
        }
    }

    private Object window() {
        return Reflect.has(minecraft, "getWindow|getMainWindow", 0)
                ? Reflect.call(minecraft, "getWindow|getMainWindow") : Reflect.get(minecraft, "window", "mainWindow");
    }

    private long handle() {
        return ((Number) Reflect.call(Reflect.type("dev.itemnamecopy.client.MinecraftAccess"), "windowHandle")).longValue();
    }

    private Object literal(String value) {
        Class<?> component = Reflect.type("net.minecraft.network.chat.Component", "net.minecraft.util.text.ITextComponent");
        if (Reflect.has(component, "literal", 1)) return Reflect.call(component, "literal", value);
        return Reflect.make(Reflect.type(
                "net.minecraft.network.chat.TextComponent", "net.minecraft.util.text.StringTextComponent"), value);
    }

    private void select(Object textInput, String value) {
        setText(textInput, value);
        Reflect.call(textInput, "setFocused", true);
        Reflect.call(textInput, "setCursorPosition", 0);
        Reflect.call(textInput, "setHighlightPos|setSelectionPos", value.length());
    }

    private void collectWidgets(Object owner, List<Object> found, Set<Object> seen, int depth) {
        if (owner == null || !seen.add(owner) || depth > 8) return;
        found.add(owner);
        Object children = Reflect.has(owner, "children|getEventListeners", 0)
                ? Reflect.call(owner, "children|getEventListeners") : Reflect.optionalGet(owner, "buttonList");
        if (children instanceof Iterable) {
            for (Object child : (Iterable<?>) children) collectWidgets(child, found, seen, depth + 1);
        }
    }

    private boolean isTextInput(Object value) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getSimpleName();
            if (name.equals("EditBox") || name.equals("TextFieldWidget") || name.equals("GuiTextField")) return true;
        }
        return false;
    }

    private boolean isButton(Object widget) {
        for (Class<?> type = widget.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().contains("Button")) return true;
        }
        return false;
    }
}
