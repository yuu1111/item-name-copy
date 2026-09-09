package com.github.yuu1111.itemnamecopy.core;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CopyShortcutHandlerTest {
    private final CopyShortcutHandler handler = new CopyShortcutHandler();
    private final FakeTarget target = new FakeTarget();

    @ParameterizedTest
    @ValueSource(strings = {"Stone", "石", "  名付けた剣 ✨  ", "first\nsecond", ""})
    void preservesTheExactDisplayName(String name) {
        target.name = Optional.of(name);
        assertTrue(press());
        assertEquals(name, target.clipboard);
        assertEquals(name, target.feedback);
        assertEquals(1, target.writes);
        assertEquals(1, target.notifications);
    }

    @Test
    void leavesTextInputAndConsumedEventsAlone() {
        target.textInput = true;
        assertFalse(press());
        handler.releaseKey();
        target.textInput = false;
        assertFalse(handler.pressKey(false, true, target));
        assertEquals(0, target.writes);
    }

    @Test
    void missingSlotDoesNotChangeTheClipboard() {
        target.name = Optional.empty();
        assertFalse(press());
        assertEquals("existing", target.clipboard);
        assertEquals(0, target.notifications);
    }

    @Test
    void failedWriteDoesNotConsumeTheEventOrNotify() {
        target.writeSucceeds = false;
        assertFalse(press());
        assertEquals("existing", target.clipboard);
        assertEquals(0, target.notifications);
    }

    @Test
    void heldKeyCannotCopyAgainUntilReleased() {
        assertTrue(press());
        assertFalse(press());
        assertFalse(handler.pressKey(true, false, target));
        assertEquals(1, target.writes);
        handler.releaseKey();
        assertTrue(press());
        assertEquals(2, target.writes);
    }

    @Test
    void repeatCannotBecomeFirstPressAfterReset() {
        handler.reset();
        assertFalse(handler.pressKey(true, false, target));
        assertEquals(0, target.writes);
    }

    @Test
    void movingOntoAnItemWhileHoldingCDoesNotCopy() {
        target.name = Optional.empty();
        assertFalse(press());
        target.name = Optional.of("Stone");
        assertFalse(press());
        assertEquals(0, target.writes);
    }

    private boolean press() {
        return handler.pressKey(false, false, target);
    }

    private static final class FakeTarget implements CopyShortcutHandler.Target {
        Optional<String> name = Optional.of("Stone");
        String clipboard = "existing";
        String feedback;
        boolean textInput;
        boolean writeSucceeds = true;
        int writes;
        int notifications;

        public boolean isTextInputFocused() { return textInput; }
        public Optional<String> hoveredItemName() { return name; }
        public boolean writeClipboard(String value) {
            writes++;
            if (!writeSucceeds) return false;
            clipboard = value;
            return true;
        }
        public void showFeedback(String value) {
            feedback = value;
            notifications++;
        }
    }
}
