package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.RaveX;
import ravex.modules.Category;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TextureLoader {
    private static final Map<Identifier, AbstractTexture> loaded = new HashMap<>();
    private static boolean markerLoaded = false;

    private static final String NS = "ravex";
    private static final String GUI_PREFIX = "gui/";
    private static final String CLASSPATH_PREFIX = "/assets/ravex/textures/";

    private static final int ICON_SIZE = 32;
    private static final int MODULE_ICON_SIZE = 16;
    private static final String MODULES_SUBDIR = "modules/";

    private static final Map<String, Identifier> MODULE_ICONS = new HashMap<>();
    private static final HashSet<String> MODULE_ICON_MISSING = new HashSet<>();

    public static final Identifier SEARCH = id("search");
    public static final Identifier SEARCH_WHITE = id("search_white");
    public static final Identifier SETTINGS = id("settings");
    public static final Identifier SETTINGS_WHITE = id("settings_white");
    public static final Identifier CIRCLE = id("circle");
    public static final Identifier CIRCLE_WHITE = id("circle_white");
    public static final Identifier TRACK = id("track_white");
    public static final Identifier SWITCHER = id("switcher");
    private static final Identifier FALLBACK = id("misc");
    private static final Map<Category, Identifier> CAT_IDS = new HashMap<>();
    private static final Map<Category, Identifier> CAT_WHITE_IDS = new HashMap<>();

    public static final Identifier FEMBOY = Identifier.fromNamespaceAndPath(NS, "img/femboy");
    public static final Identifier WYPHER1 = Identifier.fromNamespaceAndPath(NS, "img/wypher1");
    public static final Identifier BOYKGUN = Identifier.fromNamespaceAndPath(NS, "img/boykgun");
    public static final Identifier CUTIE = Identifier.fromNamespaceAndPath(NS, "img/cutie");
    public static final Identifier KISS = Identifier.fromNamespaceAndPath(NS, "img/kiss");
    public static final Identifier LAYING = Identifier.fromNamespaceAndPath(NS, "img/laying");
    public static final Identifier LICKING = Identifier.fromNamespaceAndPath(NS, "img/licking");
    public static final Identifier PILLOW = Identifier.fromNamespaceAndPath(NS, "img/pillow");
    public static final Identifier CUTIEEEE = Identifier.fromNamespaceAndPath(NS, "img/cutieeee");
    public static final Identifier CUTIEMONSTER = Identifier.fromNamespaceAndPath(NS, "img/cutiemonster");
    public static final Identifier FURIK = Identifier.fromNamespaceAndPath(NS, "img/furik");
    public static final Identifier GODOFCODING = Identifier.fromNamespaceAndPath(NS, "img/godofcoding");

    public static final Identifier TERRYDAVIS = Identifier.fromNamespaceAndPath(NS, "img/terrydavis");
    public static final Identifier MARKER = Identifier.fromNamespaceAndPath(NS, "wp_marker");
    public static final Identifier CAPTURE = Identifier.fromNamespaceAndPath(NS, "capture");
    public static final Identifier FIREFLY = Identifier.fromNamespaceAndPath(NS, "firefly");
    public static final Identifier SOLID_CIRCLE = Identifier.fromNamespaceAndPath(NS, "solid_circle");
    public static final Identifier ENABLE = Identifier.fromNamespaceAndPath(NS, "enable");
    public static final Identifier DISABLE = Identifier.fromNamespaceAndPath(NS, "disable");

    public static final Identifier HUD_COORDS = hudId("coords");
    public static final Identifier HUD_TPS = hudId("tps");
    public static final Identifier HUD_INDICATORS = hudId("indicators");
    public static final Identifier HUD_CURRENCY = hudId("currency");
    public static final Identifier HUD_MEDIA = hudId("media");
    public static final Identifier HUD_COOLDOWN = hudId("cooldown");
    public static final Identifier HUD_INVENTORY = hudId("inventory");
    public static final Identifier HUD_CHAT = hudId("chat");
    public static final Identifier HUD_SERVERBRAND = hudId("serverbrand");
    public static final Identifier HUD_WAYPOINT = hudId("waypoint");
    public static final Identifier HUD_FPS = hudId("fps");
    public static final Identifier HUD_ARRAYLIST = hudId("arraylist");

    public static final Identifier HUD_COORDS_WHITE = hudWhiteId("coords");
    public static final Identifier HUD_TPS_WHITE = hudWhiteId("tps");
    public static final Identifier HUD_INDICATORS_WHITE = hudWhiteId("indicators");
    public static final Identifier HUD_CURRENCY_WHITE = hudWhiteId("currency");
    public static final Identifier HUD_MEDIA_WHITE = hudWhiteId("media");
    public static final Identifier HUD_COOLDOWN_WHITE = hudWhiteId("cooldown");
    public static final Identifier HUD_INVENTORY_WHITE = hudWhiteId("inventory");
    public static final Identifier HUD_CHAT_WHITE = hudWhiteId("chat");
    public static final Identifier HUD_SERVERBRAND_WHITE = hudWhiteId("serverbrand");
    public static final Identifier HUD_WAYPOINT_WHITE = hudWhiteId("waypoint");
    public static final Identifier HUD_FPS_WHITE = hudWhiteId("fps");
    public static final Identifier HUD_ARRAYLIST_WHITE = hudWhiteId("arraylist");

    static {
        for (Category cat : Category.values()) {
            String name = cat.name().toLowerCase();
            CAT_IDS.put(cat, id(name));
            CAT_WHITE_IDS.put(cat, id(name + "_white"));
        }
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(NS, GUI_PREFIX + name);
    }

    private static Identifier hudId(String name) {
        return Identifier.fromNamespaceAndPath(NS, "hud/" + name);
    }

    private static Identifier hudWhiteId(String name) {
        return Identifier.fromNamespaceAndPath(NS, "hud_white/" + name);
    }

    private static NativeImage downscaleTo(NativeImage image, int maxDim) {
        int sw = image.getWidth();
        int sh = image.getHeight();
        if (sw <= maxDim && sh <= maxDim) return image;
        float scale = Math.min((float) maxDim / sw, (float) maxDim / sh);
        int tw = Math.max(1, Math.round(sw * scale));
        int th = Math.max(1, Math.round(sh * scale));
        NativeImage dst = new NativeImage(tw, th, false);

        for (int dy = 0; dy < th; dy++) {
            int sy0 = dy * sh / th;
            int sy1 = Math.min(sh, (dy + 1) * sh / th);
            for (int dx = 0; dx < tw; dx++) {
                int sx0 = dx * sw / tw;
                int sx1 = Math.min(sw, (dx + 1) * sw / tw);

                int maxA = 0, sumA = 0, opaqueCount = 0, count = 0;
                for (int sy = sy0; sy < sy1; sy++) {
                    for (int sx = sx0; sx < sx1; sx++) {
                        int a = (image.getPixel(sx, sy) >> 24) & 0xFF;
                        if (a > maxA) maxA = a;
                        if (a > 0) opaqueCount++;
                        sumA += a;
                        count++;
                    }
                }
                float coverage = (float) opaqueCount / count;
                float blend = Math.min(1.0f, coverage * 2.5f);
                int avgA = sumA / count;
                int resultA = Math.round(avgA * (1.0f - blend) + maxA * blend);
                dst.setPixel(dx, dy, (resultA << 24) | 0xFFFFFF);
            }
        }

        image.close();
        return dst;
    }

    private static NativeImage removeBackground(NativeImage image, String name) {
        if ("boykgun".equals(name) || "cutie".equals(name)) {
            return removeWhiteBackground(image);
        }
        return removeBackground(image);
    }

    private static NativeImage removeWhiteBackground(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= 0 || h <= 0) return image;
        int threshold = 230;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = image.getPixel(x, y);
                if (((rgba >> 24) & 0xFF) > 0) {
                    int r = (rgba >> 16) & 0xFF;
                    int g = (rgba >> 8) & 0xFF;
                    int b = rgba & 0xFF;
                    if (r >= threshold && g >= threshold && b >= threshold) {
                        image.setPixel(x, y, 0);
                    }
                }
            }
        }
        return image;
    }

    private static NativeImage removeBackground(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= 0 || h <= 0) return image;

        int bgR = 255, bgG = 255, bgB = 255;
        int sampleSize = Math.min(5, Math.min(w / 2, h / 2));
        int totalR = 0, totalG = 0, totalB = 0, samples = 0;
        int[][] corners = {{0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}};
        for (int[] corner : corners) {
            int cx = corner[0], cy = corner[1];
            int sx = cx == 0 ? 0 : w - sampleSize;
            int sy = cy == 0 ? 0 : h - sampleSize;
            for (int x = sx; x < sx + sampleSize; x++) {
                for (int y = sy; y < sy + sampleSize; y++) {
                    int c = image.getPixel(x, y);
                    if (((c >> 24) & 0xFF) > 0) {
                        totalR += (c >> 16) & 0xFF;
                        totalG += (c >> 8) & 0xFF;
                        totalB += c & 0xFF;
                        samples++;
                    }
                }
            }
        }
        if (samples > 0) {
            bgR = totalR / samples;
            bgG = totalG / samples;
            bgB = totalB / samples;
        }

        int tolerance = 40;
        boolean[][] bg = new boolean[w][h];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();

        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                if (!bg[x][y] && isClose(image.getPixel(x, y), bgR, bgG, bgB, tolerance)) {
                    bg[x][y] = true;
                    queue.addLast(new int[]{x, y});
                }
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x : new int[]{0, w - 1}) {
                if (!bg[x][y] && isClose(image.getPixel(x, y), bgR, bgG, bgB, tolerance)) {
                    bg[x][y] = true;
                    queue.addLast(new int[]{x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst();
            int px = p[0], py = p[1];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = px + dx, ny = py + dy;
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h || bg[nx][ny]) continue;
                    if (isClose(image.getPixel(nx, ny), bgR, bgG, bgB, tolerance)) {
                        bg[nx][ny] = true;
                        queue.addLast(new int[]{nx, ny});
                    }
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (bg[x][y]) {
                    image.setPixel(x, y, 0);
                }
            }
        }
        return image;
    }

    private static boolean isClose(int rgba, int bgR, int bgG, int bgB, int tolerance) {
        int a = (rgba >> 24) & 0xFF;
        if (a == 0) return false;
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = rgba & 0xFF;
        return Math.abs(r - bgR) <= tolerance
            && Math.abs(g - bgG) <= tolerance
            && Math.abs(b - bgB) <= tolerance;
    }

    public static Identifier getCategoryTexture(Category cat) {
        if (cat == Category.CUSTOM) cat = Category.MISC;
        Identifier id = CAT_IDS.get(cat);
        if (ensureLoaded(id, cat.name().toLowerCase())) return id;
        if (ensureLoaded(FALLBACK, "misc")) return FALLBACK;
        return null;
    }

    public static Identifier getSearchTexture() {
        return ensureLoaded(SEARCH, "search") ? SEARCH : null;
    }

    public static Identifier getSettingsTexture() {
        return ensureLoaded(SETTINGS, "settings") ? SETTINGS : null;
    }

    public static final Identifier PALETTE = Identifier.fromNamespaceAndPath(NS, "hud/palette");

    public static Identifier getPaletteTexture() {
        if (loaded.containsKey(PALETTE)) return PALETTE;
        String resourcePath = "/assets/ravex/textures/hud/palette.png";
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[TextureLoader] Palette icon not found: {}", resourcePath);
                return null;
            }
            NativeImage image = NativeImage.read(stream);
            image = downscaleTo(image, ICON_SIZE);
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(PALETTE, tex);
            loaded.put(PALETTE, tex);
            return PALETTE;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load palette icon: {}", e.getMessage());
            return null;
        }
    }

    public static Identifier getSettingsWhiteTexture() {
        if (!loaded.containsKey(SETTINGS_WHITE)) {
            if (!ensureLoaded(SETTINGS, "settings")) return null;
            try (InputStream stream = TextureLoader.class.getResourceAsStream(CLASSPATH_PREFIX + "settings.png")) {
                if (stream == null) return null;
                NativeImage image = NativeImage.read(stream);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixel(x, y);
                        int a = (rgba >> 24) & 0xFF;
                        image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                    }
                }
                image = downscaleTo(image, ICON_SIZE);
                AbstractTexture tex = createLinearTexture(image);
                Minecraft.getInstance().getTextureManager().register(SETTINGS_WHITE, tex);
                loaded.put(SETTINGS_WHITE, tex);
            } catch (Exception e) {
                RaveX.LOGGER.warn("[TextureLoader] Failed to load white settings: {}", e.getMessage());
                return null;
            }
        }
        return SETTINGS_WHITE;
    }

    public static Identifier getCategoryTextureWhite(Category cat) {
        if (cat == Category.CUSTOM) cat = Category.MISC;
        Identifier whiteId = CAT_WHITE_IDS.get(cat);
        if (whiteId == null) return getCategoryTexture(cat);
        if (loaded.containsKey(whiteId)) return whiteId;
        Identifier originalId = CAT_IDS.get(cat);
        if (!ensureLoaded(originalId, cat.name().toLowerCase())) return null;
        Identifier result = makeWhiteCopy(originalId, whiteId, cat.name().toLowerCase());
        return result != null ? result : originalId;
    }

    public static Identifier getSearchWhiteTexture() {
        if (loaded.containsKey(SEARCH_WHITE)) return SEARCH_WHITE;
        if (!ensureLoaded(SEARCH, "search")) return null;
        Identifier result = makeWhiteCopy(SEARCH, SEARCH_WHITE, "search");
        return result != null ? result : SEARCH;
    }

    private static Identifier makeWhiteCopy(Identifier sourceId, Identifier targetId, String name) {
        try (InputStream stream = TextureLoader.class.getResourceAsStream(CLASSPATH_PREFIX + name + ".png")) {
            if (stream == null) return null;
            NativeImage image = NativeImage.read(stream);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgba = image.getPixel(x, y);
                    int a = (rgba >> 24) & 0xFF;
                    image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                }
            }
            image = downscaleTo(image, ICON_SIZE);
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(targetId, tex);
            loaded.put(targetId, tex);
            return targetId;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load white {}: {}", name, e.getMessage());
            return null;
        }
    }

    public static Identifier getCircleWhiteTexture() {
        if (!loaded.containsKey(CIRCLE_WHITE)) {
            if (!ensureLoaded(CIRCLE, "circle")) return null;
            try (InputStream stream = TextureLoader.class.getResourceAsStream(CLASSPATH_PREFIX + "circle.png")) {
                if (stream == null) return null;
                NativeImage image = NativeImage.read(stream);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixel(x, y);
                        int a = (rgba >> 24) & 0xFF;
                        image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                    }
                }
                image = downscaleTo(image, ICON_SIZE);
                AbstractTexture tex = createLinearTexture(image);
                Minecraft.getInstance().getTextureManager().register(CIRCLE_WHITE, tex);
                loaded.put(CIRCLE_WHITE, tex);
            } catch (Exception e) {
                RaveX.LOGGER.warn("[TextureLoader] Failed to load white circle: {}", e.getMessage());
                return null;
            }
        }
        return CIRCLE_WHITE;
    }

    public static Identifier getTrackWhiteTexture() {
        if (!loaded.containsKey(TRACK)) {
            int w = 34;
            int h = 16;
            NativeImage image = new NativeImage(w, h, false);
            float r = h / 2.0f;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float px = x + 0.5f;
                    float py = y + 0.5f;
                    float cx = Math.max(r, Math.min(w - r, px));
                    float cy = Math.max(r, Math.min(h - r, py));
                    float dx = px - cx;
                    float dy = py - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy) - r;
                    float alpha = Math.max(0, Math.min(1, 0.5f - dist));
                    int a = Math.round(alpha * 255);
                    if (a > 0) {
                        image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                    }
                }
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(TRACK, tex);
            loaded.put(TRACK, tex);
        }
        return TRACK;
    }

    public static Identifier getSolidCircleTexture() {
        if (!loaded.containsKey(SOLID_CIRCLE)) {
            int size = 64;
            NativeImage image = new NativeImage(size, size, false);
            float cx = size / 2.0f;
            float cy = size / 2.0f;
            float r = size / 2.0f - 1.0f;
            float r2 = r * r;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = x + 0.5f - cx;
                    float dy = y + 0.5f - cy;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= r2) {
                        float dist = (float) Math.sqrt(d2);
                        float delta = r - dist;
                        int alpha = Math.max(0, Math.min(255, Math.round(Math.clamp(delta * 2.0f, 0.0f, 1.0f) * 255)));
                        image.setPixel(x, y, (alpha << 24) | 0xFFFFFF);
                    }
                }
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(SOLID_CIRCLE, tex);
            loaded.put(SOLID_CIRCLE, tex);
        }
        return SOLID_CIRCLE;
    }

    public static Identifier getSwitcherTexture() {
        if (!loaded.containsKey(SWITCHER)) {
            try (InputStream stream = TextureLoader.class.getResourceAsStream(CLASSPATH_PREFIX + "switcher.png")) {
                if (stream == null) {
                    RaveX.LOGGER.warn("[TextureLoader] Switcher texture not found");
                    return null;
                }
                NativeImage image = NativeImage.read(stream);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixel(x, y);
                        int a = (rgba >> 24) & 0xFF;
                        int r = (rgba >> 16) & 0xFF;
                        int g = (rgba >> 8) & 0xFF;
                        int b = rgba & 0xFF;
                        if (r > 200 && g > 200 && b > 200) {
                            image.setPixel(x, y, 0);
                        } else {
                            image.setPixel(x, y, (a << 24) | 0x808080);
                        }
                    }
                }
                image = downscaleTo(image, ICON_SIZE);
                AbstractTexture tex = createLinearTexture(image);
                Minecraft.getInstance().getTextureManager().register(SWITCHER, tex);
                loaded.put(SWITCHER, tex);
            } catch (Exception e) {
                RaveX.LOGGER.warn("[TextureLoader] Failed to load switcher: {}", e.getMessage());
                return null;
            }
        }
        return SWITCHER;
    }

    private static Identifier tintedParticleId(String name) {
        return Identifier.fromNamespaceAndPath(NS, "tinted/" + name.toLowerCase());
    }

    public static Identifier getParticleTexture(String name, int color) {
        name = name.toLowerCase();
        Identifier tintedId = tintedParticleId(name);
        if (loaded.containsKey(tintedId) && lastTintedColor.getOrDefault(tintedId, -1) == color) {
            return tintedId;
        }

        String resPath = "/assets/ravex/particles/" + name + ".png";
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resPath)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[TextureLoader] Particle stream NULL: {}", resPath);
                Identifier baseId = Identifier.fromNamespaceAndPath(NS, "particles/" + name);
                if (loaded.containsKey(baseId)) return baseId;
                return null;
            }
            NativeImage image = NativeImage.read(stream);
            int cR = (color >> 16) & 0xFF;
            int cG = (color >> 8) & 0xFF;
            int cB = color & 0xFF;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgba = image.getPixel(x, y);
                    int a = (rgba >> 24) & 0xFF;
                    image.setPixel(x, y, (a << 24) | (cR << 16) | (cG << 8) | cB);
                }
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(tintedId, tex);
            loaded.put(tintedId, tex);
            lastTintedColor.put(tintedId, color);
            return tintedId;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load particle {}: {}", name, e.getMessage());
            Identifier baseId = Identifier.fromNamespaceAndPath(NS, "particles/" + name);
            if (loaded.containsKey(baseId)) return baseId;
            return null;
        }
    }

    private static final Map<Identifier, Integer> lastTintedColor = new HashMap<>();

    public static void preloadAll() {
        ensureLoaded(SEARCH, "search");
        ensureLoaded(SETTINGS, "settings");
        getSettingsWhiteTexture();
        getCircleWhiteTexture();
        getSearchWhiteTexture();
        getTrackWhiteTexture();
        getSwitcherTexture();
        for (Map.Entry<Category, Identifier> e : CAT_IDS.entrySet()) {
            if (e.getKey() == Category.CUSTOM) continue;
            ensureLoaded(e.getValue(), e.getKey().name().toLowerCase());
            getCategoryTextureWhite(e.getKey());
        }
        ensureLoaded(FEMBOY, "femboy");
        ensureLoaded(WYPHER1, "wypher1");
        ensureLoaded(BOYKGUN, "boykgun");
        ensureLoaded(CUTIE, "cutie");
        ensureLoaded(KISS, "kiss");
        ensureLoaded(LAYING, "laying");
        ensureLoaded(LICKING, "licking");
        ensureLoaded(PILLOW, "pillow");
        ensureLoaded(CUTIEEEE, "cutieeee");
        ensureLoaded(CUTIEMONSTER, "cutiemonster");
        ensureLoaded(FURIK, "furik");
        ensureLoaded(GODOFCODING, "godofcoding");
        ensureLoaded(TERRYDAVIS, "terrydavis");
        ensureLoaded3D(CAPTURE, "capture");
        ensureLoaded3D(FIREFLY, "firefly");
        ensureLoaded3D(ENABLE, "enable");
        ensureLoaded3D(DISABLE, "disable");
        getSolidCircleTexture();
        try (InputStream stream = TextureLoader.class.getResourceAsStream("/assets/ravex/textures/marker.png")) {
            if (stream != null) {
                NativeImage image = NativeImage.read(stream);
                AbstractTexture tex = createLinearTexture(image);
                Minecraft.getInstance().getTextureManager().register(MARKER, tex);
                loaded.put(MARKER, tex);
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load marker: {}", e.getMessage());
        }

        for (String hudName : new String[]{"coords", "tps", "indicators", "currency", "media", "cooldown", "inventory", "chat", "serverbrand", "waypoint", "fps", "arraylist"}) {
            getHudIconWhite(hudName);
        }
    }

    public static Identifier getMarkerTexture(int color) {
        Identifier tintedId = Identifier.fromNamespaceAndPath(NS, "wp_marker_tinted");
        if (loaded.containsKey(tintedId) && lastTintedColor.getOrDefault(tintedId, -1) == color) {
            return tintedId;
        }

        if (!loaded.containsKey(MARKER)) {
            try (InputStream stream = TextureLoader.class.getResourceAsStream("/assets/ravex/textures/marker.png")) {
                if (stream != null) {
                    NativeImage image = NativeImage.read(stream);
                    AbstractTexture tex = createLinearTexture(image);
                    Minecraft.getInstance().getTextureManager().register(MARKER, tex);
                    loaded.put(MARKER, tex);
                }
            } catch (Exception e) {
                RaveX.LOGGER.warn("[TextureLoader] Failed to load marker base: {}", e.getMessage());
            }
        }

        try (InputStream stream = TextureLoader.class.getResourceAsStream("/assets/ravex/textures/marker.png")) {
            if (stream == null) return MARKER;
            NativeImage image = NativeImage.read(stream);
            int cR = (color >> 16) & 0xFF;
            int cG = (color >> 8) & 0xFF;
            int cB = color & 0xFF;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgba = image.getPixel(x, y);
                    int a = (rgba >> 24) & 0xFF;
                    image.setPixel(x, y, (a << 24) | (cR << 16) | (cG << 8) | cB);
                }
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(tintedId, tex);
            loaded.put(tintedId, tex);
            lastTintedColor.put(tintedId, color);
            return tintedId;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to tint marker: {}", e.getMessage());
            return MARKER;
        }
    }

    public static Identifier getModuleIcon(String moduleKey) {
        if (MODULE_ICON_MISSING.contains(moduleKey)) return null;
        Identifier cached = MODULE_ICONS.get(moduleKey);
        if (cached != null) return cached;
        String fileName = moduleKey.toLowerCase().replaceAll("[^a-z0-9/._-]", "");
        if (fileName.isEmpty()) {
            MODULE_ICON_MISSING.add(moduleKey);
            return null;
        }
        Identifier id = Identifier.fromNamespaceAndPath(NS, GUI_PREFIX + MODULES_SUBDIR + fileName);
        if (ensureModuleIcon(id, fileName)) {
            MODULE_ICONS.put(moduleKey, id);
            return id;
        }
        MODULE_ICON_MISSING.add(moduleKey);
        return null;
    }

    public static Identifier getHudIcon(String name) {
        Identifier id = hudId(name);
        if (loaded.containsKey(id)) return id;
        if (ensureHudIcon(id, name)) return id;
        return null;
    }

    public static Identifier getHudIconWhite(String name) {
        Identifier colored = getHudIcon(name);
        if (colored == null) return null;
        Identifier whiteId = Identifier.fromNamespaceAndPath(NS, "hud_white/" + name);
        if (loaded.containsKey(whiteId)) return whiteId;
        Identifier result = makeWhiteCopy(colored, whiteId, "hud/" + name);
        return result != null ? result : colored;
    }

    private static boolean ensureHudIcon(Identifier id, String name) {
        if (loaded.containsKey(id)) return true;
        String resourcePath = "/assets/ravex/textures/hud/" + name + ".png";
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[TextureLoader] HUD icon not found: {}", resourcePath);
                return false;
            }
            NativeImage image = NativeImage.read(stream);
            image = downscaleTo(image, 16);
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            loaded.put(id, tex);
            return true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load HUD icon {}: {}", name, e.getMessage());
            return false;
        }
    }

    private static boolean ensureModuleIcon(Identifier guiId, String fileName) {
        if (loaded.containsKey(guiId)) return true;
        String resourcePath = CLASSPATH_PREFIX + MODULES_SUBDIR + fileName + ".png";
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return false;
            NativeImage image = NativeImage.read(stream);
            removeBackground(image);
            image = downscaleTo(image, MODULE_ICON_SIZE);
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(guiId, tex);
            loaded.put(guiId, tex);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean ensureLoaded(Identifier guiId, String name) {
        if (loaded.containsKey(guiId)) return true;

        String resourcePath;
        if (guiId.getPath().startsWith("companion/") || guiId.getPath().startsWith("img/") || guiId.getPath().startsWith("particles/")) {
            resourcePath = "/assets/ravex/" + guiId.getPath() + ".png";
        } else {
            resourcePath = CLASSPATH_PREFIX + name + ".png";
        }

        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[TextureLoader] Resource not found on classpath: {}", resourcePath);
                return false;
            }
            NativeImage image = NativeImage.read(stream);
            boolean isImage = guiId.getPath().startsWith("img/");
            if (isImage) {
                removeBackground(image, name);
            } else {
                image = downscaleTo(image, ICON_SIZE);
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(guiId, tex);
            loaded.put(guiId, tex);
            return true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load {}: {}", guiId, e.getMessage());
            return false;
        }
    }

    private static boolean ensureLoaded3D(Identifier guiId, String name) {
        if (loaded.containsKey(guiId)) return true;
        String resourcePath = CLASSPATH_PREFIX + name + ".png";
        try (InputStream stream = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[TextureLoader] 3D Resource not found: {}", resourcePath);
                return false;
            }
            NativeImage image;
            if (name.equals("firefly")) {
                int width = 64;
                int height = 64;
                image = new NativeImage(width, height, true);
                float centerX = width / 2.0f;
                float centerY = height / 2.0f;
                float maxRadius = width / 2.0f;

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        float dx = x - centerX + 0.5f;
                        float dy = y - centerY + 0.5f;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);

                        float factor = dist / maxRadius;
                        int alpha = 0;
                        if (factor < 1.0f) {
                            alpha = (int) (255.0f * Math.exp(-3.5f * factor * factor));
                            if (alpha < 0) alpha = 0;
                            if (alpha > 255) alpha = 255;
                        }
                        image.setPixel(x, y, (alpha << 24) | 0xFFFFFF);
                    }
                }
            } else {
                image = NativeImage.read(stream);
            }
            AbstractTexture tex = createLinearTexture(image);
            Minecraft.getInstance().getTextureManager().register(guiId, tex);
            loaded.put(guiId, tex);
            return true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load 3D {}: {}", guiId, e.getMessage());
            return false;
        }
    }

    private static AbstractTexture createNearestTexture(NativeImage image) {
        DynamicTexture tex = new DynamicTexture(() -> "nearest_icon", image);
        try {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            for (Field f : AbstractTexture.class.getDeclaredFields()) {
                if (GpuSampler.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(tex, sampler);
                    break;
                }
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to set nearest sampler: {}", e.getMessage());
        }
        return tex;
    }

    private static AbstractTexture createLinearTexture(NativeImage image) {
        DynamicTexture tex = new DynamicTexture(() -> "linear_icon", image);
        try {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            for (Field f : AbstractTexture.class.getDeclaredFields()) {
                if (GpuSampler.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(tex, sampler);
                    break;
                }
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to set linear sampler: {}", e.getMessage());
        }
        return tex;
    }
}
