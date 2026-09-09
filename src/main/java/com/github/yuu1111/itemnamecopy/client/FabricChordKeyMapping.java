package com.github.yuu1111.itemnamecopy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if >=1.16 {
import net.minecraft.network.chat.Component;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}
//?}
//? if <1.16 {
/*import net.minecraft.client.resources.language.I18n;
*///?}
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

final class FabricChordKeyMapping extends KeyMapping {
    private static final int ALL_MODIFIERS = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL
            | GLFW.GLFW_MOD_ALT;
    private static final int DEFAULT_MODIFIERS = GLFW.GLFW_MOD_CONTROL;

    private int modifiers = DEFAULT_MODIFIERS;
    private int pendingModifiers;
    private boolean pendingRebind;
    private boolean loadingOptions;
    private File configFile;

    //? if >=1.21.9 {
    /*FabricChordKeyMapping(String name, int key, KeyMapping.Category category) {
        super(name, key, category);
    }
    *///?} else {
    FabricChordKeyMapping(String name, int key, String category) {
        super(name, key, category);
    }
    //?}

    boolean matchesShortcut(int key, int scanCode, int eventModifiers) {
        //? if >=1.21.9 {
        /*boolean keyMatches = matches(new net.minecraft.client.input.KeyEvent(key, scanCode, eventModifiers));
        *///?} else {
        boolean keyMatches = matches(key, scanCode);
        //?}
        return keyMatches && (modifiers == 0 || (eventModifiers & modifiers) != 0);
    }

    void beginOptionsLoad(File gameDirectory) {
        configFile = new File(new File(gameDirectory, "config"), "itemnamecopy.properties");
        loadingOptions = true;
        if (!configFile.isFile()) return;

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            int stored = Integer.parseInt(properties.getProperty("copyModifiers", "2"));
            if (stored == 0 || stored == GLFW.GLFW_MOD_SHIFT || stored == GLFW.GLFW_MOD_CONTROL
                    || stored == GLFW.GLFW_MOD_ALT) modifiers = stored;
        } catch (IOException | NumberFormatException ignored) {
            modifiers = DEFAULT_MODIFIERS;
        }
    }

    void finishOptionsLoad() {
        loadingOptions = false;
    }

    void prepareRebind(int eventModifiers) {
        pendingModifiers = normalizeModifier(eventModifiers);
        pendingRebind = true;
    }

    @Override
    public void setKey(InputConstants.Key key) {
        super.setKey(key);
        if (loadingOptions) return;

        if (pendingRebind) {
            modifiers = pendingModifiers;
            pendingRebind = false;
        } else if (key.equals(getDefaultKey())) {
            modifiers = DEFAULT_MODIFIERS;
        }
        saveModifiers();
    }

    @Override
    public boolean isDefault() {
        return super.isDefault() && modifiers == DEFAULT_MODIFIERS;
    }

    //? if >=1.16 {
    @Override
    public Component getTranslatedKeyMessage() {
        String display = displayName();
        //? if >=1.19 {
        return Component.literal(display);
        //?} else {
        /*return new TextComponent(display);
        *///?}
    }
    //?} else {
    /*@Override
    public String getTranslatedKeyMessage() {
        return displayName();
    }
    *///?}

    private String displayName() {
        StringBuilder display = new StringBuilder();
        appendModifier(display, GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL);
        appendModifier(display, GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT);
        appendModifier(display, GLFW.GLFW_MOD_ALT, GLFW.GLFW_KEY_LEFT_ALT);
        if (display.length() > 0) display.append(" + ");
        //? if >=1.16 {
        display.append(super.getTranslatedKeyMessage().getString());
        //?} else {
        /*display.append(super.getTranslatedKeyMessage());
        *///?}
        return display.toString();
    }

    private void appendModifier(StringBuilder display, int flag, int keyCode) {
        if ((modifiers & flag) == 0) return;
        if (display.length() > 0) display.append(" + ");
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        //? if >=1.19 {
        display.append(Component.translatable(key.getName()).getString());
        //?} elif >=1.16 {
        /*display.append(new TranslatableComponent(key.getName()).getString());
        *///?} else {
        /*display.append(I18n.get(key.getName()));
        *///?}
    }

    private void saveModifiers() {
        if (configFile == null) return;
        File parent = configFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) return;

        Properties properties = new Properties();
        properties.setProperty("copyModifiers", Integer.toString(modifiers));
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "ItemNameCopy key binding");
        } catch (IOException ignored) {
        }
    }

    private static int normalizeModifier(int eventModifiers) {
        if ((eventModifiers & GLFW.GLFW_MOD_SHIFT) != 0) return GLFW.GLFW_MOD_SHIFT;
        if ((eventModifiers & GLFW.GLFW_MOD_CONTROL) != 0) return GLFW.GLFW_MOD_CONTROL;
        if ((eventModifiers & GLFW.GLFW_MOD_ALT) != 0) return GLFW.GLFW_MOD_ALT;
        return 0;
    }
}
