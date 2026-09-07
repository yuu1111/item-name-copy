package dev.itemnamecopy.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class BundledFeedback {
    private static final String ENGLISH = load("en_us");
    private static final String JAPANESE = load("ja_jp");

    private BundledFeedback() {}

    static String copied(String language, String name) {
        return String.format(Locale.ROOT, "ja_jp".equals(language) ? JAPANESE : ENGLISH, name);
    }

    private static String load(String language) {
        String path = "/assets/itemnamecopy/lang/" + language + ".json";
        try (InputStream stream = BundledFeedback.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing bundled translations: " + path);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, JsonObject.class).get("itemnamecopy.copied").getAsString();
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not read bundled translations: " + path, exception);
        }
    }
}
