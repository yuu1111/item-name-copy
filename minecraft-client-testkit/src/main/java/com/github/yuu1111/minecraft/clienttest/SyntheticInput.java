package com.github.yuu1111.minecraft.clienttest;

public final class SyntheticInput {
    private int modifiers = -1;
    private int action;
    private int eventKey = 46;
    private int eventCharacter = 'c';
    private int pointerX = -1;
    private int pointerY = -1;

    public int controlState() {
        return modifiers < 0 ? -1 : (modifiers & 2) == 0 ? 0 : 1;
    }

    public int shiftState() {
        return modifiers < 0 ? -1 : (modifiers & 1) == 0 ? 0 : 1;
    }

    public int altState() {
        return modifiers < 0 ? -1 : (modifiers & 4) == 0 ? 0 : 1;
    }

    public int eventKey() {
        return modifiers < 0 ? -1 : eventKey;
    }

    public int eventCharacter() {
        return modifiers < 0 ? -1 : eventCharacter;
    }

    public int eventKeyState() {
        return modifiers < 0 ? -1 : action == 0 ? 0 : 1;
    }

    public int repeatState() {
        return modifiers < 0 ? -1 : action == 2 ? 1 : 0;
    }

    public int mouseX() {
        return pointerX;
    }

    public int mouseY() {
        return pointerY;
    }

    public int keyDown(int key) {
        if (modifiers < 0) return -1;
        if (key == 29 || key == 157) return controlState();
        if (key == 42 || key == 54) return (modifiers & 1) == 0 ? 0 : 1;
        if (key == 56 || key == 184) return (modifiers & 4) == 0 ? 0 : 1;
        if (key == 219 || key == 220) return (modifiers & 8) == 0 ? 0 : 1;
        return key == eventKey && action != 0 ? 1 : 0;
    }

    public int glfwKeyDown(int key) {
        if (modifiers < 0) return -1;
        if (key == 341 || key == 345) return controlState();
        if (key == 340 || key == 344) return shiftState();
        if (key == 342 || key == 346) return altState();
        if (key == 343 || key == 347) return (modifiers & 8) == 0 ? 0 : 1;
        int glfwEventKey = eventKey == 46 ? 67 : eventKey == 37 ? 75 : -1;
        return key == glfwEventKey && action != 0 ? 1 : 0;
    }

    public void beginKeyEvent(int action, int modifiers) {
        beginKeyEvent(46, 'c', action, modifiers);
    }

    public void beginKeyEvent(int eventKey, int eventCharacter, int action, int modifiers) {
        this.eventKey = eventKey;
        this.eventCharacter = eventCharacter;
        this.action = action;
        this.modifiers = modifiers;
    }

    public void endKeyEvent() {
        modifiers = -1;
    }

    public void movePointer(int x, int y) {
        pointerX = x;
        pointerY = y;
    }

    public void reset() {
        pointerX = -1;
        pointerY = -1;
        modifiers = -1;
    }
}
