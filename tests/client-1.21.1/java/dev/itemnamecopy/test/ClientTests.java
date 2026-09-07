package dev.itemnamecopy.test;

import com.google.gson.GsonBuilder;
import dev.itemnamecopy.client.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ClientTests {
    public static Integer modifiers;
    private static final Minecraft MC = Minecraft.getInstance();
    private static final List<Map<String, String>> RESULTS = new ArrayList<>();
    private static final Path REPORT = Path.of(System.getProperty("itemnamecopy.testReport"));
    private static int stage;
    private static int delay;
    private static volatile boolean finished;
    private static String originalClipboard;
    private static CompletableFuture<?> preparation;

    private ClientTests() {}

    public static void tick() {
        if (finished) return;
        try {
            if (stage == 0) {
                if (!(MC.screen instanceof TitleScreen) || MC.getOverlay() != null) return;
                originalClipboard = MC.keyboardHandler.getClipboard();
                Thread watchdog = new Thread(() -> {
                    try { Thread.sleep(180_000); } catch (InterruptedException ignored) { return; }
                    if (!finished) {
                        System.err.println("Client test timed out after 180 seconds");
                        Runtime.getRuntime().halt(124);
                    }
                }, "client-test-timeout");
                watchdog.setDaemon(true);
                watchdog.start();
                MC.options.pauseOnLostFocus = false;
                MC.options.languageCode = "ja_jp";
                MC.getLanguageManager().setSelected("ja_jp");
                preparation = MC.reloadResourcePacks();
                stage = 1;
            } else if (stage == 1 && preparation.isDone()) {
                preparation.join();
                stage = 2;
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                MC.createWorldOpenFlows().createFreshLevel("client-test-" + UUID.randomUUID(),
                    new LevelSettings("ItemNameCopy Automated Test", GameType.SURVIVAL, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            } else if (stage == 2 && MC.player != null && MC.level != null && MC.screen == null) {
                stage = 3;
                delay = 20;
            } else if (stage == 3 && delay-- <= 0) {
                runSurvivalTests();
                stage = 4;
                preparation = MC.getSingleplayerServer().submit(() -> {
                    MC.getSingleplayerServer().getPlayerList().getPlayer(MC.player.getUUID()).setGameMode(GameType.CREATIVE);
                });
            } else if (stage == 4 && preparation.isDone() && MC.gameMode.hasInfiniteItems()) {
                preparation.join();
                runCreativeTests();
                finish();
            }
        } catch (Throwable error) {
            record("harness", error);
            finish();
        }
    }

    private static void runSurvivalTests() {
        check("world-settings", () -> {
            equal(Difficulty.PEACEFUL, MC.level.getDifficulty());
            require(MC.getSingleplayerServer().getWorldData().isFlatWorld(), "World is not flat");
            require(MC.getSingleplayerServer().getWorldData().isAllowCommands(), "Commands are disabled");
        });
        check("survival-copy-and-item-unchanged", () -> {
            seedClipboard("survival-sentinel");
            InventoryScreen screen = inventory(new ItemStack(Items.OAK_LOG, 3));
            ItemStack before = MC.player.getInventory().getItem(0).copy();
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            equal("オークの原木", MC.keyboardHandler.getClipboard());
            require(ItemStack.matches(before, MC.player.getInventory().getItem(0)), "Copy changed the item");
        });
        check("localized-success-notification", () -> {
            MC.gui.setOverlayMessage(Component.literal("notification-sentinel"), false);
            InventoryScreen screen = inventory(new ItemStack(Items.OAK_LOG));
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            Component message = field(MC.gui, "overlayMessageString");
            equal("コピーしました: オークの原木", message == null ? null : message.getString());
        });
        check("empty-slot-preserves-clipboard", () -> {
            InventoryScreen screen = inventory(ItemStack.EMPTY);
            seedClipboard("empty-slot-sentinel");
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            equal("empty-slot-sentinel", MC.keyboardHandler.getClipboard());
        });
        check("custom-name-exact-text", () -> {
            seedClipboard("custom-name-sentinel");
            ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
            String name = "  名付けた剣 ✨  ";
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            InventoryScreen screen = inventory(stack);
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            equal(name, MC.keyboardHandler.getClipboard());
        });
        check("held-key-does-not-repeat", () -> {
            seedClipboard("held-key-sentinel");
            InventoryScreen screen = inventory(new ItemStack(Items.OAK_LOG));
            hover(screen, screen.getMenu().slots.get(36));
            key(GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
            equal("オークの原木", MC.keyboardHandler.getClipboard());
            seedClipboard("repeat-sentinel");
            key(GLFW.GLFW_REPEAT, GLFW.GLFW_MOD_CONTROL);
            equal("repeat-sentinel", MC.keyboardHandler.getClipboard());
            key(GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_CONTROL);
            copy();
            equal("オークの原木", MC.keyboardHandler.getClipboard());
        });
        check("other-modifiers-do-not-copy", () -> {
            InventoryScreen screen = inventory(new ItemStack(Items.OAK_LOG));
            hover(screen, screen.getMenu().slots.get(36));
            for (int modifier : new int[] {0, GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_MOD_ALT, GLFW.GLFW_MOD_SUPER}) {
                seedClipboard("modifier-sentinel");
                int flags = modifier == 0 ? 0 : GLFW.GLFW_MOD_CONTROL | modifier;
                key(GLFW.GLFW_PRESS, flags);
                key(GLFW.GLFW_RELEASE, flags);
                equal("modifier-sentinel", MC.keyboardHandler.getClipboard());
            }
        });
        check("recipe-search-priority-and-return-to-copy", () -> {
            seedClipboard("recipe-search-sentinel");
            InventoryScreen screen = inventory(new ItemStack(Items.OAK_LOG));
            var recipe = screen.getRecipeBookComponent();
            if (!recipe.isVisible()) recipe.toggleVisibility();
            EditBox search = field(recipe, "searchBox");
            search.setValue("recipe-copy-test");
            search.setFocused(true);
            search.setCursorPosition(0);
            search.setHighlightPos(search.getValue().length());
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            equal("recipe-copy-test", MC.keyboardHandler.getClipboard());
            recipe.toggleVisibility();
            hover(screen, screen.getMenu().slots.get(36));
            copy();
            equal("オークの原木", MC.keyboardHandler.getClipboard());
        });
        MC.setScreen(null);
    }

    private static void runCreativeTests() {
        check("creative-copy", () -> {
            seedClipboard("creative-sentinel");
            var screen = new CreativeModeInventoryScreen(MC.player, MC.level.enabledFeatures(), true);
            MC.setScreen(screen);
            Slot slot = screen.getMenu().slots.stream().filter(Slot::hasItem).findFirst().orElseThrow();
            String expected = slot.getItem().getHoverName().getString();
            hover(screen, slot);
            copy();
            equal(expected, MC.keyboardHandler.getClipboard());
        });
        check("creative-search-priority", () -> {
            seedClipboard("creative-search-sentinel");
            var screen = new CreativeModeInventoryScreen(MC.player, MC.level.enabledFeatures(), true);
            MC.setScreen(screen);
            try {
                var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, CreativeModeTabs.searchTab());
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            EditBox search = field(screen, "searchBox");
            search.setValue("トウヒ");
            search.setFocused(true);
            search.setCursorPosition(0);
            search.setHighlightPos(search.getValue().length());
            Slot slot = screen.getMenu().slots.stream().filter(Slot::hasItem).findFirst().orElseThrow();
            hover(screen, slot);
            copy();
            equal("トウヒ", MC.keyboardHandler.getClipboard());
        });
    }

    private static InventoryScreen inventory(ItemStack stack) {
        MC.setScreen(null);
        MC.player.getInventory().setItem(0, stack);
        InventoryScreen screen = new InventoryScreen(MC.player);
        MC.setScreen(screen);
        if (screen.getRecipeBookComponent().isVisible()) screen.getRecipeBookComponent().toggleVisibility();
        return screen;
    }

    private static void hover(AbstractContainerScreen<?> screen, Slot slot) {
        int left = field(screen, "leftPos");
        int top = field(screen, "topPos");
        GuiGraphics graphics = new GuiGraphics(MC, MC.renderBuffers().bufferSource());
        screen.render(graphics, left + slot.x + 8, top + slot.y + 8, 0);
        graphics.flush();
        require(MinecraftAccess.hoveredSlot(screen) == slot, "Rendering did not hover the expected slot");
    }

    private static void copy() {
        key(GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
        key(GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_CONTROL);
    }

    private static void key(int action, int flags) {
        settleClipboard();
        modifiers = flags;
        try {
            MC.keyboardHandler.keyPress(MC.getWindow().getWindow(), GLFW.GLFW_KEY_C, 0, action, flags);
        } finally {
            modifiers = null;
        }
        settleClipboard();
    }

    private static void seedClipboard(String value) {
        settleClipboard();
        MC.keyboardHandler.setClipboard(value);
        settleClipboard();
        equal(value, MC.keyboardHandler.getClipboard());
    }

    private static void settleClipboard() {
        try { Thread.sleep(150); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the clipboard", error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object object, String name) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(object);
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
        throw new AssertionError("Field missing: " + name);
    }

    private static void check(String name, Runnable test) {
        try {
            test.run();
            RESULTS.add(Map.of("name", name, "status", "passed"));
            System.out.println("CLIENT_TEST PASS " + name);
        } catch (Throwable error) { record(name, error); }
    }

    private static void record(String name, Throwable error) {
        RESULTS.add(Map.of("name", name, "status", "failed", "message", error.toString()));
        System.err.println("CLIENT_TEST FAIL " + name + ": " + error);
        error.printStackTrace();
    }

    private static void equal(Object expected, Object actual) {
        require(java.util.Objects.equals(expected, actual), "Expected <" + expected + "> but was <" + actual + ">");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void finish() {
        if (finished) return;
        finished = true;
        try {
            long failed = RESULTS.stream().filter(result -> result.get("status").equals("failed")).count();
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("target", "1.21.1-fabric");
            report.put("mode", "in-process client integration; synthetic keyboard callbacks; real clipboard and rendering");
            report.put("passed", RESULTS.size() - failed);
            report.put("failed", failed);
            report.put("tests", RESULTS);
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuite name=\"client-1.21.1-fabric\" tests=\"" + RESULTS.size() + "\" failures=\"" + failed + "\">");
            for (var result : RESULTS) {
                xml.append("<testcase classname=\"ClientTests\" name=\"").append(result.get("name")).append("\">");
                if (result.get("status").equals("failed")) xml.append("<failure message=\"").append(escape(result.get("message"))).append("\"/>");
                xml.append("</testcase>");
            }
            Files.writeString(REPORT.resolveSibling("TEST-client.xml"), xml.append("</testsuite>").toString());
            if (originalClipboard != null) MinecraftAccess.writeClipboard(new com.mojang.blaze3d.platform.ClipboardManager(), originalClipboard);
        } catch (Exception error) {
            error.printStackTrace();
        } finally {
            MC.stop();
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
