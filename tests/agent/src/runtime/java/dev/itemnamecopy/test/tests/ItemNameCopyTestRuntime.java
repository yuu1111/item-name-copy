package dev.itemnamecopy.test.tests;

import dev.itemnamecopy.test.runtime.ClientTestRunner;
import dev.itemnamecopy.test.runtime.SyntheticInput;
import dev.itemnamecopy.test.support.MinecraftClientDriver;
import dev.itemnamecopy.test.support.MinecraftTestLifecycle;

/**
 * Entry points invoked by the instrumented Minecraft and LWJGL classes.
 */
public final class ItemNameCopyTestRuntime {
    private static final SyntheticInput INPUT = new SyntheticInput();
    private static final MinecraftClientDriver CLIENT = new MinecraftClientDriver(INPUT);
    private static final ClientTestRunner RUNNER = new ClientTestRunner(
        INPUT,
        new MinecraftTestLifecycle(CLIENT),
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
