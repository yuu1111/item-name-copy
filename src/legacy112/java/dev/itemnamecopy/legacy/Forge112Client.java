package dev.itemnamecopy.legacy;

import dev.itemnamecopy.core.CopyShortcutHandler;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
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

import javax.annotation.Nonnull;

@Mod(
        modid = Forge112Client.MOD_ID,
        name = "ItemNameCopy",
        clientSideOnly = true,
        acceptableRemoteVersions = "*",
        useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]"
)
@Mod.EventBusSubscriber(modid = Forge112Client.MOD_ID, value = Side.CLIENT)
public final class Forge112Client {
    public static final String MOD_ID = "itemnamecopy";

    private static final CopyShortcutHandler HANDLER = new CopyShortcutHandler();
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final ClassValue<List<Field>> FOCUS_FIELDS = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(@Nonnull Class<?> type) {
            return findFocusFields(type);
        }
    };

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Keyboard.getEventKey() != Keyboard.KEY_C) return;

        if (!Keyboard.getEventKeyState()) {
            HANDLER.releaseC();
            return;
        }

        boolean repeat = Keyboard.isRepeatEvent();
        if (!repeat) HANDLER.releaseC();

        if (HANDLER.pressC(
                isControlDown(),
                isOtherModifierDown(),
                repeat,
                event.isCanceled(),
                targetFor(event.getGui())
        )) {
            event.setCanceled(true);
        }
    }

    private static boolean isControlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private static boolean isOtherModifierDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_RMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA)
                || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
    }

    private static CopyShortcutHandler.Target targetFor(GuiScreen screen) {
        if (!(screen instanceof GuiContainer)) return null;
        return new ContainerTarget((GuiContainer) screen);
    }

    private static boolean hasFocusedTextInput(Object owner) {
        try {
            for (Field field : FOCUS_FIELDS.get(owner.getClass())) {
                Object value = field.get(owner);
                if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) return true;
                if (value instanceof GuiRecipeBook
                        && ((GuiRecipeBook) value).isVisible()
                        && hasFocusedTextField(value)) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("Could not inspect focused text input", exception);
            return true;
        }
    }

    private static boolean hasFocusedTextField(Object owner) throws IllegalAccessException {
        for (Field field : FOCUS_FIELDS.get(owner.getClass())) {
            Object value = field.get(owner);
            if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) return true;
        }
        return false;
    }

    private static List<Field> findFocusFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (isFocusField(field)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private static boolean isFocusField(Field field) {
        if (Modifier.isStatic(field.getModifiers())) return false;
        return GuiTextField.class.isAssignableFrom(field.getType())
                || GuiRecipeBook.class.isAssignableFrom(field.getType());
    }

    private static final class ContainerTarget implements CopyShortcutHandler.Target {
        private final GuiContainer screen;

        private ContainerTarget(GuiContainer screen) {
            this.screen = screen;
        }

        @Override
        public boolean isTextInputFocused() {
            return hasFocusedTextInput(screen);
        }

        @Override
        public Optional<String> hoveredItemName() {
            Slot slot = screen.getSlotUnderMouse();
            if (slot == null || !slot.getHasStack()) return Optional.empty();

            String displayName = slot.getStack().getDisplayName();
            return Optional.ofNullable(TextFormatting.getTextWithoutFormattingCodes(displayName));
        }

        @Override
        public boolean writeClipboard(String name) {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(name), null);
                return name.equals(clipboard.getData(DataFlavor.stringFlavor));
            } catch (Exception exception) {
                LOGGER.warn("Could not copy item name to clipboard", exception);
                return false;
            }
        }

        @Override
        public void showFeedback(String name) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.player == null) return;

            TextComponentTranslation message = new TextComponentTranslation("itemnamecopy.copied", name);
            minecraft.player.sendStatusMessage(message, true);
        }
    }
}
