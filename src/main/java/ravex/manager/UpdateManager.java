package ravex.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ravex.RaveX;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automatic update system.
 *
 * Flow: check GitHub Releases for a newer tag, let the user download the new jar,
 * then hand the replacement over to {@link ravex.update.UpdateInstaller} which waits
 * for the game process to exit before swapping the file (a running jar is locked on
 * Windows, so the swap cannot happen from inside the game).
 */
public class UpdateManager {
    public static final UpdateManager INSTANCE = new UpdateManager();

    private static final String REPO_OWNER = "StormDevzz";
    private static final String REPO_NAME = "RaveX";
    private static final String API_RELEASES = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases";
    private static final String API_LATEST = API_RELEASES + "/latest";
    private static final String WEB_RELEASES = "https://github.com/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";

    private static final long REQUEST_TIMEOUT_SECONDS = 20L;
    private static final long CONNECT_TIMEOUT_SECONDS = 10L;
    private static final long LAST_CHECK_FILE_MAX_AGE = 3L * 60L * 60L * 1000L;
    private static final int DOWNLOAD_BUFFER = 64 * 1024;

    public enum State {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        AVAILABLE,
        DOWNLOADING,
        DOWNLOADED,
        INSTALLING,
        ERROR
    }

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile State state = State.IDLE;
    private volatile String latestVersion;
    private volatile String releaseName;
    private volatile String releaseNotes;
    private volatile String publishedAt;
    private volatile String releasePageUrl = WEB_RELEASES;
    private volatile String downloadUrl;
    private volatile String expectedSha256;
    private volatile String errorMessage;
    private volatile String statusMessage = "";
    private volatile long downloadTotal;
    private volatile long downloadDone;
    private volatile File stagedJar;
    private volatile boolean updatable = true;

    private UpdateManager() {
        updatable = resolveCurrentJarPath() != null;
    }

    /* ------------------------------------------------------------------ state */

    public State getState() {
        return state;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getReleasePageUrl() {
        return releasePageUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getDownloadTotal() {
        return downloadTotal;
    }

    public long getDownloadDone() {
        return downloadDone;
    }

    public File getStagedJar() {
        return stagedJar;
    }

    /**
     * False when the mod runs from a dev environment, where replacing the jar makes no sense.
     * Resolved once because it depends on where Fabric loaded the mod from.
     */
public boolean isUpdatable() {
        return updatable;
    }

    public boolean isUpdateAvailable() {
        return state == State.AVAILABLE
                || state == State.DOWNLOADING
                || state == State.DOWNLOADED
                || state == State.INSTALLING;
    }

    public float getDownloadProgress() {
        long total = downloadTotal;
        if (total <= 0L) return 0f;
        return Math.min(1f, (float) ((double) downloadDone / (double) total));
    }

    public String getCurrentVersion() {
        String v = RaveX.version;
        return v == null ? "0.0.0" : v.trim();
    }

    /* ------------------------------------------------------------------ check */

    /** Runs a check in the background, unless one is already running or was done recently. */
    public void checkInBackground() {
        if (isUpdatable() && shouldSkipRecentCheck()) return;
        checkForUpdates();
    }

    public CompletableFuture<Void> checkForUpdates() {
        if (!busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        state = State.CHECKING;
        statusMessage = "Checking for updates...";
        errorMessage = null;

        HttpRequest request = buildRequest(API_LATEST);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseRelease)
                .thenAccept(this::applyReleaseResult)
                .exceptionally(ex -> {
                    errorMessage = describe(ex);
                    state = State.ERROR;
                    statusMessage = "";
                    RaveX.LOGGER.warn("[UpdateManager] Update check failed: {}", errorMessage);
                    return null;
                })
                .whenComplete((ignored, err) -> busy.set(false));
    }

    private void applyReleaseResult(JsonObject release) {
        if (release == null) {
            state = State.ERROR;
            errorMessage = "GitHub returned no release data";
            return;
        }
        latestVersion = readVersion(release);
        releaseName = optString(release, "name", latestVersion);
        releaseNotes = optString(release, "body", "");
        publishedAt = optString(release, "published_at", "");
        releasePageUrl = optString(release, "html_url", WEB_RELEASES);

        String tag = optString(release, "tag_name", "");
        if (latestVersion == null || latestVersion.isEmpty()) {
            latestVersion = versionFromTag(tag);
        }

        if (latestVersion == null || latestVersion.isEmpty()) {
            state = State.ERROR;
            errorMessage = "Could not read the released version";
            statusMessage = "";
            return;
        }

        if (compareVersions(latestVersion, getCurrentVersion()) > 0) {
            JsonObject jar = pickJarAsset(release);
            expectedSha256 = findSha256Asset(release, jar);
            if (jar == null) {
                state = State.AVAILABLE;
                downloadUrl = null;
                statusMessage = ravex.utility.misc.LanguageUtility.t("upd_manual_download");
            } else {
                downloadUrl = optString(jar, "browser_download_url", "");
                state = State.AVAILABLE;
                statusMessage = "Update available";
            }
            RaveX.LOGGER.info("[UpdateManager] Update available: {} (current {})", latestVersion, getCurrentVersion());
        } else {
            downloadUrl = null;
            expectedSha256 = null;
            state = State.UP_TO_DATE;
            statusMessage = "";
        }
        // Stamp the check so the next launch does not hit the GitHub API again for 3 hours.
        writeCheckCache();
    }

    /* --------------------------------------------------------------- download */

    public CompletableFuture<Void> downloadUpdate() {
        String url = downloadUrl;
        if (url == null || url.isEmpty()) {
            errorMessage = ravex.utility.misc.LanguageUtility.t("upd_manual_download");
            state = State.ERROR;
            return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
        }
        if (!busy.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        state = State.DOWNLOADING;
        statusMessage = "Downloading...";
        errorMessage = null;
        downloadDone = 0L;
        downloadTotal = 0L;

        HttpRequest request = buildRequest(url);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenCompose(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("HTTP status " + response.statusCode()));
                    }
                    long declared = response.headers().firstValueAsLong("content-length").orElse(0L);
                    downloadTotal = declared;
                    return writeToFile(response.body());
                })
                .thenAccept(this::verifyDownloadedJar)
                .thenAccept(ignored -> {
                    state = State.DOWNLOADED;
                    statusMessage = "Ready to install";
                    updatable = true;
                })
                .exceptionally(ex -> {
                    errorMessage = describe(ex);
                    state = State.ERROR;
                    statusMessage = "";
                    RaveX.LOGGER.warn("[UpdateManager] Download failed: {}", errorMessage);
                    return null;
                })
                .whenComplete((ignored, err) -> busy.set(false));
    }

    private CompletableFuture<File> writeToFile(InputStream in) {
        return CompletableFuture.supplyAsync(() -> {
            File target = null;
            try (InputStream stream = in) {
                File dir = getUpdateDir();
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("Cannot create " + dir);
                }
                File tmp = File.createTempFile("ravex-update-", ".jar.tmp", dir);
                target = tmp;

                byte[] buffer = new byte[DOWNLOAD_BUFFER];
                try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
                    int read;
                    while ((read = stream.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloadDone += read;
                        if (downloadTotal <= 0L) downloadTotal = downloadDone;
                    }
                }

                File finalJar = new File(dir, "ravex-" + sanitize(latestVersion) + ".jar");
                if (finalJar.exists() && !finalJar.delete()) {
                    throw new IllegalStateException("Cannot replace " + finalJar.getName());
                }
                Files.move(tmp.toPath(), finalJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                stagedJar = finalJar;
                return finalJar;
            } catch (Exception e) {
                if (target != null && target.exists()) target.delete();
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private void verifyDownloadedJar(File jar) {
        if (jar == null || !jar.isFile()) {
            throw new IllegalStateException("Downloaded file is missing");
        }
        if (!isValidJar(jar)) {
            jar.delete();
            stagedJar = null;
            throw new IllegalStateException("Downloaded file is not a valid RaveX jar");
        }
        String expected = expectedSha256;
        if (expected != null && !expected.isBlank()) {
            String actual = sha256(jar);
            if (!actual.equalsIgnoreCase(expected.trim())) {
                jar.delete();
                stagedJar = null;
                throw new IllegalStateException("Checksum mismatch (expected " + expected.trim() + ", got " + actual + ")");
            }
        }
    }

    private static boolean isValidJar(File jar) {
        try (InputStream in = Files.newInputStream(jar.toPath())) {
            byte[] magic = new byte[4];
            int read = in.read(magic);
            if (read < 4) return false;
            boolean zip = magic[0] == 'P' && magic[1] == 'K'
                    && magic[2] == 3 && magic[3] == 4;
            if (!zip) return false;
        } catch (Exception e) {
            return false;
        }
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            return zip.getEntry("fabric.mod.json") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /* ---------------------------------------------------------------- install */

    public CompletableFuture<Void> installUpdate(boolean relaunch) {
        File jar = stagedJar;
        if (jar == null || !jar.isFile()) {
            errorMessage = ravex.utility.misc.LanguageUtility.t("upd_none_downloaded");
            state = State.ERROR;
            return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
        }
        String currentJarPath = resolveCurrentJarPath();
        if (currentJarPath == null) {
            errorMessage = ravex.utility.misc.LanguageUtility.t("upd_not_located");
            updatable = false;
            state = State.ERROR;
            return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
        }
        if (jar.getAbsolutePath().equalsIgnoreCase(currentJarPath)) {
            errorMessage = ravex.utility.misc.LanguageUtility.t("upd_already_in_place");
            return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
        }

        state = State.INSTALLING;
        statusMessage = "Installing...";
        errorMessage = null;

        return CompletableFuture.supplyAsync(() -> {
            ravex.update.UpdateInstaller.launch(jar.getAbsolutePath(), currentJarPath, relaunch);
            return null;
        }).thenRun(() -> {
            statusMessage = ravex.utility.misc.LanguageUtility.t("upd_finished");
        }).exceptionally(ex -> {
            errorMessage = describe(ex);
            state = State.ERROR;
            statusMessage = "";
            return null;
        });
    }

    /** Absolute path of the jar this mod is running from, or null in a dev environment. */
    public static String resolveCurrentJarPath() {
        try {
            String p = RaveX.getModJarPath();
            if (p != null && !p.isEmpty()) {
                File f = new File(p);
                if (f.isFile()) return f.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        try {
            File self = new File(UpdateManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (self.isFile() && self.getName().endsWith(".jar")) return self.getAbsolutePath();
        } catch (Exception ignored) {
        }
        return null;
    }

    /* ------------------------------------------------------------------ misc */

    public static File getGameDir() {
        try {
            return ravex.mcwrapper.MinecraftWrapper.getWrapper().getRaw().gameDirectory;
        } catch (Throwable t) {
            return new File(".");
        }
    }

    public static File getUpdateDir() {
        return new File(getGameDir(), "RaveX/update");
    }

    private HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RaveX-Client")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build();
    }

    private JsonObject parseRelease(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new IllegalStateException("GitHub returned HTTP " + response.statusCode());
        }
        JsonElement root = JsonParser.parseString(response.body());
        if (!root.isJsonObject()) throw new IllegalStateException("Unexpected release payload");
        return root.getAsJsonObject();
    }

    /**
 * Looks for a checksum published next to the jar (typically {@code ravex.jar.sha256}).
 * Returns null when the release does not ship one, in which case the download is only
 * validated structurally.
 */
private String findSha256Asset(JsonObject release, JsonObject jarAsset) {
    JsonArray assets = release.has("assets") && release.get("assets").isJsonArray()
            ? release.getAsJsonArray("assets") : new JsonArray();
        String jarName = jarAsset == null ? "" : optString(jarAsset, "name", "");
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            String name = optString(asset, "name", "").toLowerCase(Locale.ROOT);
            if (!name.contains("sha256") && !name.contains("sha-256")) continue;
            if (!jarName.isEmpty() && !name.contains(jarName.toLowerCase(Locale.ROOT))) continue;
            String body = optString(asset, "browser_download_url", "");
            if (body.isEmpty()) continue;
            String hash = fetchSmallText(body);
            if (hash != null) return hash;
        }
        return null;
    }

    private String fetchSmallText(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "RaveX-Client")
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)\\b([0-9a-f]{64})\\b")
                    .matcher(response.body());
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject pickJarAsset(JsonObject release) {
        JsonArray assets = release.has("assets") && release.get("assets").isJsonArray()
                ? release.getAsJsonArray("assets") : new JsonArray();
        JsonObject fallback = null;
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            String name = optString(asset, "name", "");
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jar")
                    && !lower.contains("sources")
                    && !lower.contains("slim")
                    && !lower.contains("javadoc")) {
                if (lower.startsWith("ravex")) return asset;
                if (fallback == null) fallback = asset;
            }
        }
        return fallback;
    }

    /**
     * Resolves the real version from a release payload.
     * The release title and the notes heading are authoritative; the tag is only a
     * last resort because {@code ravex149} cannot be inverted reliably.
 */
private static String readVersion(JsonObject release) {
        String fromName = versionFromLabel(optString(release, "name", ""));
        if (fromName != null) return fromName;
        String fromBody = versionFromNotesTitle(optString(release, "body", ""));
        if (fromBody != null) return fromBody;
        return versionFromTag(optString(release, "tag_name", ""));
    }

    public static String versionFromLabel(String label) {
        if (label == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)*)")
                .matcher(label);
        if (!m.find()) return null;
        String version = m.group(1);
        while (version.endsWith(".")) {
            version = version.substring(0, version.length() - 1);
        }
        return version.isEmpty() ? null : version;
    }

    /**
     * Best-effort inversion of the release tag format {@code ravex<digits>}.
     *
     * <p>Only tags of up to three digits can be reversed without ambiguity, which covers the
     * major.minor.patch versions this project uses. Longer tags are ambiguous (1.4.10 and 1.410
     * both produce {@code ravex1410}) so {@code null} is returned and the caller falls back to
     * the release title, which always carries the real version.
     */
    public static String versionFromTag(String tag) {
        if (tag == null) return null;
        String digits = tag.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return versionFromLabel(tag);
        if (digits.length() > 3) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0) sb.append('.');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    /** Extracts a version from a release body title, e.g. {@code # RaveX 1.4.9}. */
    public static String versionFromNotesTitle(String body) {
        if (body == null) return null;
        for (String raw : body.split("\\r?\\n")) {
            String line = raw.strip();
            if (line.startsWith("# ")) {
                String version = versionFromLabel(line);
                if (version != null) return version;
                break;
            }
        }
        return null;
    }

    /** Numeric-aware version compare. Returns >0 when {@code a} is newer than {@code b}. */
    public static int compareVersions(String a, String b) {
        String[] pa = split(a);
        String[] pb = split(b);
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            String x = i < pa.length ? pa[i] : "0";
            String y = i < pb.length ? pb[i] : "0";
            boolean xNumeric = x.matches("\\d+");
            boolean yNumeric = y.matches("\\d+");
            int result;
            if (xNumeric && yNumeric) {
                result = Integer.compare(x.length(), y.length());
                if (result == 0) result = x.compareTo(y);
            } else if (xNumeric != yNumeric) {
                // a released numeric component always outranks a pre-release qualifier
                result = xNumeric ? 1 : -1;
            } else {
                result = x.compareTo(y);
            }
            if (result != 0) return result > 0 ? 1 : -1;
        }
        return 0;
    }

    private static String[] split(String version) {
        if (version == null) return new String[]{"0"};
        String cleaned = version.trim().replaceAll("^[vV]", "");
        String[] parts = cleaned.split("[.\\-_+]");
        return parts.length == 0 ? new String[]{"0"} : parts;
    }

    private static String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^\\w.\\-]", "_");
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

    public static String sha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String describe(Throwable ex) {
        if (ex == null) return "Unknown error";
        Throwable cause = ex;
        while (cause.getCause() != null && (cause.getMessage() == null || cause.getMessage().isBlank())) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = cause.getClass().getSimpleName();
        return message;
    }

    /* ------------------------------------------------------------ last check */

    private static File getLastCheckFile() {
        return new File(getGameDir(), "RaveX/update/last_check.txt");
    }

    private boolean shouldSkipRecentCheck() {
        try {
            File f = getLastCheckFile();
            if (!f.exists()) return false;
            long age = Instant.now().toEpochMilli() - Files.getLastModifiedTime(f.toPath()).toMillis();
            if (age < LAST_CHECK_FILE_MAX_AGE) return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void writeCheckCache() {
        try {
            File f = getLastCheckFile();
            Path parent = f.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(f.toPath(), getCurrentVersion());
        } catch (Exception ignored) {
        }
    }
}