package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.nativelib.NativeLibraryUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;

public class FontSdfUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger("ravex/sdf");
    private static final NativeLibraryUtility LIB = NativeLibraryUtility.of("font");
    private static volatile boolean available = false;
    private static volatile long nextRetryMs = 0;
    private static final long RETRY_DELAY_MS = 30000;

    private static final int ATLAS_WIDTH = 1024;
    private static final int ATLAS_MAX_HEIGHT = 2048;
    private static final int PAD = 8;
    private static final float TARGET_PX = 12.0f;
    private static final int RASTER_PX = 64;
    private static final int TABLE_SIZE = 2048;

    private static final Map<String, SdfFont> FONTS = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    public static final class SdfFont {
        public final Identifier textureId;
        public final int atlasW;
        public final int atlasH;
        public final float emPx;
        public final int[][] table;
        public final int spaceAdvance;

        public SdfFont(Identifier textureId, int atlasW, int atlasH, float emPx, int[][] table, int spaceAdvance) {
            this.textureId = textureId;
            this.atlasW = atlasW;
            this.atlasH = atlasH;
            this.emPx = emPx;
            this.table = table;
            this.spaceAdvance = spaceAdvance;
        }
    }

    private static final class Cell {
        int codepoint;
        int w;
        int h;
        int xoff;
        int yoff;
        int advance;
        BufferedImage image;
    }

    private static native int[] nativeBake(byte[] ttfData, int pixelHeight);

    private static native byte[] nativePixels();

    public static boolean isAvailable() {
        if (available) return true;
        long now = System.currentTimeMillis();
        if (now < nextRetryMs) return false;
        nextRetryMs = now + RETRY_DELAY_MS;
        try {
            available = LIB.load();
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }

    public static int bakeHeight(float fontSize) {
        return Math.max(6, Math.round(TARGET_PX * fontSize));
    }

    private static String cacheKey(String key, int bakePx) {
        return key + "@" + bakePx;
    }

    public static SdfFont peek(String key, float fontSize) {
        return FONTS.get(cacheKey(key, bakeHeight(fontSize)));
    }

    public static SdfFont getFont(String key, float fontSize) {
        int bakePx = bakeHeight(fontSize);
        String ck = cacheKey(key, bakePx);
        SdfFont cached = FONTS.get(ck);
        if (cached != null) return cached;
        if (FAILED.contains(key)) return null;
        SdfFont baked = bake(key, fontSize);
        if (baked != null) FONTS.put(ck, baked);
        else FAILED.add(key);
        return baked;
    }

    private static byte[] readTtf(String key) {
        try (InputStream in = FontSdfUtility.class.getResourceAsStream("/assets/ravex/font/" + key + ".ttf")) {
            if (in == null) return null;
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1 << 20);
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) buf.write(chunk, 0, read);
            return buf.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static SdfFont bake(String key, float fontSize) {
        int bakePx = bakeHeight(fontSize);
        float em = TARGET_PX * fontSize;
        byte[] ttf = readTtf(key);
        if (ttf == null) return null;
        SdfFont f = ftBake(key, ttf, bakePx, em);
        if (f != null) return f;
        if (isAvailable()) {
            SdfFont n = nativeBakeFont(key, ttf, bakePx, em);
            if (n != null) return n;
        }
        return awtBake(key, ttf, bakePx, em);
    }

    private static SdfFont nativeBakeFont(String key, byte[] ttf, int bakePx, float em) {
        int[] metrics;
        byte[] pixels;
        try {
            metrics = nativeBake(ttf, bakePx);
            if (metrics == null || metrics.length < 4) return null;
            pixels = nativePixels();
            if (pixels == null) return null;
        } catch (Throwable e) {
            available = false;
            return null;
        }
        int atlasW = metrics[0];
        int atlasH = metrics[1];
        int count = metrics[3];
        if (atlasW <= 0 || atlasH <= 0 || em <= 0 || count <= 0) return null;
        if (pixels.length < atlasW * atlasH * 4) return null;
        int[][] table = new int[TABLE_SIZE][];
        int spaceAdvance = Math.round(em / 4.0f);
        int off = 4;
        for (int i = 0; i < count; i++) {
            if (off + 10 > metrics.length) break;
            int cp = metrics[off];
            int[] g = new int[9];
            g[0] = metrics[off + 1];
            g[1] = metrics[off + 2];
            g[2] = metrics[off + 3];
            g[3] = metrics[off + 4];
            g[4] = metrics[off + 5];
            g[5] = metrics[off + 6];
            g[6] = metrics[off + 7];
            g[7] = metrics[off + 8];
            g[8] = metrics[off + 9] * 4;
            if (cp >= 0 && cp < TABLE_SIZE) table[cp] = g;
            if (cp == 32) spaceAdvance = g[8];
            off += 10;
        }
        NativeImage image = new NativeImage(atlasW, atlasH, false);
        long alphaSum = 0;
        for (int y = 0; y < atlasH; y++) {
            for (int x = 0; x < atlasW; x++) {
                int p = (y * atlasW + x) * 4;
                int r = pixels[p] & 0xFF;
                int g = pixels[p + 1] & 0xFF;
                int b = pixels[p + 2] & 0xFF;
                int a = pixels[p + 3] & 0xFF;
                alphaSum += a;
                image.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        LOGGER.info("[sdf] baked key={} via=native atlas={}x{} em={} cells={} alphaSum={}", key, atlasW, atlasH, em, count, alphaSum);
        return registerFont(key, bakePx, image, atlasW, atlasH, em, table, spaceAdvance);
    }

    private static SdfFont ftBake(String key, byte[] ttf, int bakePx, float em) {
        long lib = 0;
        FT_Face face = null;
        ByteBuffer data = null;
        try {
            PointerBuffer libBuf = MemoryUtil.memAllocPointer(1);
            try {
                if (FreeType.FT_Init_FreeType(libBuf) != 0) return null;
                lib = libBuf.get(0);
            } finally {
                MemoryUtil.memFree(libBuf);
            }
            data = MemoryUtil.memAlloc(ttf.length);
            data.put(ttf).flip();
            PointerBuffer faceBuf = MemoryUtil.memAllocPointer(1);
            try {
                if (FreeType.FT_New_Memory_Face(lib, data, 0, faceBuf) != 0) return null;
                face = FT_Face.create(faceBuf.get(0));
            } finally {
                MemoryUtil.memFree(faceBuf);
            }
            if (FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE) != 0) return null;
            int rasterPx = bakePx * 4;
            if (FreeType.FT_Set_Pixel_Sizes(face, 0, rasterPx) != 0) return null;
            int asc = (int) (face.size().metrics().ascender() >> 6);
            List<Cell> cells = new ArrayList<>(320);
            int[][] ranges = {{32, 126}, {160, 255}, {1024, 1119}};
            for (int[] range : ranges) {
                for (int cp = range[0]; cp <= range[1]; cp++) {
                    int gid = FreeType.FT_Get_Char_Index(face, cp);
                    if (gid == 0) continue;
                    if (FreeType.FT_Load_Glyph(face, gid, FreeType.FT_LOAD_RENDER) != 0) continue;
                    FT_GlyphSlot slot = face.glyph();
                    FT_Bitmap bm = slot.bitmap();
                    if (bm.pixel_mode() != FreeType.FT_PIXEL_MODE_GRAY) continue;
                    Cell c = new Cell();
                    c.codepoint = cp;
                    c.advance = (int) (slot.advance().x() >> 6);
                    int w = bm.width();
                    int h = bm.rows();
                    if (w <= 0 || h <= 0) {
                        cells.add(c);
                        continue;
                    }
                    c.w = w;
                    c.h = h;
                    c.xoff = slot.bitmap_left();
                    c.yoff = asc - slot.bitmap_top();
                    int pitch = bm.pitch();
                    int apitch = Math.abs(pitch);
                    ByteBuffer buf = bm.buffer(apitch * h);
                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    for (int row = 0; row < h; row++) {
                        int srcRow = pitch >= 0 ? row : h - 1 - row;
                        for (int col = 0; col < w; col++) {
                            int a = buf.get(srcRow * apitch + col) & 0xFF;
                            img.setRGB(col, row, (a << 24) | 0xFFFFFF);
                        }
                    }
                    c.image = img;
                    cells.add(c);
                }
            }
            if (cells.isEmpty()) return null;
            return packCells(key, bakePx, cells, 0.25f, em, false, "ft");
        } catch (Throwable e) {
            return null;
        } finally {
            if (face != null) {
                try {
                    FreeType.FT_Done_Face(face);
                } catch (Throwable ignored) {
                }
            }
            if (lib != 0) {
                try {
                    FreeType.FT_Done_FreeType(lib);
                } catch (Throwable ignored) {
                }
            }
            if (data != null) MemoryUtil.memFree(data);
        }
    }

    private static SdfFont awtBake(String key, byte[] ttf, int bakePx, float em) {
        Font base;
        try {
            base = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf));
        } catch (Exception e) {
            return null;
        }
        Font font;
        try {
            font = base.deriveFont(Font.PLAIN, (float) RASTER_PX);
        } catch (Exception e) {
            return null;
        }
        FontRenderContext frc = new FontRenderContext(null, true, true);
        LineMetrics lm = font.getLineMetrics("AgА", frc);
        float emPx = lm.getHeight();
        float ascent = lm.getAscent();
        if (emPx <= 0) return null;
        float down = bakePx / emPx;
        List<Cell> cells = new ArrayList<>(320);
        int[][] ranges = {{32, 126}, {160, 255}, {1024, 1119}};
        for (int[] range : ranges) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                if (!font.canDisplay(cp)) continue;
                GlyphVector gv;
                try {
                    gv = font.createGlyphVector(frc, new char[]{(char) cp});
                } catch (Exception e) {
                    continue;
                }
                Rectangle2D vb = gv.getVisualBounds();
                Cell c = new Cell();
                c.codepoint = cp;
                c.advance = Math.round(gv.getGlyphMetrics(0).getAdvance());
                if (vb.isEmpty()) {
                    c.w = 0;
                    c.h = 0;
                    c.xoff = 0;
                    c.yoff = 0;
                    cells.add(c);
                    continue;
                }
                c.w = (int) Math.ceil(vb.getWidth()) + PAD * 2;
                c.h = (int) Math.ceil(vb.getHeight()) + PAD * 2;
                c.xoff = (int) Math.round(vb.getX()) - PAD;
                c.yoff = (int) Math.round(ascent + vb.getY()) - PAD;
                BufferedImage img = new BufferedImage(c.w, c.h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.drawGlyphVector(gv, (float) (PAD - vb.getX()), (float) (PAD - vb.getY()));
                g2d.dispose();
                c.image = img;
                cells.add(c);
            }
        }
        if (cells.isEmpty()) return null;
        return packCells(key, bakePx, cells, down, em, true, "awt");
    }

    private static SdfFont packCells(String key, int bakePx, List<Cell> cells, float cellScale, float em, boolean enhance, String via) {
        int n = cells.size();
        int[] posX = new int[n];
        int[] posY = new int[n];
        int[] iw = new int[n];
        int[] ih = new int[n];
        int[] qw = new int[n];
        int[] qh = new int[n];
        BufferedImage[] imgs = new BufferedImage[n];
        for (int i = 0; i < n; i++) {
            Cell c = cells.get(i);
            if (c.image == null || c.w <= 0 || c.h <= 0) continue;
            if (enhance) {
                iw[i] = Math.max(1, Math.round(c.w * cellScale));
                ih[i] = Math.max(1, Math.round(c.h * cellScale));
                imgs[i] = downsampleCell(c.image, iw[i], ih[i]);
            } else {
                iw[i] = c.w;
                ih[i] = c.h;
                imgs[i] = c.image;
            }
            qw[i] = Math.max(1, Math.round(c.w * cellScale));
            qh[i] = Math.max(1, Math.round(c.h * cellScale));
        }
        int cursorX = 0;
        int cursorY = 0;
        int rowH = 0;
        for (int i = 0; i < n; i++) {
            if (cursorX + iw[i] > ATLAS_WIDTH) {
                cursorY += rowH;
                cursorX = 0;
                rowH = 0;
            }
            posX[i] = cursorX;
            posY[i] = cursorY;
            cursorX += iw[i];
            if (ih[i] > rowH) rowH = ih[i];
        }
        int atlasH = cursorY + rowH;
        if (atlasH > ATLAS_MAX_HEIGHT) return null;
        BufferedImage atlas = new BufferedImage(ATLAS_WIDTH, atlasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pack = atlas.createGraphics();
        for (int i = 0; i < n; i++) {
            if (imgs[i] == null) continue;
            pack.drawImage(imgs[i], posX[i], posY[i], null);
        }
        pack.dispose();
        if (enhance) sharpenAtlas(atlas);
        NativeImage image = new NativeImage(ATLAS_WIDTH, atlasH, false);
        int[] px = atlas.getRGB(0, 0, ATLAS_WIDTH, atlasH, null, 0, ATLAS_WIDTH);
        long alphaSum = 0;
        for (int i = 0; i < px.length; i++) {
            int a = (px[i] >> 24) & 0xFF;
            if (a < 26) a = 0;
            alphaSum += a;
            image.setPixel(i % ATLAS_WIDTH, i / ATLAS_WIDTH, (a << 24) | 0xFFFFFF);
        }
        int[][] table = new int[TABLE_SIZE][];
        int spaceAdvance = Math.max(1, Math.round(em / 4.0f));
        for (int i = 0; i < n; i++) {
            Cell c = cells.get(i);
            int[] g = new int[]{posX[i], posY[i], iw[i], ih[i], qw[i], qh[i], (int) Math.round(c.xoff * cellScale), (int) Math.round(c.yoff * cellScale), (int) Math.round(c.advance * cellScale * 4)};
            if (c.codepoint >= 0 && c.codepoint < TABLE_SIZE) table[c.codepoint] = g;
            if (c.codepoint == 32) spaceAdvance = g[8];
        }
        LOGGER.info("[sdf] baked key={} via={} atlas={}x{} em={} cells={} alphaSum={}", key, via, ATLAS_WIDTH, atlasH, em, cells.size(), alphaSum);
        return registerFont(key, bakePx, image, ATLAS_WIDTH, atlasH, em, table, spaceAdvance);
    }

    private static BufferedImage scaleImage(BufferedImage src, int dw, int dh) {
        BufferedImage dst = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, dw, dh, 0, 0, src.getWidth(), src.getHeight(), null);
        g.dispose();
        return dst;
    }

    private static BufferedImage downsampleCell(BufferedImage src, int dw, int dh) {
        BufferedImage cur = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        while (cw > dw * 2 || ch > dh * 2) {
            int nw = Math.max(dw, cw / 2);
            int nh = Math.max(dh, ch / 2);
            if (nw >= cw && nh >= ch) break;
            cur = scaleImage(cur, nw, nh);
            cw = nw;
            ch = nh;
        }
        if (cw != dw || ch != dh) cur = scaleImage(cur, dw, dh);
        return cur;
    }

    private static void sharpenAtlas(BufferedImage atlas) {
        int w = atlas.getWidth();
        int h = atlas.getHeight();
        int[] px = atlas.getRGB(0, 0, w, h, null, 0, w);
        int[] blur = new int[px.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy < 0 ? 0 : (y + dy >= h ? h - 1 : y + dy);
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx < 0 ? 0 : (x + dx >= w ? w - 1 : x + dx);
                        sum += (px[ny * w + nx] >> 24) & 0xFF;
                    }
                }
                blur[y * w + x] = sum / 9;
            }
        }
        for (int i = 0; i < px.length; i++) {
            int a = (px[i] >> 24) & 0xFF;
            float s = a + (a - blur[i]) * 0.5f;
            if (s < 0) s = 0;
            if (s > 255) s = 255;
            int g = (int) Math.round(255.0 * Math.pow(s / 255.0, 1.05));
            px[i] = (g << 24) | 0xFFFFFF;
        }
        atlas.setRGB(0, 0, w, h, px, 0, w);
    }

    private static void setNearestSampler(AbstractTexture tex) {
        try {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            for (Field f : AbstractTexture.class.getDeclaredFields()) {
                if (GpuSampler.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(tex, sampler);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static int shadowColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * 0.25f);
        int g = (int) (((color >> 8) & 0xFF) * 0.25f);
        int b = (int) ((color & 0xFF) * 0.25f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static SdfFont registerFont(String key, int bakePx, NativeImage image, int atlasW, int atlasH, float emPx, int[][] table, int spaceAdvance) {
        DynamicTexture tex = new DynamicTexture(() -> "sdf_" + key + "_" + bakePx, image);
        setNearestSampler(tex);
        Identifier id = Identifier.fromNamespaceAndPath("ravex", "sdf/" + key + "_" + bakePx);
        MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
        return new SdfFont(id, atlasW, atlasH, emPx, table, spaceAdvance);
    }

    public static int measureWidth(String key, String text, float fontSize) {
        SdfFont font = getFont(key, fontSize);
        if (font == null || text == null) return -1;
        if (!mappable(font, text)) return -1;
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += font.table[text.charAt(i)][8] / 4.0f;
        }
        return Math.round(w);
    }

    private static boolean mappable(SdfFont font, String text) {
        for (int i = 0; i < text.length(); i++) {
            int cp = text.charAt(i);
            if (cp >= TABLE_SIZE || font.table[cp] == null) return false;
        }
        return true;
    }

    public static int lineHeight(String key, float fontSize) {
        SdfFont font = peek(key, fontSize);
        if (font == null) font = getFont(key, fontSize);
        if (font == null) return -1;
        return (int) (TARGET_PX * fontSize);
    }

    public static boolean drawString(GuiGraphics graphics, String key, String text, float x, float y, float fontSize, int color, boolean shadow) {
        SdfFont font = getFont(key, fontSize);
        if (font == null || text == null) return false;
        if (!mappable(font, text)) return false;
        if (shadow) {
            drawRun(graphics, font, text, x + 1, y + 1, shadowColor(color));
        }
        drawRun(graphics, font, text, x, y, color);
        return true;
    }

    private static void drawRun(GuiGraphics graphics, SdfFont font, String text, float x, float y, int color) {
        var pose = graphics.pose();
        float pen = x;
        for (int i = 0; i < text.length(); i++) {
            int[] g = font.table[text.charAt(i)];
            float advance = g[8] / 4.0f;
            if (g[2] <= 0 || g[3] <= 0 || g[4] <= 0 || g[5] <= 0) {
                pen += advance;
                continue;
            }
            float dx = pen + g[6];
            float dy = y + g[7];
            pose.pushMatrix();
            pose.translate(dx, dy);
            pose.scale(g[4] / (float) g[2], g[5] / (float) g[3]);
            graphics.blit(RenderPipelines.GUI_TEXTURED, font.textureId, 0, 0, (float) g[0], (float) g[1], g[2], g[3], font.atlasW, font.atlasH, color);
            pose.popMatrix();
            pen += advance;
        }
    }
}
