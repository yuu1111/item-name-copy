package dev.itemnamecopy.test.tests;

import dev.itemnamecopy.test.runtime.Pending;
import dev.itemnamecopy.test.runtime.Reflect;
import dev.itemnamecopy.test.runtime.RuntimeConfig;
import dev.itemnamecopy.test.runtime.TestAssertions;
import dev.itemnamecopy.test.runtime.TestCase;
import dev.itemnamecopy.test.runtime.TestStep;
import dev.itemnamecopy.test.runtime.TestSuite;
import dev.itemnamecopy.test.support.MinecraftClientDriver;

import java.util.ArrayList;
import java.util.List;

public final class ItemNameCopyTestSuite implements TestSuite {
    private final MinecraftClientDriver client;
    private final List<TestCase> tests = new ArrayList<TestCase>();
    private Object itemBeforeCopy;
    private String expectedClipboard;

    public ItemNameCopyTestSuite(MinecraftClientDriver client) {
        this.client = client;
    }

    @Override
    public List<TestCase> defineTests() {
        worldSettings();
        survivalCopy();
        notificationsAndEdgeCases();
        modifierHandling();
        recipeSearch();
        creativeInventory();
        return tests;
    }

    private void worldSettings() {
        test("world-settings", () -> {
            Object minecraft = client.minecraft();
            Object level = Reflect.get(minecraft, "level", "world");
            TestAssertions.equal("PEACEFUL", String.valueOf(Reflect.call(level, "getDifficulty")));
            Object server = Reflect.call(minecraft, "getSingleplayerServer|getIntegratedServer|integratedServer");
            client.verifyGameRules(server);
            Object data;
            if (Reflect.has(server, "getWorldData", 0)) {
                data = Reflect.call(server, "getWorldData");
            } else {
                Object serverLevel;
                if (RuntimeConfig.LEGACY) {
                    serverLevel = Reflect.call(server, "getWorld", 0);
                } else {
                    Object dimension = Reflect.get(Reflect.type("net.minecraft.world.level.dimension.DimensionType",
                            "net.minecraft.world.dimension.DimensionType"), "OVERWORLD");
                    serverLevel = Reflect.call(server, "getLevel|getWorld", dimension);
                }
                data = Reflect.call(serverLevel, "getLevelData|getWorldInfo");
            }
            Object settings = Reflect.has(data, "getLevelSettings", 0) ? Reflect.call(data, "getLevelSettings") : data;
            boolean cheats = (Boolean) Reflect.call(settings,
                    "allowCommands|isAllowCommands|getAllowCommands|areCommandsAllowed");
            TestAssertions.require(cheats, "Commands are disabled");
            if (Reflect.has(data, "isFlatWorld", 0)) {
                TestAssertions.require((Boolean) Reflect.call(data, "isFlatWorld"), "World is not flat");
            } else if (Reflect.has(data, "worldGenSettings", 0)) {
                TestAssertions.require((Boolean) Reflect.call(Reflect.call(data, "worldGenSettings"), "isFlatWorld"),
                        "World is not flat");
            } else {
                Object worldType = Reflect.call(data, "getGeneratorType|getTerrainType|getGenerator");
                String type = String.valueOf(Reflect.call(worldType, "getName"));
                TestAssertions.require(type.toLowerCase(java.util.Locale.ROOT).contains("flat"),
                        "World is not flat: " + type);
            }
        });
    }

    private void survivalCopy() {
        test("survival-copy-and-item-unchanged",
                () -> client.inventory(false), () -> client.seedClipboard("survival-sentinel"),
                () -> {
                    client.hover(36);
                    itemBeforeCopy = Reflect.call(client.item(client.testSlot()), "copy");
                },
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> {
                    TestAssertions.equal("オークの原木", client.clipboard());
                    TestAssertions.equal(String.valueOf(itemBeforeCopy), String.valueOf(client.item(client.testSlot())));
                });
    }

    private void notificationsAndEdgeCases() {
        test("localized-success-notification",
                () -> client.inventory(false), () -> client.seedClipboard("notification-sentinel"), () -> client.hover(36),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("コピーしました: オークの原木", client.overlayText()));
        test("empty-slot-preserves-clipboard",
                () -> client.inventory(false), () -> client.seedClipboard("empty-sentinel"), () -> client.hover(37),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("empty-sentinel", client.clipboard()));
        test("custom-name-exact-text",
                () -> client.inventory(true), () -> client.seedClipboard("custom-sentinel"), () -> client.hover(36),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("  名付けた剣 ✨  ", client.clipboard()));
        test("held-key-does-not-repeat",
                () -> client.inventory(false), () -> client.seedClipboard("held-sentinel"), () -> client.hover(36),
                () -> client.copy(1, 2), () -> TestAssertions.equal("オークの原木", client.clipboard()),
                () -> client.seedClipboard("repeat-sentinel"), () -> client.copy(2, 2),
                () -> TestAssertions.equal("repeat-sentinel", client.clipboard()),
                () -> client.copy(0, 2), () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("オークの原木", client.clipboard()));
    }

    private void modifierHandling() {
        for (final int flags : new int[]{0, 3, 6, 10}) {
            test("ignored-modifiers-" + flags,
                    () -> client.inventory(false), () -> client.seedClipboard("modifier-sentinel"), () -> client.hover(36),
                    () -> client.copy(1, flags), () -> client.copy(0, flags),
                    () -> TestAssertions.equal("modifier-sentinel", client.clipboard()));
        }
    }

    private void recipeSearch() {
        test("recipe-search-priority-and-return-to-copy",
                () -> client.inventory(false), () -> client.selectRecipeSearch("recipe-copy-test"),
                () -> client.seedClipboard("recipe-sentinel"), () -> client.hover(36),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("recipe-copy-test", client.clipboard()),
                () -> client.toggleRecipe(), () -> client.hover(36),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("オークの原木", client.clipboard()));
    }

    private void creativeInventory() {
        test("creative-copy",
                () -> {
                    client.show(null);
                    client.command("gamemode creative");
                },
                () -> {
                    if (!client.creative()) throw new Pending();
                    client.openCreative();
                },
                () -> client.seedClipboard("creative-sentinel"),
                () -> {
                    client.hover(client.firstOccupiedSlot());
                    expectedClipboard = client.text(Reflect.call(client.item(client.testSlot()), "getHoverName|getDisplayName"));
                },
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal(expectedClipboard, client.clipboard()));
        test("creative-search-priority",
                () -> client.openCreative(), () -> client.selectCreativeSearch(),
                () -> client.seedClipboard("creative-search-sentinel"),
                () -> client.hover(client.firstOccupiedSlot()),
                () -> client.copy(1, 2), () -> client.copy(0, 2),
                () -> TestAssertions.equal("トウヒ", client.clipboard()));
    }

    private void test(String name, TestStep... steps) {
        tests.add(new TestCase(name, steps));
    }
}
