package ravex.utility.system;

import ravex.RaveX;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SystemUtility {

    private static final Set<String> MEDIA_PRIORITY = new LinkedHashSet<>(Arrays.asList(
        "spotify", "vlc", "mpv", "audacious", "clementine", "rhythmbox",
        "kodi", "smplayer", "deadbeef", "amarok", "elisa", "strawberry",
        "qobuz", "tidal", "ytmdl", "pulseeffects", "easyeffects"
    ));

    private static final Set<String> MEDIA_BLOCKED = new HashSet<>(Arrays.asList(
        "tdesktop", "telegram", "discord", "firefox", "chromium", "brave",
        "opera", "edge", "thunderbird", "slack", "teams", "signal",
        "whatsapp", "skype"
    ));

    private static String OS = null;

    private static String getOs() {
        if (OS == null) {
            OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        }
        return OS;
    }

    private static boolean isLinux() { return getOs().contains("linux"); }
    private static boolean isWindows() { return getOs().contains("windows"); }
    private static boolean isMac() { return getOs().contains("mac"); }

    private static List<String> exec(List<String> command) {
        List<String> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                }
            }
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[SystemUtility] exec failed: {} {}", command.get(0), t.getMessage());
        }
        return result;
    }

    public static boolean isAvailable() {
        if (isLinux() || isFreeBSD()) {
            return hasBusctl() || hasDbusSend();
        }
        if (isWindows()) {
            return true;
        }
        if (isMac()) {
            return hasOsascript();
        }
        return false;
    }

    private static boolean isFreeBSD() {
        return getOs().contains("freebsd");
    }

    private static boolean hasBusctl() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "busctl");
            Process p = pb.start();
            return p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasDbusSend() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "dbus-send");
            Process p = pb.start();
            return p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasOsascript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "osascript");
            Process p = pb.start();
            return p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getNowPlaying() {
        try {
            if (isLinux() || isFreeBSD()) return queryMpris();
            if (isWindows()) return queryWindows();
            if (isMac()) return queryMacOs();
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[SystemUtility] getNowPlaying error", t);
        }
        return "";
    }

    private static String getPlayerPriority(String playerName) {
        String lower = playerName.toLowerCase(Locale.ROOT);
        for (String blocked : MEDIA_BLOCKED) {
            if (lower.contains(blocked)) return null;
        }
        for (String prio : MEDIA_PRIORITY) {
            if (lower.contains(prio)) return prio;
        }
        return "";
    }

    private static String queryMpris() {
        if (hasBusctl()) {
            return queryMprisBusctl();
        }
        if (hasDbusSend()) {
            return queryMprisDbusSend();
        }
        return "";
    }

    private static String queryMprisBusctl() {
        List<String> lines = exec(Arrays.asList("busctl", "list", "--no-legend"));
        String bestPlayer = null;
        int bestScore = Integer.MAX_VALUE;

        for (String line : lines) {
            String name = line.trim().split("\\s+")[0];
            if (!name.contains("org.mpris.MediaPlayer2")) continue;

            String priority = getPlayerPriority(name);
            if (priority == null) continue;

            int score;
            if (priority.isEmpty()) {
                score = 999;
            } else {
                int idx = 0;
                for (String p : MEDIA_PRIORITY) {
                    if (p.equals(priority)) break;
                    idx++;
                }
                score = idx;
            }

            if (score < bestScore) {
                String status = getMprisPropertyBusctl(name, "PlaybackStatus");
                if (status == null || status.isEmpty()) continue;
                if (!"Playing".equals(status) && !"Paused".equals(status)) continue;
                bestPlayer = name;
                bestScore = score;
                if (score == 0) break;
            }
        }

        if (bestPlayer == null) return "";

        String status = getMprisPropertyBusctl(bestPlayer, "PlaybackStatus");
        String metadata = getMprisPropertyBusctl(bestPlayer, "Metadata");
        String position = getMprisPropertyBusctl(bestPlayer, "Position");

        return formatMprisOutput(bestPlayer, status, metadata, position);
    }

    private static String getMprisPropertyBusctl(String busName, String property) {
        List<String> out = exec(Arrays.asList(
            "busctl", "get-property", busName,
            "/org/mpris/MediaPlayer2",
            "org.mpris.MediaPlayer2.Player", property
        ));
        if (out.isEmpty()) return "";
        return String.join("\n", out);
    }

    private static String queryMprisDbusSend() {
        List<String> lines = exec(Arrays.asList(
            "dbus-send", "--session", "--dest=org.freedesktop.DBus",
            "--type=method_call", "--print-reply",
            "/org/freedesktop/DBus",
            "org.freedesktop.DBus.ListNames"
        ));
        String bestPlayer = null;
        int bestScore = Integer.MAX_VALUE;

        for (String line : lines) {
            String name = line.trim().replaceAll("\"", "");
            if (!name.contains("org.mpris.MediaPlayer2")) continue;

            String priority = getPlayerPriority(name);
            if (priority == null) continue;

            int score;
            if (priority.isEmpty()) {
                score = 999;
            } else {
                int idx = 0;
                for (String p : MEDIA_PRIORITY) {
                    if (p.equals(priority)) break;
                    idx++;
                }
                score = idx;
            }

            if (score < bestScore) {
                String status = getMprisPropertyDbusSend(name, "PlaybackStatus");
                if (status == null || status.isEmpty()) continue;
                if (!"Playing".equals(status) && !"Paused".equals(status)) continue;
                bestPlayer = name;
                bestScore = score;
                if (score == 0) break;
            }
        }

        if (bestPlayer == null) return "";

        String status = getMprisPropertyDbusSend(bestPlayer, "PlaybackStatus");
        String metadata = getMprisPropertyDbusSend(bestPlayer, "Metadata");
        String position = getMprisPropertyDbusSend(bestPlayer, "Position");

        return formatMprisOutput(bestPlayer, status, metadata, position);
    }

    private static String getMprisPropertyDbusSend(String busName, String property) {
        List<String> out = exec(Arrays.asList(
            "dbus-send", "--session", "--dest=" + busName,
            "--type=method_call", "--print-reply",
            "/org/mpris/MediaPlayer2",
            "org.freedesktop.DBus.Properties.Get",
            "string:org.mpris.MediaPlayer2.Player",
            "string:" + property
        ));
        if (out.isEmpty()) return "";
        return String.join("\n", out);
    }

    private static String formatMprisOutput(String playerName, String status, String metadata, String position) {
        boolean playing = "Playing".equals(parseDbusString(status));
        String title = extractMetadataField(metadata, "mpris:title");
        String artist = extractMetadataField(metadata, "mpris:artist");
        String artUrl = extractMetadataField(metadata, "mpris:artUrl");
        long len = extractMetadataLong(metadata, "mpris:length");
        long pos = parseDbusLong(position);

        String artistStr = artist.isEmpty() ? "" : artist;
        String artStr = artUrl.isEmpty() ? "" : artUrl;
        String iconStr = "";

        return String.join("|",
            playing ? "Playing" : "Paused",
            title,
            artistStr,
            artStr,
            iconStr,
            String.valueOf(pos),
            String.valueOf(len)
        );
    }

    static String parseDbusString(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        raw = raw.trim();
        if (raw.startsWith("s ")) {
            raw = raw.substring(2).trim();
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                return raw.substring(1, raw.length() - 1);
            }
            return raw;
        }
        if (raw.startsWith("variant")) {
            raw = raw.substring(7).trim();
            if (raw.startsWith("s ")) {
                raw = raw.substring(2).trim();
                if (raw.startsWith("\"") && raw.endsWith("\"")) {
                    return raw.substring(1, raw.length() - 1);
                }
                return raw;
            }
        }
        return "";
    }

    static long parseDbusLong(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        raw = raw.trim();
        if (raw.startsWith("t ")) {
            raw = raw.substring(2).trim();
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (raw.startsWith("x ")) {
            raw = raw.substring(2).trim();
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (raw.startsWith("variant")) {
            raw = raw.substring(7).trim();
            if (raw.startsWith("t ") || raw.startsWith("x ")) {
                raw = raw.substring(2).trim();
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    static String extractMetadataField(String metadata, String field) {
        if (metadata == null || metadata.isEmpty()) return "";
        int idx = metadata.indexOf("\"" + field + "\"");
        if (idx < 0) return "";
        String after = metadata.substring(idx + field.length() + 2).trim();
        String[] tokens = after.split("\\s+", 3);
        if (tokens.length < 2) return "";
        String type = tokens[0];
        String value = tokens.length >= 2 ? tokens[1] : "";

        switch (type) {
            case "s":
                if (value.startsWith("\"")) {
                    int end = value.indexOf("\"", 1);
                    if (end > 0) return value.substring(1, end);
                    return value.substring(1);
                }
                return value;
            case "as":
                if (tokens.length >= 3) {
                    String rest = tokens[1] + " " + tokens[2];
                    int start = rest.indexOf("\"");
                    if (start >= 0) {
                        int end = rest.indexOf("\"", start + 1);
                        if (end > start) return rest.substring(start + 1, end);
                    }
                }
                return "";
            default:
                return "";
        }
    }

    static long extractMetadataLong(String metadata, String field) {
        if (metadata == null || metadata.isEmpty()) return 0;
        int idx = metadata.indexOf("\"" + field + "\"");
        if (idx < 0) return 0;
        String after = metadata.substring(idx + field.length() + 2).trim();
        String[] tokens = after.split("\\s+", 3);
        if (tokens.length < 2) return 0;
        String type = tokens[0];
        if (!type.equals("t") && !type.equals("x")) return 0;
        try {
            return Long.parseLong(tokens[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String queryWindows() {
        try {
            List<String> out = exec(Arrays.asList(
                "powershell", "-NoProfile", "-Command",
                "try { " +
                "$m = Get-CimInstance -Namespace 'Root/Media/Control' -Class 'SystemMediaTransportControls' 2>$null; " +
                "if ($m) { $status='Stopped'; " +
                "if ($m.PlaybackStatus -eq 3) { $status='Playing' } " +
                "elseif ($m.PlaybackStatus -eq 4) { $status='Paused' }; " +
                "Write-Output $status; " +
                "Write-Output $m.Title; " +
                "Write-Output $m.Artist; " +
                "Write-Output $m.Thumbnail; " +
                "Write-Output ($m.Position.Ticks / 10); " +
                "Write-Output ($m.MediaDuration.Ticks / 10) } } catch {}"
            ));
            if (out.size() >= 2) {
                return String.join("|", out.get(0), out.get(1),
                    out.size() > 2 ? out.get(2) : "",
                    out.size() > 3 ? out.get(3) : "", "",
                    out.size() > 4 ? out.get(4) : "0",
                    out.size() > 5 ? out.get(5) : "0");
            }
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[SystemUtility] Windows query failed", t);
        }
        return "";
    }

    private static String queryMacOs() {
        List<String> apps = Arrays.asList("Spotify", "Music", "iTunes");
        for (String app : apps) {
            try {
                List<String> out = exec(Arrays.asList(
                    "osascript", "-e",
                    "tell application \"" + app + "\" " +
                    "if player state is playing or player state is paused then " +
                    "set trackName to name of current track " +
                    "set artistName to artist of current track " +
                    "set artUrl to \"\" " +
                    "try set artUrl to artwork url of current track end try " +
                    "set pos to player position * 1000000 " +
                    "set dur to duration of current track * 1000000 " +
                    "set st to \"Paused\" " +
                    "if player state is playing then set st to \"Playing\" " +
                    "return st & \"|\" & trackName & \"|\" & artistName & \"|\" & artUrl & \"||\" & pos & \"|\" & dur " +
                    "end if " +
                    "end tell"
                ));
                if (!out.isEmpty()) {
                    String result = String.join("", out);
                    if (!result.isEmpty() && (result.contains("Playing") || result.contains("Paused"))) {
                        return result;
                    }
                }
            } catch (Throwable t) {
                // try next app
            }
        }
        return "";
    }

    public static byte[] downloadArt(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.startsWith("file://")) {
                Path path = Paths.get(java.net.URI.create(url));
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
                return null;
            }
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "RaveX/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            try (java.io.InputStream in = conn.getInputStream()) {
                byte[] data = in.readAllBytes();
                if (data.length > 0) return data;
            }
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[SystemUtility] downloadArt failed: {}", t.getMessage());
        }
        return null;
    }

    public static byte[] getAppIcon(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        if (isLinux() || isFreeBSD()) return getLinuxAppIcon(playerName);
        return null;
    }

    private static byte[] getLinuxAppIcon(String playerName) {
        String lower = playerName.toLowerCase(Locale.ROOT);
        String iconName = lower
            .replace("org.mpris.mediaplayer2.", "")
            .replace("org.mpris.mediaplayer2.", "");
        if (iconName.isEmpty()) iconName = lower;

        List<Path> searchPaths = Arrays.asList(
            Paths.get(System.getProperty("user.home"), ".local", "share", "icons", "hicolor", "48x48", "apps"),
            Paths.get("/usr", "share", "icons", "hicolor", "48x48", "apps"),
            Paths.get("/usr", "share", "pixmaps")
        );

        for (Path dir : searchPaths) {
            if (!Files.isDirectory(dir)) continue;
            try {
                for (String ext : Arrays.asList(".png", ".svg", ".xpm")) {
                    Path iconPath = dir.resolve(iconName + ext);
                    if (Files.exists(iconPath)) {
                        return Files.readAllBytes(iconPath);
                    }
                }
                String searchName = iconName;
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                    Optional<Path> match = stream
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(searchName))
                        .findFirst();
                    if (match.isPresent()) {
                        Path p = match.get();
                        String name = p.getFileName().toString();
                        if (name.endsWith(".png")) {
                            return Files.readAllBytes(p);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
