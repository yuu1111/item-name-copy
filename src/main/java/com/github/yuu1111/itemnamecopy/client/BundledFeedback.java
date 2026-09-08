package com.github.yuu1111.itemnamecopy.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class BundledFeedback {
    private static final String COPIED_KEY = "itemnamecopy.copied";
    private static final String ENGLISH = load("en_us");
    private static final String JAPANESE = load("ja_jp");

    private BundledFeedback() {
    }

    static String copied(String language, String name) {
        String format = "ja_jp".equals(language) ? JAPANESE : ENGLISH;
        return String.format(Locale.ROOT, format, name);
    }

    private static String load(String language) {
        String path = "/assets/itemnamecopy/lang/" + language + ".json";
        try (InputStream stream = BundledFeedback.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing bundled translations: " + path);

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject translations = new Gson().fromJson(reader, JsonObject.class);
                return translations.get(COPIED_KEY).getAsString();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled translations: " + path, exception);
        }
    }
}
