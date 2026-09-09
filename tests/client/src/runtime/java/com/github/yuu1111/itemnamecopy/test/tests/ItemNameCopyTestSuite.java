package com.github.yuu1111.itemnamecopy.test.tests;

import com.github.yuu1111.itemnamecopy.test.support.ItemNameCopyClientDriver;
import com.github.yuu1111.minecraft.clienttest.Pending;
import com.github.yuu1111.minecraft.clienttest.Reflect;
import com.github.yuu1111.minecraft.clienttest.TestAssertions;
import com.github.yuu1111.minecraft.clienttest.TestCase;
import com.github.yuu1111.minecraft.clienttest.TestStep;
import com.github.yuu1111.minecraft.clienttest.TestSuite;

import java.util.ArrayList;
import java.util.List;

public final class ItemNameCopyTestSuite implements TestSuite {
    private final ItemNameCopyClientDriver client;
    private final List<TestCase> tests = new ArrayList<TestCase>();
    private Object itemBeforeCopy;
    private String expectedClipboard;

    public ItemNameCopyTestSuite(ItemNameCopyClientDriver client) {
        this.client = client;
    }

    @Override
    public List<TestCase> defineTests() {
        worldSettings();
        survivalCopy();
        notificationsAndEdgeCases();
        configurableKey();
        recipeSearch();
        creativeInventory();
        recipeViewer();
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
            if (Reflect.has(server, "getWorldData|func_240793_aU_", 0)) {
                data = Reflect.call(server, "getWorldData|func_240793_aU_");
            } else {
                Object serverLevel;
                if (client.isLegacy()) {
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
            } else if (Reflect.has(data, "worldGenSettings|getDimensionGeneratorSettings", 0)) {
                TestAssertions.require((Boolean) Reflect.call(
                        Reflect.call(data, "worldGenSettings|getDimensionGeneratorSettings"), "isFlatWorld|func_236228_i_"),
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
                () -> client.openInventoryWithRegularItem(), () -> client.seedClipboard("survival-sentinel"),
                () -> {
                    client.hoverSlot(36);
                    itemBeforeCopy = Reflect.call(client.itemInSlot(client.testSlot()), "copy");
                },
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> {
                    TestAssertions.equal("オークの原木", client.clipboardText());
                    TestAssertions.equal(String.valueOf(itemBeforeCopy), String.valueOf(client.itemInSlot(client.testSlot())));
                });
    }

    private void notificationsAndEdgeCases() {
        test("localized-success-notification",
                () -> client.openInventoryWithRegularItem(), () -> client.seedClipboard("notification-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("コピーしました: オークの原木", client.overlayText()));
        test("empty-slot-preserves-clipboard",
                () -> client.openInventoryWithRegularItem(), () -> client.seedClipboard("empty-sentinel"), () -> client.hoverSlot(37),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("empty-sentinel", client.clipboardText()));
        test("custom-name-exact-text",
                () -> client.openInventoryWithCustomNamedItem(), () -> client.seedClipboard("custom-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("  名付けた剣 ✨  ", client.clipboardText()));
        test("held-key-does-not-repeat",
                () -> client.openInventoryWithRegularItem(), () -> client.seedClipboard("held-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 2), () -> TestAssertions.equal("オークの原木", client.clipboardText()),
                () -> client.seedClipboard("repeat-sentinel"), () -> client.sendCopyKeyEvent(2, 2),
                () -> TestAssertions.equal("repeat-sentinel", client.clipboardText()),
                () -> client.sendCopyKeyEvent(0, 2), () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("オークの原木", client.clipboardText()));
    }

    private void configurableKey() {
        test("default-copy-shortcut-requires-control",
                () -> client.openInventoryWithRegularItem(), client::verifyCopyKeyRegistration,
                () -> client.seedClipboard("default-shortcut-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 0), () -> client.sendCopyKeyEvent(0, 0),
                () -> TestAssertions.equal("default-shortcut-sentinel", client.clipboardText()),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("オークの原木", client.clipboardText()));
        test("copy-shortcut-without-modifier",
                () -> client.openInventoryWithRegularItem(), client::verifyCopyKeyRegistration,
                () -> client.setCopyShortcut(true, 0), () -> client.seedClipboard("rebind-sentinel"),
                () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 0), () -> client.sendCopyKeyEvent(0, 0),
                () -> TestAssertions.equal("rebind-sentinel", client.clipboardText()),
                () -> client.sendAlternateCopyKeyEvent(1, 0),
                () -> client.sendAlternateCopyKeyEvent(0, 0),
                () -> TestAssertions.equal("オークの原木", client.clipboardText()),
                client::restoreDefaultCopyShortcut);
        test("copy-shortcut-with-alternate-modifier",
                () -> client.openInventoryWithRegularItem(), () -> client.setCopyShortcut(true, 1),
                () -> client.seedClipboard("shift-shortcut-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendAlternateCopyKeyEvent(1, 0),
                () -> client.sendAlternateCopyKeyEvent(0, 0),
                () -> TestAssertions.equal("shift-shortcut-sentinel", client.clipboardText()),
                () -> client.sendAlternateCopyKeyEvent(1, 1),
                () -> client.sendAlternateCopyKeyEvent(0, 1),
                () -> TestAssertions.equal("オークの原木", client.clipboardText()),
                client::restoreDefaultCopyShortcut);
    }

    private void recipeSearch() {
        test("recipe-search-priority-and-return-to-copy",
                () -> client.openInventoryWithRegularItem(), () -> client.selectRecipeSearch("recipe-copy-test"),
                () -> client.seedClipboard("recipe-sentinel"), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("recipe-copy-test", client.clipboardText()),
                () -> client.toggleRecipe(), () -> client.hoverSlot(36),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("オークの原木", client.clipboardText()));
    }

    private void creativeInventory() {
        test("creative-copy",
                () -> {
                    client.showScreen(null);
                    client.sendCommand("gamemode creative");
                },
                () -> {
                    if (!client.isCreative()) throw new Pending();
                    client.openCreativeInventory();
                },
                () -> client.seedClipboard("creative-sentinel"),
                () -> {
                    client.hoverSlot(client.firstOccupiedSlot());
                    expectedClipboard = client.text(Reflect.call(client.itemInSlot(client.testSlot()), "getHoverName|getDisplayName"));
                },
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal(expectedClipboard, client.clipboardText()));
        test("creative-search-priority",
                () -> client.openCreativeInventory(), () -> client.selectCreativeSearch("トウヒ"),
                () -> client.seedClipboard("creative-search-sentinel"),
                () -> client.hoverSlot(client.firstOccupiedSlot()),
                () -> client.sendCopyKeyEvent(1, 2), () -> client.sendCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("トウヒ", client.clipboardText()));
    }

    private void recipeViewer() {
        String viewer = System.getProperty("itemnamecopy.test.recipeViewer", "");
        if (viewer.isEmpty()) return;

        test(viewer + "-pseudo-slot-copy",
                client::openRecipeViewerAndHoverItem,
                () -> client.seedClipboard(viewer + "-pseudo-slot-sentinel"),
                () -> client.sendRecipeViewerCopyKeyEvent(1, 2),
                () -> client.sendRecipeViewerCopyKeyEvent(0, 2),
                () -> TestAssertions.equal(client.recipeViewerItemName(), client.clipboardText()));
        test(viewer + "-search-priority-and-return-to-pseudo-slot",
                client::openRecipeViewerAndHoverItem,
                () -> client.selectRecipeViewerSearch("recipe-viewer-copy-test"),
                () -> client.seedClipboard(viewer + "-search-sentinel"),
                () -> client.sendRecipeViewerCopyKeyEvent(1, 2),
                () -> client.sendRecipeViewerCopyKeyEvent(0, 2),
                () -> TestAssertions.equal("recipe-viewer-copy-test", client.clipboardText()),
                client::unfocusRecipeViewerSearch,
                () -> client.sendRecipeViewerCopyKeyEvent(1, 2),
                () -> client.sendRecipeViewerCopyKeyEvent(0, 2),
                () -> TestAssertions.equal(client.recipeViewerItemName(), client.clipboardText()));
    }

    private void test(String name, TestStep... steps) {
        tests.add(new TestCase(name, steps));
    }
}
