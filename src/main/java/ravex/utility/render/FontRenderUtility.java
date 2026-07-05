package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class FontRenderUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger("ravex/font");

    private static final Identifier COMFORTAA_FONT = Identifier.fromNamespaceAndPath("ravex", "comfortaa");
    private static final FontDescription COMFORTAA_DESC = new FontDescription.Resource(COMFORTAA_FONT);

    private static final Identifier SF_MEDIUM_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_medium");
    private static final FontDescription SF_MEDIUM_DESC = new FontDescription.Resource(SF_MEDIUM_FONT);

    private static final Identifier SF_BOLD_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_bold");
    private static final FontDescription SF_BOLD_DESC = new FontDescription.Resource(SF_BOLD_FONT);

    private static boolean renderOnce = false;

    private static final HashMap<Integer, Component> componentCache = new HashMap<>();
    private static final int CACHE_MAX = 1024;

    public enum FontType {
        SF_MEDIUM, SF_BOLD, COMFORTAA, VANILLA
    }

    public static FontType getCurrentFontType() {
        if (!ravex.modules.client.Fonts.INSTANCE.enabled.getValue()) {
            return FontType.VANILLA;
        }
        String font = ravex.modules.client.Fonts.INSTANCE.fontType.getValue();
        switch (font) {
            case "Comfortaa":     return FontType.COMFORTAA;
            case "SF Medium":     return FontType.SF_MEDIUM;
            case "SF Bold":       return FontType.SF_BOLD;
            default:              return FontType.VANILLA;
        }
    }

    private static Component getFontComponent(FontType fontType, String text) {
        if (text == null) return Component.empty();

        FontType actualType = fontType;
        boolean customFontActive = ravex.modules.client.Fonts.INSTANCE.enabled.getValue();

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
                default:            result = Component.literal(text); componentCache.put(key, result); return result;
            }
            result = Component.literal(text).withStyle(Style.EMPTY.withFont(desc));
        }

        componentCache.put(key, result);
        if (componentCache.size() > CACHE_MAX) componentCache.clear();
        return result;
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        drawString(graphics, getCurrentFontType(), text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphics graphics, FontType fontType, String text, int x, int y, int color, boolean shadow) {
        double scale = ravex.modules.client.Fonts.INSTANCE.fontSize.getValue();
        Component component = getFontComponent(fontType, text);

        if (!renderOnce) {
            renderOnce = true;
            LOGGER.info("[RaveX/font] Custom fonts successfully initialized!");
        }

        if (Math.abs(scale - 1.0) < 0.001) {
            graphics.drawString(Minecraft.getInstance().font, component, x, y, color, shadow);
        } else {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate((float) x, (float) y);
            pose.scale((float) scale, (float) scale);
            graphics.drawString(Minecraft.getInstance().font, component, 0, 0, color, shadow);
            pose.popMatrix();
        }
    }

    public static int getStringWidth(String text) {
        return getStringWidth(getCurrentFontType(), text);
    }

    public static int getStringWidth(FontType fontType, String text) {
        double scale = ravex.modules.client.Fonts.INSTANCE.fontSize.getValue();
        return (int) (Minecraft.getInstance().font.width(getFontComponent(fontType, text)) * scale);
    }

    public static int getFontHeight() {
        double scale = ravex.modules.client.Fonts.INSTANCE.fontSize.getValue();
        return (int) (Minecraft.getInstance().font.lineHeight * scale);
    }
}
