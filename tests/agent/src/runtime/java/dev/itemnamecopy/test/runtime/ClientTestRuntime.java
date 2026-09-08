package dev.itemnamecopy.test.runtime;

/** Entry points invoked by the instrumented Minecraft and LWJGL classes. */
public final class ClientTestRuntime {
    private static final SyntheticInput INPUT = new SyntheticInput();
    private static final ClientTestController CONTROLLER = new ClientTestController(INPUT);

    private ClientTestRuntime() {}

    public static int controlState() { return INPUT.controlState(); }
    public static int eventKey() { return INPUT.eventKey(); }
    public static int eventCharacter() { return INPUT.eventCharacter(); }
    public static int eventKeyState() { return INPUT.eventKeyState(); }
    public static int repeatState() { return INPUT.repeatState(); }
    public static int mouseX() { return INPUT.mouseX(); }
    public static int mouseY() { return INPUT.mouseY(); }
    public static int keyDown(int key) { return INPUT.keyDown(key); }
    public static void tick(Object client) { CONTROLLER.tick(client); }
}
