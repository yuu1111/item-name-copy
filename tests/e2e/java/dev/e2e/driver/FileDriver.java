package dev.e2e.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileDriver {
    private final Path directory = Paths.get(System.getenv("E2E_CONTROL"));
    private final long pid = ProcessHandle.current().pid();
    private int sequence;

    public void call(String action, String fields, Runnable pump) throws Exception {
        int id = ++sequence;
        String request = "{\"id\":" + id + ",\"pid\":" + pid + ",\"action\":" + quote(action) + fields + "}";
        Path temporary = directory.resolve("request.tmp");
        Files.write(temporary, request.getBytes(StandardCharsets.UTF_8));
        Files.move(temporary, directory.resolve("request.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        long deadline = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < deadline) {
            pump.run();
            Path responsePath = directory.resolve("response.json");
            if (Files.isRegularFile(responsePath)) {
                String response = new String(Files.readAllBytes(responsePath), StandardCharsets.UTF_8);
                Matcher responseId = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(response);
                Matcher exitCode = Pattern.compile("\"exitCode\"\\s*:\\s*(\\d+)").matcher(response);
                if (responseId.find() && Integer.parseInt(responseId.group(1)) == id) {
                    if (!exitCode.find() || Integer.parseInt(exitCode.group(1)) != 0) {
                        throw new AssertionError("External driver failed: " + response);
                    }
                    return;
                }
            }
            Thread.sleep(5);
        }
        throw new AssertionError("External driver timed out: " + request);
    }

    public static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
