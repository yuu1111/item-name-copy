package dev.itemnamecopy.legacy;

import dev.itemnamecopy.core.CopyShortcutHandler;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@Mod(modid = "itemnamecopy", name = "ItemNameCopy",
        clientSideOnly = true, acceptableRemoteVersions = "*", useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = "itemnamecopy", value = Side.CLIENT)
public final class Forge112Client {
    private static final CopyShortcutHandler HANDLER = new CopyShortcutHandler();
    private static final Logger LOGGER = LogManager.getLogger("itemnamecopy");
    private static final ClassValue<List<Field>> INPUT_FIELDS = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())
                            && (GuiTextField.class.isAssignableFrom(field.getType())
                            || GuiRecipeBook.class.isAssignableFrom(field.getType()))) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
            }
            return fields;
        }
    };

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Keyboard.getEventKey() != Keyboard.KEY_C) return;
        if (!Keyboard.getEventKeyState()) {
            HANDLER.releaseC();
            return;
        }
        if (!Keyboard.isRepeatEvent()) HANDLER.releaseC();
        GuiScreen screen = event.getGui();
        boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean other = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
        if (HANDLER.pressC(control, other, Keyboard.isRepeatEvent(), event.isCanceled(),
                screen instanceof GuiContainer ? new Target((GuiContainer) screen) : null)) {
            event.setCanceled(true);
        }
    }

    private static boolean focusedInput(Object owner) {
        try {
            for (Field field : INPUT_FIELDS.get(owner.getClass())) {
                Object value = field.get(owner);
                if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) return true;
                if (value instanceof GuiRecipeBook && ((GuiRecipeBook) value).isVisible()
                        && focusedRecipeInput((GuiRecipeBook) value)) return true;
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("Could not inspect focused text input", exception);
            return true;
        }
    }

    private static boolean focusedRecipeInput(GuiRecipeBook book) throws IllegalAccessException {
        for (Field field : INPUT_FIELDS.get(book.getClass())) {
            Object value = field.get(book);
            if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) return true;
        }
        return false;
    }

    private static final class Target implements CopyShortcutHandler.Target {
        private final GuiContainer screen;

        private Target(GuiContainer screen) {
            this.screen = screen;
        }

        @Override
        public boolean isTextInputFocused() {
            return focusedInput(screen);
        }

        @Override
        public Optional<String> hoveredItemName() {
            Slot slot = screen.getSlotUnderMouse();
            if (slot == null || !slot.getHasStack()) return Optional.empty();
            return Optional.ofNullable(TextFormatting.getTextWithoutFormattingCodes(slot.getStack().getDisplayName()));
        }

        @Override
        public boolean writeClipboard(String name) {
            try {
                java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(name), null);
                return name.equals(clipboard.getData(DataFlavor.stringFlavor));
            } catch (Exception exception) {
                LOGGER.warn("Could not copy item name to clipboard", exception);
                return false;
            }
        }

        @Override
        public void showFeedback(String name) {
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.sendStatusMessage(
                        new TextComponentTranslation("itemnamecopy.copied", name), true);
            }
        }
    }
}
