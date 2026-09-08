package com.github.yuu1111.itemnamecopy.test.tests;

import com.github.yuu1111.itemnamecopy.test.support.ItemNameCopyClientDriver;
import com.github.yuu1111.minecraft.clienttest.Pending;
import com.github.yuu1111.minecraft.clienttest.Reflect;
import com.github.yuu1111.minecraft.clienttest.TestAssertions;
import com.github.yuu1111.minecraft.clienttest.TestCase;
import com.github.yuu1111.minecraft.clienttest.TestSuite;
import dev.e2e.driver.FileDriver;
import java.util.ArrayList;
import java.util.List;

public final class ExternalInputTestSuite implements TestSuite {
    private final ItemNameCopyClientDriver client;
    private final FileDriver driver = new FileDriver();
    private Object slot;
    private Object itemBefore;
    private long hoverDeadline;

    public ExternalInputTestSuite(ItemNameCopyClientDriver client) {
        this.client = client;
    }

    @Override
    public List<TestCase> defineTests() {
        if (client.isLegacy()) throw new IllegalStateException("External LWJGL 2 input is not qualified yet");
        List<TestCase> tests = new ArrayList<TestCase>();
        for (TestCase test : new ItemNameCopyTestSuite(client).defineTests()) {
            if (test.name().equals("world-settings")) tests.add(test);
        }
        TestAssertions.require(tests.size() == 1, "World settings test is missing");
        tests.add(new TestCase("external-copy-and-item-unchanged",
            () -> client.openInventoryWithRegularItem(),
            () -> client.seedClipboard("external-copy-sentinel"),
            () -> hover(36), this::await,
            () -> send("screenshot", ""), this::await,
            this::verifyHover,
            () -> send("hotkey", ",\"keys\":\"ctrl+c\""), this::await,
            () -> {
                TestAssertions.equal("オークの原木", client.clipboardText());
                TestAssertions.require((Boolean) Reflect.call(itemBefore.getClass(), "matches|areItemStacksEqual",
                    itemBefore, client.itemInSlot(slot)), "The copied item changed");
                verifyReleasedKeys();
            },
            () -> send("clipboard", ",\"expected\":" + FileDriver.quote("オークの原木")), this::await,
            () -> send("screenshot", ""), this::await));
        tests.add(new TestCase("external-empty-slot",
            () -> client.seedClipboard("external-empty-sentinel"),
            () -> hover(9), this::await,
            () -> {
                verifyHover();
                TestAssertions.require((Boolean) Reflect.call(client.itemInSlot(slot), "isEmpty"), "Expected an empty slot");
            },
            () -> send("hotkey", ",\"keys\":\"ctrl+c\""), this::await,
            () -> {
                TestAssertions.equal("external-empty-sentinel", client.clipboardText());
                verifyReleasedKeys();
            },
            () -> send("clipboard", ",\"expected\":" + FileDriver.quote("external-empty-sentinel")), this::await));
        return tests;
    }

    private void hover(int index) {
        hoverDeadline = 0;
        slot = client.slots().get(index);
        itemBefore = Reflect.call(client.itemInSlot(slot), "copy");
        Object screen = client.testScreen();
        int x = ((Number) Reflect.get(screen, "leftPos", "guiLeft")).intValue()
            + ((Number) Reflect.get(slot, "x", "xPos")).intValue() + 8;
        int y = ((Number) Reflect.get(screen, "topPos", "guiTop")).intValue()
            + ((Number) Reflect.get(slot, "y", "yPos")).intValue() + 8;
        Object window = window();
        int width = ((Number) Reflect.call(window, "getScreenWidth|getWidth")).intValue();
        int height = ((Number) Reflect.call(window, "getScreenHeight|getHeight")).intValue();
        x = x * width / ((Number) Reflect.get(screen, "width")).intValue();
        y = y * height / ((Number) Reflect.get(screen, "height")).intValue();
        send("hover", ",\"x\":" + x + ",\"y\":" + y);
    }

    private void verifyHover() {
        TestAssertions.require(client.screen() == client.testScreen(), "The inventory screen changed");
        Object hovered = Reflect.call(Reflect.type("com.github.yuu1111.itemnamecopy.client.MinecraftAccess"),
            "hoveredSlot", client.testScreen());
        if (hovered != slot) {
            if (hoverDeadline == 0) hoverDeadline = System.nanoTime() + 5_000_000_000L;
            TestAssertions.require(System.nanoTime() < hoverDeadline,
                "External pointer did not hover slot " + client.slots().indexOf(slot)
                    + "; observed slot " + client.slots().indexOf(hovered));
            throw new Pending();
        }
    }

    private Object window() {
        return Reflect.call(client.minecraft(), "getWindow|getMainWindow");
    }

    private void verifyReleasedKeys() {
        long handle = ((Number) Reflect.call(window(), "getWindow|getHandle")).longValue();
        Class<?> glfw = Reflect.type("org.lwjgl.glfw.GLFW");
        for (int key : new int[] { 67, 341, 345 }) {
            TestAssertions.equal(0, ((Number) Reflect.call(glfw, "glfwGetKey", handle, key)).intValue());
        }
    }

    private void send(String action, String fields) {
        try {
            driver.submit(action, fields);
        } catch (Exception error) {
            throw new IllegalStateException("Could not submit external input", error);
        }
    }

    private void await() {
        boolean complete;
        try {
            complete = driver.poll();
        } catch (Exception error) {
            throw new IllegalStateException("Could not read external input result", error);
        }
        if (!complete) throw new Pending();
    }
}
