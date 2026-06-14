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
import java.util.Map;

public class TextureLoader {
    private static final Map<Identifier, AbstractTexture> loaded = new HashMap<>();

    private static final String NS = "ravex";
    private static final String GUI_PREFIX = "gui/";
    private static final String CLASSPATH_PREFIX = "/assets/ravex/textures/";

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

    public static Identifier getCategoryTexture(Category cat) {
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
                        if (a > 0) {
                            int gray = 0xA0A0A0;
                            image.setPixel(x, y, (a << 24) | gray);
                        }
                    }
                }
                AbstractTexture tex = createLinearTexture(image);
                image.close();
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
                    if (a > 0) {
                        image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                    }
                }
            }
            AbstractTexture tex = createLinearTexture(image);
            image.close();
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
                        if (a > 0) {
                            image.setPixel(x, y, (a << 24) | 0xA0A0A0);
                        }
                    }
                }
                AbstractTexture tex = createLinearTexture(image);
                image.close();
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
            image.close();
            Minecraft.getInstance().getTextureManager().register(TRACK, tex);
            loaded.put(TRACK, tex);
        }
        return TRACK;
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
                AbstractTexture tex = createLinearTexture(image);
                image.close();
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
        RaveX.LOGGER.info("[TextureLoader] getParticleTexture: name={}, resPath={}", name, resPath);
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
            image.close();
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
    }

    private static boolean ensureLoaded(Identifier guiId, String name) {
        if (loaded.containsKey(guiId)) return true;

        String resourcePath;
        if (guiId.getPath().startsWith("companion/")) {
            resourcePath = "/assets/ravex/" + guiId.getPath() + ".png";
        } else if (guiId.getPath().startsWith("img/")) {
            resourcePath = "/assets/ravex/" + guiId.getPath() + ".png";
        } else if (guiId.getPath().startsWith("particles/")) {
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



            AbstractTexture tex = createLinearTexture(image);
            image.close();
            Minecraft.getInstance().getTextureManager().register(guiId, tex);
            loaded.put(guiId, tex);
            RaveX.LOGGER.info("[TextureLoader] Loaded {} ({}x{}) from classpath", guiId, image.getWidth(), image.getHeight());
            return true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to load {}: {}", guiId, e.getMessage());
            return false;
        }
    }

    private static AbstractTexture createLinearTexture(NativeImage image) {
        DynamicTexture tex = new DynamicTexture(() -> "linear_icon", image);
        try {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            Field f = AbstractTexture.class.getDeclaredField("sampler");
            f.setAccessible(true);
            f.set(tex, sampler);
        } catch (Exception e) {
            RaveX.LOGGER.warn("[TextureLoader] Failed to set linear sampler: {}", e.getMessage());
        }
        return tex;
    }
}
