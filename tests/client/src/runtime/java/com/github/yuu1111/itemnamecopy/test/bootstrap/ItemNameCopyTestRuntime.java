package com.github.yuu1111.itemnamecopy.test.bootstrap;

import com.github.yuu1111.itemnamecopy.test.support.ItemNameCopyClientDriver;
import com.github.yuu1111.itemnamecopy.test.support.ItemNameCopyTestLifecycle;
import com.github.yuu1111.itemnamecopy.test.tests.ItemNameCopyTestSuite;
import com.github.yuu1111.minecraft.clienttest.ClientTestOptions;
import com.github.yuu1111.minecraft.clienttest.ClientTestRunner;
import com.github.yuu1111.minecraft.clienttest.SyntheticInput;

/**
 * 計装されたMinecraftとLWJGLから呼び出すItemNameCopyテストの入口
 */
public final class ItemNameCopyTestRuntime {
    private static final ClientTestOptions OPTIONS = ClientTestOptions.fromSystemProperties(
        "itemnamecopy.test",
        "real client, synthetic input callbacks, OS clipboard");
    private static final SyntheticInput INPUT = new SyntheticInput();
    private static final ItemNameCopyClientDriver CLIENT = new ItemNameCopyClientDriver(INPUT, OPTIONS);
    private static final ClientTestRunner RUNNER = new ClientTestRunner(
        INPUT,
        new ItemNameCopyTestLifecycle(CLIENT, OPTIONS),
        new ItemNameCopyTestSuite(CLIENT),
        OPTIONS);

    private ItemNameCopyTestRuntime() {
    }

    public static int controlState() {
        return INPUT.controlState();
    }

    public static int eventKey() {
        return INPUT.eventKey();
    }

    public static int eventCharacter() {
        return INPUT.eventCharacter();
    }

    public static int eventKeyState() {
        return INPUT.eventKeyState();
    }

    public static int repeatState() {
        return INPUT.repeatState();
    }

    public static int mouseX() {
        return INPUT.mouseX();
    }

    public static int mouseY() {
        return INPUT.mouseY();
    }

    public static int keyDown(int key) {
        return INPUT.keyDown(key);
    }

    public static void tick(Object client) {
        RUNNER.tick(client);
    }
}
