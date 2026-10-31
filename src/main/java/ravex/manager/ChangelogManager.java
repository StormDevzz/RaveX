package ravex.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ravex.RaveX;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// the history screen pulls the commit history from github
// the result is cached on disk and refreshed once a day
// the bundled changelog is the offline fallback
public class ChangelogManager {
    public static final ChangelogManager INSTANCE = new ChangelogManager();

    private static final String API_URL =
            "https://api.github.com/repos/StormDevzz/RaveX/commits?per_page=100";
    // hard cap for paging,30 pages =3000 commits.
    private static final int MAX_PAGES = 30;
    // the history is refreshed at most once a day.
    private static final long REFRESH_MS = 24L * 60L * 60L * 1000L;
    private static final String WEB_URL = "https://github.com/StormDevzz/RaveX/releases";
    private static final String BUNDLED_RESOURCE = "/ravex/changelog.md";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile List<Entry> entries = Collections.emptyList();
    private volatile boolean fromNetwork;

    private ChangelogManager() {}

    /* --------------------------------------------------------------- model */

    public enum BlockType {
        TITLE,
        HEADING,
        BULLET,
        TEXT,
        QUOTE
    }

    public static final class Span {
        public final String text;
        public final boolean bold;
        public final boolean code;

        Span(String text, boolean bold, boolean code) {
            this.text = text;
            this.bold = bold;
            this.code = code;
        }
    }

    public static final class Line {
        public final BlockType type;
        public final List<Span> spans;
        public final int indent;

        Line(BlockType type, List<Span> spans, int indent) {
            this.type = type;
            this.spans = spans;
            this.indent = indent;
        }

        public String plain() {
            StringBuilder sb = new StringBuilder();
            for (Span span : spans) sb.append(span.text);
            return sb.toString();
        }
    }

    public static final class Entry {
        public final String version;
        public final String date;
        public final String url;
        public final List<Line> lines;

        Entry(String version, String date, String url, List<Line> lines) {
            this.version = version;
            // bundled changelogs have no date, avoid nulls
            this.date = date == null ? "" : date;
            this.url = url;
            this.lines = lines;
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public Entry getLatest() {
        return entries.isEmpty() ? null : entries.get(0);
    }

    public boolean isFromNetwork() {
        return fromNetwork;
    }

    /* ------------------------------------------------------------- loading */

    // blocking load, call it off the render thread
    public void loadLocal() {
        List<Entry> cached = readCache();
        if (cached != null && !cached.isEmpty()) {
            entries = cached;
            fromNetwork = false;
            return;
        }
        String bundled = readBundledResource();
        if (bundled != null && !bundled.isBlank()) {
            List<Entry> parsed = parseMarkdown(bundled, null, null);
            if (!parsed.isEmpty()) {
                entries = parsed;
                fromNetwork = false;
                return;
            }
        }
        entries = Collections.emptyList();
        fromNetwork = false;
    }

    // pulls the full commit history from GitHub, at most once a day.
    public void loadRemote() {
        File cache = getCacheFile();
        if (cache.isFile() && System.currentTimeMillis() - cache.lastModified() < REFRESH_MS) {
            // fresh commits only show up about a day later
            return;
        }
        try {
            List<Entry> parsed = new ArrayList<>();
            for (int page = 1; page <= MAX_PAGES; page++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "&page=" + page))
                        .header("User-Agent", "RaveX-Client")
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    RaveX.LOGGER.info("[History] GitHub returned HTTP {}", response.statusCode());
                    return;
                }
                JsonElement root = JsonParser.parseString(response.body());
                if (!root.isJsonArray()) return;
                JsonArray array = root.getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject commit = element.getAsJsonObject();
                    String sha = optString(commit, "sha", "");
                    if (sha.length() < 7) continue;
                    if (!commit.has("commit") || !commit.get("commit").isJsonObject()) continue;
                    JsonObject inner = commit.getAsJsonObject("commit");
                    String message = optString(inner, "message", "");
                    if (message.isBlank()) continue;
                    String date = "";
                    if (inner.has("author") && inner.get("author").isJsonObject()) {
                        date = optString(inner.getAsJsonObject("author"), "date", "");
                    }
                    List<Line> lines = new ArrayList<>();
                    for (String messageLine : message.split("\\r?\\n")) {
                        if (messageLine.isBlank()) continue;
                        lines.addAll(parseMarkdownLines(messageLine.strip()));
                    }
                    if (lines.isEmpty()) continue;
                    parsed.add(new Entry(sha.substring(0, 7), formatDate(date),
                            optString(commit, "html_url", WEB_URL), lines));
                }
                if (array.size() < 100) break;
            }
            if (!parsed.isEmpty()) {
                entries = parsed;
                fromNetwork = true;
                writeCache(parsed);
            }
        } catch (Exception e) {
            RaveX.LOGGER.info("[History] Remote load failed: {}", e.getMessage());
        }
    }

        // the title wins, the tag cannot be inverted reliably
private static String versionFrom(JsonObject release) {
        String fromName = UpdateManager.versionFromLabel(optString(release, "name", ""));
        if (fromName != null) return fromName;
        String fromBody = UpdateManager.versionFromNotesTitle(optString(release, "body", ""));
        if (fromBody != null) return fromBody;
        String fromTag = UpdateManager.versionFromTag(optString(release, "tag_name", ""));
        return fromTag != null ? fromTag : "";
    }

    private static String formatDate(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return DATE_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return iso.length() > 10 ? iso.substring(0, 10) : iso;
        }
    }

    /* -------------------------------------------------------------- parsing */

    // splits markdown into one entry per # title block.
    public static List<Entry> parseMarkdown(String markdown, String defaultDate, String defaultUrl) {
        List<Entry> result = new ArrayList<>();
        List<Line> current = new ArrayList<>();
        String currentVersion = DEFAULT_VERSION;

        for (String raw : markdown.split("\\r?\\n")) {
            String line = raw.strip();
            if (line.startsWith("# ")) {
                if (!current.isEmpty()) {
                    result.add(new Entry(currentVersion, defaultDate, defaultUrl, current));
                }
                currentVersion = versionFromTitle(line.substring(2));
                current = new ArrayList<>();
                continue;
            }
            current.addAll(parseMarkdownLines(line));
        }
        if (!current.isEmpty()) {
            result.add(new Entry(currentVersion, defaultDate, defaultUrl, current));
        }
        result.removeIf(entry -> entry.lines.isEmpty());
        return result;
    }

    private static final String DEFAULT_VERSION = null;

    // parses a single markdown line into styled spans.
    public static List<Line> parseMarkdownLines(String raw) {
        List<Line> out = new ArrayList<>();
        if (raw == null) return out;
        String line = raw.strip();
        if (line.isEmpty()) return out;

        BlockType type = BlockType.TEXT;
        String content = line;

        if (line.startsWith("### ")) {
            type = BlockType.HEADING;
            content = line.substring(4).strip();
        } else if (line.startsWith("## ")) {
            type = BlockType.HEADING;
            content = line.substring(3).strip();
        } else if (line.startsWith("# ")) {
            type = BlockType.TITLE;
            content = line.substring(2).strip();
        } else if (line.startsWith("> ")) {
            type = BlockType.QUOTE;
            content = line.substring(2).strip();
        } else {
            int indent = 0;
            String trimmed = line;
            while (trimmed.startsWith("  ") || trimmed.startsWith("\t")) {
                indent++;
                trimmed = trimmed.strip();
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                type = BlockType.BULLET;
                content = trimmed.substring(2).strip();
            } else {
                content = trimmed;
            }
            out.add(new Line(type, parseSpans(content), indent));
            return out;
        }

        out.add(new Line(type, parseSpans(content), 0));
        return out;
    }

    private static String versionFromTitle(String title) {
        String cleaned = title.replaceAll("[^0-9.]", "");
        while (cleaned.startsWith(".")) cleaned = cleaned.substring(1);
        while (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        while (cleaned.contains("..")) cleaned = cleaned.replace("..", ".");
        return cleaned.isEmpty() ? DEFAULT_VERSION : cleaned;
    }

    // minimal inline markdown: **bold**, *italic*, `code`, [text](url).
    private static List<Span> parseSpans(String text) {
        List<Span> spans = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean bold = false;
        boolean code = false;
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (!code && text.startsWith("**", i)) {
                flush(spans, buffer, bold, code);
                bold = !bold;
                i += 2;
                continue;
            }
            if (!code && c == '`') {
                flush(spans, buffer, bold, code);
                code = !code;
                i++;
                continue;
            }
            if (!code && c == '[') {
                int close = text.indexOf(']', i);
                if (close != -1 && close + 1 < text.length() && text.charAt(close + 1) == '(') {
                    int end = text.indexOf(')', close + 2);
                    if (end != -1) {
                        flush(spans, buffer, bold, code);
                        spans.add(new Span(text.substring(i + 1, close), bold, true));
                        i = end + 1;
                        continue;
                    }
                }
            }
            if (!code && !bold && c == '*') {
                i++;
                continue;
            }

            buffer.append(c);
            i++;
        }
        flush(spans, buffer, bold, code);
        return spans;
    }

    private static void flush(List<Span> spans, StringBuilder buffer, boolean bold, boolean code) {
        if (buffer.isEmpty()) return;
        spans.add(new Span(buffer.toString(), bold, code));
        buffer.setLength(0);
    }

    /* ---------------------------------------------------------------- cache */

    private static File getCacheFile() {
        return new File(UpdateManager.getGameDir(), "RaveX/update/changelog.json");
    }

    private void writeCache(List<Entry> list) {
        try {
            JsonArray array = new JsonArray();
            for (Entry entry : list) {
                JsonObject obj = new JsonObject();
                obj.addProperty("v", entry.version);
                obj.addProperty("d", entry.date);
                obj.addProperty("u", entry.url);
                JsonArray lines = new JsonArray();
                for (Line line : entry.lines) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(line.type.name()).append('\t').append(line.indent);
                    for (Span span : line.spans) {
                        sb.append('\t').append(span.bold ? "B" : (span.code ? "C" : "N")).append(span.text);
                    }
                    lines.add(sb.toString());
                }
                obj.add("l", lines);
                array.add(obj);
            }
            File file = getCacheFile();
            Path parent = file.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                out.write(("{\"kind\":\"commits\",\"entries\":" + array + "}").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            RaveX.LOGGER.info("[Changelog] Could not write cache: {}", e.getMessage());
        }
    }

    private List<Entry> readCache() {
        try {
            File file = getCacheFile();
            if (!file.isFile()) return null;
            JsonElement root = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return null;
            JsonObject rootObj = root.getAsJsonObject();
            JsonElement cacheKind = rootObj.get("kind");
            if (cacheKind == null || !"commits".equals(cacheKind.getAsString())) return null;
            JsonElement arrayElement = rootObj.get("entries");
            if (arrayElement == null || !arrayElement.isJsonArray()) return null;

            List<Entry> list = new ArrayList<>();
            for (JsonElement element : arrayElement.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                List<Line> lines = new ArrayList<>();
                JsonElement linesElement = obj.get("l");
                if (linesElement != null && linesElement.isJsonArray()) {
                    for (JsonElement lineElement : linesElement.getAsJsonArray()) {
                        String[] parts = lineElement.getAsString().split("\t", -1);
                        if (parts.length < 2) continue;
                        BlockType type;
                        try {
                            type = BlockType.valueOf(parts[0]);
                        } catch (IllegalArgumentException e) {
                            type = BlockType.TEXT;
                        }
                        int indent;
                        try {
                            indent = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            indent = 0;
                        }
                        List<Span> spans = new ArrayList<>();
                        for (int i = 2; i < parts.length; i++) {
                            String part = parts[i];
                            if (part.isEmpty()) continue;
                            char kind = part.charAt(0);
                            spans.add(new Span(part.substring(1),
                                    kind == 'B', kind == 'C'));
                        }
                        lines.add(new Line(type, spans, indent));
                    }
                }
                list.add(new Entry(optString(obj, "v", ""), optString(obj, "d", ""),
                        optString(obj, "u", WEB_URL), lines));
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readBundledResource() {
        try (InputStream in = ChangelogManager.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        try {
            String v = obj.get(key).getAsString();
            return v == null ? fallback : v;
        } catch (Exception e) {
            return fallback;
        }
    }
}
