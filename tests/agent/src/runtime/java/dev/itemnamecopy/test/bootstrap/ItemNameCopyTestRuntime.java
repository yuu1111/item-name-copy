package dev.itemnamecopy.test.bootstrap;

import dev.itemnamecopy.test.runtime.ClientTestRunner;
import dev.itemnamecopy.test.runtime.SyntheticInput;
import dev.itemnamecopy.test.support.ItemNameCopyClientDriver;
import dev.itemnamecopy.test.support.ItemNameCopyTestLifecycle;
import dev.itemnamecopy.test.tests.ItemNameCopyTestSuite;

/**
 * 計装されたMinecraftとLWJGLから呼び出すItemNameCopyテストの入口
 */
public final class ItemNameCopyTestRuntime {
    private static final SyntheticInput INPUT = new SyntheticInput();
    private static final ItemNameCopyClientDriver CLIENT = new ItemNameCopyClientDriver(INPUT);
    private static final ClientTestRunner RUNNER = new ClientTestRunner(
        INPUT,
        new ItemNameCopyTestLifecycle(CLIENT),
        new ItemNameCopyTestSuite(CLIENT));

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
