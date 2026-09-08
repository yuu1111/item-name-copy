package com.github.yuu1111.itemnamecopy.core;

import java.util.Optional;

public final class CopyShortcutHandler {
    private boolean cDown;

    public void releaseC() {
        cDown = false;
    }

    public void reset() {
        cDown = false;
    }

    public boolean pressC(boolean control, boolean otherModifier, boolean repeat,
                          boolean alreadyHandled, Target target) {
        boolean wasDown = cDown;
        cDown = true;
        if (wasDown || repeat || !control || otherModifier || alreadyHandled
                || target == null || target.isTextInputFocused()) {
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
