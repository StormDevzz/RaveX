package ravex.utility.render;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import ravex.manager.ModuleManager;

public class FontRenderUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger("ravex/font");

    private static final Identifier COMFORTAA_FONT = Identifier.fromNamespaceAndPath("ravex", "comfortaa");
    private static final FontDescription COMFORTAA_DESC = new FontDescription.Resource(COMFORTAA_FONT);

    private static final Identifier SF_MEDIUM_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_medium");
    private static final FontDescription SF_MEDIUM_DESC = new FontDescription.Resource(SF_MEDIUM_FONT);

    private static final Identifier SF_BOLD_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_bold");
    private static final FontDescription SF_BOLD_DESC = new FontDescription.Resource(SF_BOLD_FONT);

    private static final Identifier INTER_FONT = Identifier.fromNamespaceAndPath("ravex", "inter");
    private static final FontDescription INTER_DESC = new FontDescription.Resource(INTER_FONT);

    private static final Identifier INTER_BOLD_FONT = Identifier.fromNamespaceAndPath("ravex", "inter_bold");
    private static final FontDescription INTER_BOLD_DESC = new FontDescription.Resource(INTER_BOLD_FONT);

    private static boolean renderOnce = false;

    private static final HashMap<Integer, Component> componentCache = new HashMap<>();
    private static final int CACHE_MAX = 1024;

    public enum FontType {
        SF_MEDIUM, SF_BOLD, COMFORTAA, INTER, INTER_BOLD, VANILLA
    }

    public static FontType getCurrentFontType() {
        if (!ModuleManager.get(ravex.modules.client.Fonts.class).p_enabled) {
            return FontType.VANILLA;
        }
        String font = ModuleManager.get(ravex.modules.client.Fonts.class).fontType;
        switch (font) {
            case "Comfortaa":     return FontType.COMFORTAA;
            case "SFMedium":      return FontType.SF_MEDIUM;
            case "SFBold":        return FontType.SF_BOLD;
            case "Inter":         return FontType.INTER;
            case "InterBold":     return FontType.INTER_BOLD;
            default:              return FontType.VANILLA;
        }
    }

    private static Component getFontComponent(FontType fontType, String text) {
        if (text == null) return Component.empty();

        FontType actualType = fontType;
        boolean customFontActive = ModuleManager.get(ravex.modules.client.Fonts.class).p_enabled;

        if (actualType != FontType.VANILLA && !customFontActive) {
            actualType = FontType.VANILLA;
        }

        int key = text.hashCode() * 31 + actualType.ordinal();
        Component cached = componentCache.get(key);
        if (cached != null) return cached;

        Component result;
        if (actualType == FontType.VANILLA) {
            result = Component.literal(text);
        } else {
            FontDescription desc;
            switch (actualType) {
                case COMFORTAA:     desc = COMFORTAA_DESC; break;
                case SF_MEDIUM:     desc = SF_MEDIUM_DESC; break;
                case SF_BOLD:       desc = SF_BOLD_DESC; break;
                case INTER:         desc = INTER_DESC; break;
                case INTER_BOLD:    desc = INTER_BOLD_DESC; break;
                default:            result = Component.literal(text); componentCache.put(key, result); return result;
            }
            result = Component.literal(text).withStyle(Style.EMPTY.withFont(desc));
        }

        componentCache.put(key, result);
        if (componentCache.size() > CACHE_MAX) componentCache.clear();
        return result;
    }

    public static Component getTextComponent(String text) {
        return getFontComponent(getCurrentFontType(), text);
    }

    public static FontDescription getCurrentFontDescription() {
        FontType type = getCurrentFontType();
        switch (type) {
            case COMFORTAA: return COMFORTAA_DESC;
            case SF_MEDIUM: return SF_MEDIUM_DESC;
            case SF_BOLD:   return SF_BOLD_DESC;
            case INTER:     return INTER_DESC;
            case INTER_BOLD:return INTER_BOLD_DESC;
            default:        return null;
        }
    }

    public static Component applyGlobalFont(Component component) {
        FontDescription desc = getCurrentFontDescription();
        if (desc == null) return component;
        return applyFont(component, desc);
    }

    private static Component applyFont(Component component, FontDescription desc) {
        if (component.getSiblings().isEmpty()) {
            if (component.getStyle().getFont() != null) return component;
            return component.copy().withStyle(component.getStyle().withFont(desc));
        }
        net.minecraft.network.chat.MutableComponent result;
        if (component.getStyle().getFont() == null) {
            result = component.copy().withStyle(component.getStyle().withFont(desc));
        } else {
            result = component.copy();
        }
        result.getSiblings().clear();
        for (Component sib : component.getSiblings()) {
            result.append(applyFont(sib, desc));
        }
        return result;
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        drawString(graphics, getCurrentFontType(), text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphics graphics, FontType fontType, String text, int x, int y, int color, boolean shadow) {
        double scale = ModuleManager.get(ravex.modules.client.Fonts.class).fontSize;
        Component component = getFontComponent(fontType, text);

        if (!renderOnce) {
            renderOnce = true;
            LOGGER.info("[RaveX/font] Custom fonts successfully initialized!");
        }

        var font = MinecraftWrapper.getWrapper().getFont();
        if (Math.abs(scale - 1.0) < 0.001) {
            graphics.drawString(font, component, x, y, color, shadow);
        } else {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate((float) x, (float) y);
            pose.scale((float) scale, (float) scale);
            graphics.drawString(font, component, 0, 0, color, shadow);
            pose.popMatrix();
        }
    }

    public static final float FIT_MIN_FACTOR = 0.55f;

    public static final class FitText {
        public final String text;
        public final float scale;
        public final int width;

        public FitText(String text, float scale, int width) {
            this.text = text;
            this.scale = scale;
            this.width = width;
        }
    }

    public static FitText fitText(FontType fontType, String text, int maxWidth) {
        double base = ModuleManager.get(ravex.modules.client.Fonts.class).fontSize;
        float baseScale = (float) base;
        if (text == null) text = "";
        var font = MinecraftWrapper.getWrapper().getFont();
        float raw = font.width(getFontComponent(fontType, text));
        float minScale = baseScale * FIT_MIN_FACTOR;
        if (raw <= 0.5f) return new FitText(text, baseScale, 0);
        if (maxWidth <= 4) return new FitText("", minScale, 0);
        float need = maxWidth / raw;
        if (need >= baseScale) return new FitText(text, baseScale, Math.round(raw * baseScale));
        if (need >= minScale) return new FitText(text, need, Math.round(raw * need));
        String current = text;
        while (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
            String candidate = current + "…";
            float w = font.width(getFontComponent(fontType, candidate));
            if (w * minScale <= maxWidth) return new FitText(candidate, minScale, Math.round(w * minScale));
        }
        return new FitText("", minScale, 0);
    }

    public static void drawScaled(GuiGraphics graphics, FontType fontType, String text, int x, int y, float scale, int color, boolean shadow) {
        double base = ModuleManager.get(ravex.modules.client.Fonts.class).fontSize;
        Component component = getFontComponent(fontType, text);
        var font = MinecraftWrapper.getWrapper().getFont();
        if (Math.abs(scale - base) < 0.001) {
            graphics.drawString(font, component, x, y, color, shadow);
        } else {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate((float) x, (float) y);
            pose.scale(scale, scale);
            graphics.drawString(font, component, 0, 0, color, shadow);
            pose.popMatrix();
        }
    }

    public static int getStringWidth(String text) {
        return getStringWidth(getCurrentFontType(), text);
    }

    public static int getStringWidth(FontType fontType, String text) {
        double scale = ModuleManager.get(ravex.modules.client.Fonts.class).fontSize;
        return (int) (MinecraftWrapper.getWrapper().getFont().width(getFontComponent(fontType, text)) * scale);
    }

    public static int getFontHeight() {
        double scale = ModuleManager.get(ravex.modules.client.Fonts.class).fontSize;
        return (int) (MinecraftWrapper.getWrapper().getFont().lineHeight * scale);
    }
}
