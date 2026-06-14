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
    private static final Identifier FALLBACK = id("misc");
    private static final Map<Category, Identifier> CAT_IDS = new HashMap<>();

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
            CAT_IDS.put(cat, id(cat.name().toLowerCase()));
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

    public static void preloadAll() {
        ensureLoaded(SEARCH, "search");
        for (Map.Entry<Category, Identifier> e : CAT_IDS.entrySet()) {
            ensureLoaded(e.getValue(), e.getKey().name().toLowerCase());
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
