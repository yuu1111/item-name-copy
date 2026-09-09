package com.github.yuu1111.itemnamecopy.core;

import java.util.Optional;

public final class CopyShortcutHandler {
    private boolean keyDown;

    public void releaseKey() {
        keyDown = false;
    }

    public void reset() {
        keyDown = false;
    }

    public boolean pressKey(boolean repeat, boolean alreadyHandled, Target target) {
        boolean wasDown = keyDown;
        keyDown = true;
        if (wasDown || repeat || alreadyHandled || target == null || target.isTextInputFocused()) {
            return false;
        }

        Optional<String> name = target.hoveredItemName();
        if (!name.isPresent() || !target.writeClipboard(name.get())) {
            return false;
        }

        target.showFeedback(name.get());
        return true;
    }

    public interface Target {
        boolean isTextInputFocused();

        Optional<String> hoveredItemName();

        boolean writeClipboard(String name);

        void showFeedback(String name);
    }
}
