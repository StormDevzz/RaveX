package ravex.utility.system;

import ravex.RaveX;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class SystemUtility {

    private static final Set<String> MEDIA_PRIORITY = new LinkedHashSet<>(Arrays.asList(
        "spotify", "vlc", "mpv", "audacious", "clementine", "rhythmbox",
        "kodi", "smplayer", "deadbeef", "amarok", "elisa", "strawberry",
        "qobuz", "tidal", "ytmdl", "pulseeffects", "easyeffects"
    ));

    private static final Set<String> MEDIA_BLOCKED = new HashSet<>(Arrays.asList(
        "tdesktop", "telegram", "discord",
        "thunderbird", "slack", "teams", "signal",
        "whatsapp", "skype"
    ));

    private static String OS = null;

    private static String getOs() {
        if (OS == null) {
            OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        }
        return OS;
    }

    public static boolean isLinux() { return getOs().contains("linux"); }
    public static boolean isWindows() { return getOs().contains("windows"); }
    public static boolean isMac() { return getOs().contains("mac"); }

    private static List<String> exec(List<String> command) {
        List<String> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.add(line);
                    }
                } catch (Throwable ignored) {}
            }, "exec-reader");
            readerThread.setDaemon(true);
            readerThread.start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                readerThread.interrupt();
            }
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

    @Nullable private static String getPlayerPriority(String playerName) {
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

    private static final String[] BROWSER_SERVICES = {
        "soundcloud", "youtube", "yandex", "deezer", "bandcamp",
        "pandora", "tidal", "apple music", "amazon music", "mixcloud",
        "beatport", "audiomack", "spotify", "last.fm"
    };

    private static final String[] BARE_SERVICE_NAMES = {
        "youtube", "soundcloud", "youtube music", "spotify",
        "deezer", "bandcamp", "pandora", "tidal", "new tab"
    };

    private static final String[] BROWSER_EXE_SUFFIXES = {
        " - Brave", " - Google Chrome", " - Mozilla Firefox",
        " - Microsoft Edge", " - Opera", " - Vivaldi"
    };

    private static String querySmtcWindows() {
        try {
            String script = "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8;\n"
                + "try {\n"
                + "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows,ContentType=WindowsRuntime] | Out-Null;\n"
                + "[Windows.Storage.Streams.IRandomAccessStream,Windows,ContentType=WindowsRuntime] | Out-Null;\n"
                + "Add-Type -AssemblyName System.Runtime.WindowsRuntime;\n"
                + "$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1;\n"
                + "function Await-Op($op, $type) { return $asTask.MakeGenericMethod($type).Invoke($null, @($op)).GetAwaiter().GetResult() };\n"
                + "$mgr = Await-Op ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]);\n"
                + "$s = $mgr.GetCurrentSession();\n"
                + "$sessions = $mgr.GetSessions();\n"
                + "if (!$s -or $s.GetPlaybackInfo().PlaybackStatus -ne 'Playing') {\n"
                + "foreach ($candidate in $sessions) {\n"
                + "if ($candidate.GetPlaybackInfo().PlaybackStatus -eq 'Playing') { $s = $candidate; break }\n"
                + "}\n"
                + "if (!$s -and $sessions.Count -gt 0) { $s = $sessions[0] }\n"
                + "}\n"
                + "if ($null -ne $s) {\n"
                + "$props = Await-Op ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]);\n"
                + "$pos = 0; $len = 0;\n"
                + "try {\n"
                + "$t = $s.GetTimelineProperties();\n"
                + "if ($null -ne $t) {\n"
                + "$pos = [long]($t.Position.Ticks / 10);\n"
                + "$len = [long]($t.EndTime.Ticks / 10);\n"
                + "}\n"
                + "} catch { };\n"
                + "$artPath = '';\n"
                + "try {\n"
                + "if ($null -ne $props.Thumbnail) {\n"
                + "$streamOp = $props.Thumbnail.OpenReadAsync();\n"
                + "$stream = Await-Op $streamOp ([Windows.Storage.Streams.IRandomAccessStreamWithContentType]);\n"
                + "$asStream = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStream' -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1;\n"
                + "$netStream = $asStream.Invoke($null, @($stream));\n"
                + "$tempFile = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), 'ravex_smtc_art.jpg');\n"
                + "$fs = [System.IO.File]::Create($tempFile);\n"
                + "$netStream.CopyTo($fs);\n"
                + "$fs.Close();\n"
                + "$artPath = 'file:///' + $tempFile.Replace('\\', '/');\n"
                + "}\n"
                + "} catch { };\n"
                + "$app = $s.SourceAppUserModelId;\n"
                + "if ($app) {\n"
                + "$app = [System.IO.Path]::GetFileNameWithoutExtension($app);\n"
                + "if ($app.Length -gt 0) { $app = [char]::ToUpper($app[0]) + $app.Substring(1) }\n"
                + "} else { $app = '' };\n"
                + "$info = $s.GetPlaybackInfo();\n"
                + "$status = if ($info.PlaybackStatus -eq 'Playing') { 'Playing' } else { 'Paused' };\n"
                + "Write-Output ($status + '|' + $props.Title.Trim() + '|' + $props.Artist.Trim() + '|' + $artPath + '|' + $app + '|' + $pos + '|' + $len);\n"
                + "}\n"
                + "} catch { }\n";

            String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            List<String> lines = exec(Arrays.asList("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded));
            String last = "";
            for (String line : lines) {
                if (line != null && line.contains("|") && !line.startsWith("<") && !line.startsWith("#")) {
                    last = line.trim();
                }
            }
            if (last.isEmpty()) return "";
            String[] parts = last.split("\\|", -1);
            if (parts.length < 3) return "";
            String title = parts[1].trim();
            if (title.isEmpty()) return "";
            String artist = parts[2].trim();
            String artUrl = parts.length >= 4 ? parts[3].trim() : "";
            String app = parts.length >= 5 ? parts[4].trim() : "";
            String pos = parts.length >= 6 ? parts[5].trim() : "0";
            String len = parts.length >= 7 ? parts[6].trim() : "0";
            String status = "Playing".equalsIgnoreCase(parts[0].trim()) ? "Playing" : "Paused";
            return status + "|" + title + "|" + artist + "|" + artUrl + "|" + app + "|" + pos + "|" + len;
        } catch (Throwable t) {
            return "";
        }
    }

    private static List<String[]> queryWindowEntriesPowerShell() {
        List<String[]> entries = new ArrayList<>();
        try {
            String script = "Get-Process | Where-Object { $_.MainWindowTitle } | ForEach-Object { $_.ProcessName + '|' + $_.MainWindowTitle }";
            List<String> lines = exec(Arrays.asList("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script));
            for (String line : lines) {
                if (line == null) continue;
                int sep = line.indexOf('|');
                if (sep <= 0 || sep >= line.length() - 1) continue;
                String img = line.substring(0, sep).trim().toLowerCase(Locale.ROOT);
                String title = line.substring(sep + 1).trim();
                if (img.isEmpty() || title.isEmpty() || "N/A".equalsIgnoreCase(title)) continue;
                entries.add(new String[]{img, title});
            }
        } catch (Throwable t) {
        }
        return entries;
    }

    private static List<String[]> queryWindowEntriesTasklist() {
        List<String[]> entries = new ArrayList<>();
        try {
            List<String> lines = exec(Arrays.asList("cmd.exe", "/c", "chcp 65001 >nul & tasklist /v /fi \"STATUS eq RUNNING\" /fo csv"));
            for (String line : lines) {
                if (!line.startsWith("\"") || !line.contains("\",\"")) continue;
                String[] cols = line.split("\",\"");
                if (cols.length < 9) continue;
                String img = cols[0].replace("\"", "").trim().toLowerCase(Locale.ROOT);
                String title = cols[8].replace("\"", "").trim();
                if (title.isEmpty() || "N/A".equalsIgnoreCase(title) || "OleMainThreadWndName".equals(title)) continue;
                entries.add(new String[]{img, title});
            }
        } catch (Throwable t) {
        }
        return entries;
    }

    private static String queryWindows() {
        try {
            String smtc = querySmtcWindows();
            if (!smtc.isEmpty()) return smtc;

            List<String[]> entries = queryWindowEntriesPowerShell();
            if (entries.isEmpty()) entries = queryWindowEntriesTasklist();

            String bestBrowserCandidate = null;
            int bestBrowserPriority = Integer.MAX_VALUE;

            for (String[] entry : entries) {
                String img = entry[0];
                String title = entry[1];

                if (img.contains("spotify")) {
                    String result = parseSpotifyTitle(title);
                    if (result != null) return result;
                }

                if (img.contains("vlc")) {
                    String t = title.replace(" - VLC media player", "").trim();
                    return splitArtistTitle(t, "Playing");
                }

                if (img.contains("aimp")) {
                    String t = title.replace(" - AIMP", "").trim();
                    return splitArtistTitle(t, "Playing");
                }

                if (img.contains("musicbee")) {
                    String t = title.replace(" - MusicBee", "").trim();
                    return splitArtistTitle(t, "Playing");
                }

                if (img.contains("foobar2000")) {
                    String t = title.replace(" [foobar2000]", "").trim();
                    return splitArtistTitle(t, "Playing");
                }

                if (img.contains("winamp")) {
                    String t = title.replace(" - Winamp", "").trim();
                    return splitArtistTitle(t, "Playing");
                }

                if (img.contains("jmetersofter") || img.contains("aimp")) {
                    return splitArtistTitle(title, "Playing");
                }

                if (isBrowserProcess(img)) {
                    int priority = matchBrowserMedia(title);
                    if (priority >= 0 && priority < bestBrowserPriority) {
                        String parsed = cleanBrowserTitle(title);
                        if (parsed != null && !parsed.isEmpty()) {
                            bestBrowserPriority = priority;
                            bestBrowserCandidate = parsed;
                        }
                    }
                }
            }

            if (bestBrowserCandidate != null) return bestBrowserCandidate;
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[SystemUtility] Windows query failed", t);
        }
        return "";
    }

    private static boolean isBrowserProcess(String img) {
        return img.contains("brave") || img.contains("chrome") || img.contains("msedge") ||
               img.contains("firefox") || img.contains("opera") || img.contains("vivaldi") ||
               img.contains("waterfox") || img.contains("palemoon") || img.contains("seamonkey");
    }

    private static String stripExeSuffix(String title) {
        for (String suffix : BROWSER_EXE_SUFFIXES) {
            if (title.endsWith(suffix)) {
                return title.substring(0, title.length() - suffix.length()).trim();
            }
        }
        return title;
    }

    private static boolean isBareServiceName(String title) {
        String lower = title.toLowerCase(Locale.ROOT).trim();
        for (String name : BARE_SERVICE_NAMES) {
            if (lower.equals(name)) return true;
        }
        return false;
    }

    private static int matchBrowserMedia(String title) {
        String clean = stripExeSuffix(title);
        if (clean.isEmpty() || isBareServiceName(clean)) return -1;
        String lower = clean.toLowerCase(Locale.ROOT);
        for (int i = 0; i < BROWSER_SERVICES.length; i++) {
            if (lower.contains(BROWSER_SERVICES[i])) return i;
        }
        return -1;
    }

    @Nullable private static String cleanBrowserTitle(String title) {
        String clean = stripExeSuffix(title);
        if (clean.isEmpty() || isBareServiceName(clean)) return null;
        int pipe = clean.indexOf(" | Listen");
        if (pipe >= 0) clean = clean.substring(0, pipe).trim();
        pipe = clean.indexOf(" | SoundCloud");
        if (pipe >= 0) clean = clean.substring(0, pipe).trim();
        if (clean.startsWith("Stream ")) clean = clean.substring(7).trim();
        int by = clean.lastIndexOf(" by ");
        if (by > 0 && by < clean.length() - 4) {
            String track = clean.substring(0, by).trim();
            String artist = clean.substring(by + 4).trim();
            if (!track.isEmpty() && !artist.isEmpty()) {
                return "Playing|" + track + "|" + artist + "|||0|0";
            }
        }
        return splitArtistTitle(clean, "Playing");
    }

    @Nullable private static String parseSpotifyTitle(String title) {
        if (title.equals("Spotify") || title.equals("Spotify Premium") || title.equals("Spotify Free")) {
            return "Paused|Spotify||||0|0";
        }
        if (title.startsWith("Spotify - ")) {
            return "Paused|" + title.substring(10).trim() + "||||0|0";
        }
        if (title.contains(" - ")) {
            return splitArtistTitle(title, "Playing");
        }
        if (!title.toLowerCase(Locale.ROOT).startsWith("spotify")) {
            return splitArtistTitle(title, "Playing");
        }
        return null;
    }

    private static String splitArtistTitle(String raw, String status) {
        if (raw.isEmpty()) return "";
        if (raw.contains(" - ")) {
            String[] p = raw.split(" - ", 2);
            return status + "|" + p[1].trim() + "|" + p[0].trim() + "|||0|0";
        }
        return status + "|" + raw + "||||0|0";
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
            }
        }
        return "";
    }

    @Nullable public static byte[] downloadArt(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.startsWith("file:///")) {
                Path path = Paths.get(url.substring(8));
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
                try {
                    path = Paths.get(java.net.URI.create(url));
                    if (Files.exists(path)) return Files.readAllBytes(path);
                } catch (Throwable ignored) {}
                return null;
            } else if (url.startsWith("file://")) {
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

    @Nullable public static byte[] getAppIcon(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        if (isLinux() || isFreeBSD()) return getLinuxAppIcon(playerName);
        return null;
    }

    @Nullable private static byte[] getLinuxAppIcon(String playerName) {
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
