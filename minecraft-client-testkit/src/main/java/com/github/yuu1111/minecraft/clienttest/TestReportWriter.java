package com.github.yuu1111.minecraft.clienttest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

final class TestReportWriter {
    private TestReportWriter() {
    }

    static void write(
        ClientTestOptions options,
        List<TestResult> results,
        int expectedTests,
        String suiteName
    ) throws IOException {
        int failed = 0;
        for (TestResult result : results) if (result.failure != null) failed++;
        StringBuilder json = new StringBuilder("{\n  \"target\": ").append(quote(options.target()))
            .append(",\n  \"source\": ").append(quote(options.source()))
            .append(",\n  \"mode\": ").append(quote(options.mode())).append(',')
                .append("\n  \"passed\": ").append(results.size() - failed).append(",\n  \"failed\": ").append(failed)
                .append(",\n  \"expectedTests\": ").append(expectedTests).append(",\n  \"tests\": [");
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuite name=\"")
            .append(options.target()).append("\" tests=\"").append(results.size())
                .append("\" failures=\"").append(failed).append("\">");
        for (int i = 0; i < results.size(); i++) {
            TestResult result = results.get(i);
            if (i > 0) json.append(',');
            json.append("\n    {\"name\": ").append(quote(result.name)).append(", \"status\": ")
                    .append(quote(result.failure == null ? "passed" : "failed"));
            if (result.failure != null) json.append(", \"message\": ").append(quote(result.failure));
            json.append('}');
            xml.append("<testcase classname=\"").append(escapeXml(suiteName)).append("\" name=\"")
                .append(result.name).append("\">");
            if (result.failure != null)
                xml.append("<failure message=\"").append(escapeXml(result.failure)).append("\"/>");
            xml.append("</testcase>");
        }
        json.append("\n  ]\n}\n");
        xml.append("</testsuite>");
        Files.createDirectories(options.report().toAbsolutePath().getParent());
        Files.write(options.report(), json.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(options.report().resolveSibling("TEST-client.xml"), xml.toString().getBytes(StandardCharsets.UTF_8));
        options.log("RESULT " + (results.size() - failed) + " passed, " + failed + " failed");
    }

    private static String quote(String text) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '"' || character == '\\') escaped.append('\\').append(character);
            else if (character < 32) {
                escaped.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) character));
            } else escaped.append(character);
        }
        return escaped.append('"').toString();
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
