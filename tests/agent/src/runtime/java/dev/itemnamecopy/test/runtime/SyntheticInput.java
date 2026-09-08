package dev.itemnamecopy.test.runtime;

final class SyntheticInput {
    private int modifiers = -1;
    private int action;
    private int pointerX = -1;
    private int pointerY = -1;

    int controlState() {
        return modifiers < 0 ? -1 : (modifiers & 2) == 0 ? 0 : 1;
    }

    int eventKey() {
        return modifiers < 0 ? -1 : 46;
    }

    int eventCharacter() {
        return modifiers < 0 ? -1 : 'c';
    }

    int eventKeyState() {
        return modifiers < 0 ? -1 : action == 0 ? 0 : 1;
    }

    int repeatState() {
        return modifiers < 0 ? -1 : action == 2 ? 1 : 0;
    }

    int mouseX() {
        return pointerX;
    }

    int mouseY() {
        return pointerY;
    }

    int keyDown(int key) {
        if (modifiers < 0) return -1;
        if (key == 29 || key == 157) return controlState();
        if (key == 42 || key == 54) return (modifiers & 1) == 0 ? 0 : 1;
        if (key == 56 || key == 184) return (modifiers & 4) == 0 ? 0 : 1;
        if (key == 219 || key == 220) return (modifiers & 8) == 0 ? 0 : 1;
        return key == 46 && action != 0 ? 1 : 0;
    }

    void beginKeyEvent(int action, int modifiers) {
        this.action = action;
        this.modifiers = modifiers;
    }

    void endKeyEvent() {
        modifiers = -1;
    }

    void movePointer(int x, int y) {
        pointerX = x;
        pointerY = y;
    }

    void reset() {
        pointerX = -1;
        pointerY = -1;
        modifiers = -1;
    }
}
