package dev.itemnamecopy.test.runtime;

public final class SyntheticInput {
    private int modifiers = -1;
    private int action;
    private int pointerX = -1;
    private int pointerY = -1;

    public int controlState() {
        return modifiers < 0 ? -1 : (modifiers & 2) == 0 ? 0 : 1;
    }

    public int eventKey() {
        return modifiers < 0 ? -1 : 46;
    }

    public int eventCharacter() {
        return modifiers < 0 ? -1 : 'c';
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
        return key == 46 && action != 0 ? 1 : 0;
    }

    public void beginKeyEvent(int action, int modifiers) {
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
