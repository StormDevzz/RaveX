package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import ravex.modules.render.ClickGui;

public class FontRenderUtility {
    private static final Identifier COMFORTAA_FONT = Identifier.fromNamespaceAndPath("ravex", "comfortaa");
    private static final FontDescription COMFORTAA_DESC = new FontDescription.Resource(COMFORTAA_FONT);

    private static final Identifier SF_MEDIUM_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_medium");
    private static final FontDescription SF_MEDIUM_DESC = new FontDescription.Resource(SF_MEDIUM_FONT);

    private static final Identifier SF_BOLD_FONT = Identifier.fromNamespaceAndPath("ravex", "sf_bold");
    private static final FontDescription SF_BOLD_DESC = new FontDescription.Resource(SF_BOLD_FONT);

    private static final Identifier ROBOTO_FONT = Identifier.fromNamespaceAndPath("ravex", "roboto");
    private static final FontDescription ROBOTO_DESC = new FontDescription.Resource(ROBOTO_FONT);

    private static final Identifier INTER_FONT = Identifier.fromNamespaceAndPath("ravex", "inter");
    private static final FontDescription INTER_DESC = new FontDescription.Resource(INTER_FONT);

    private static final Identifier JETBRAINS_MONO_FONT = Identifier.fromNamespaceAndPath("ravex", "jetbrains_mono");
    private static final FontDescription JETBRAINS_MONO_DESC = new FontDescription.Resource(JETBRAINS_MONO_FONT);

    private static final Identifier NOTO_MONO_FONT = Identifier.fromNamespaceAndPath("ravex", "noto_mono");
    private static final FontDescription NOTO_MONO_DESC = new FontDescription.Resource(NOTO_MONO_FONT);

    private static final Identifier FIRA_CODE_FONT = Identifier.fromNamespaceAndPath("ravex", "fira_code");
    private static final FontDescription FIRA_CODE_DESC = new FontDescription.Resource(FIRA_CODE_FONT);

    private static final Identifier OPEN_DYSLEXIC_FONT = Identifier.fromNamespaceAndPath("ravex", "open_dyslexic");
    private static final FontDescription OPEN_DYSLEXIC_DESC = new FontDescription.Resource(OPEN_DYSLEXIC_FONT);

    public enum FontType {
        SF_MEDIUM, SF_BOLD, COMFORTAA, ROBOTO, INTER, JETBRAINS_MONO, NOTO_MONO, FIRA_CODE, OPEN_DYSLEXIC, VANILLA
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
            case "Roboto":        return FontType.ROBOTO;
            case "Inter":         return FontType.INTER;
            case "JetBrainsMono": return FontType.JETBRAINS_MONO;
            case "NotoMono":      return FontType.NOTO_MONO;
            case "FiraCode":      return FontType.FIRA_CODE;
            case "OpenDyslexic":  return FontType.OPEN_DYSLEXIC;
            default:              return FontType.VANILLA;
        }
    }

    private static Component getFontComponent(FontType fontType, String text) {
        if (text == null) return Component.empty();
        
        FontType actualType = fontType;
        boolean customFontActive = ravex.modules.client.Fonts.INSTANCE.enabled.getValue();
        
        if (actualType == FontType.VANILLA && customFontActive) {
            actualType = getCurrentFontType();
        }
        if (actualType != FontType.VANILLA && !customFontActive) {
            actualType = FontType.VANILLA;
        }

        if (actualType == FontType.VANILLA) {
            return Component.literal(text);
        }

        FontDescription desc;
        switch (actualType) {
            case COMFORTAA:     desc = COMFORTAA_DESC; break;
            case SF_MEDIUM:     desc = SF_MEDIUM_DESC; break;
            case SF_BOLD:       desc = SF_BOLD_DESC; break;
            case ROBOTO:        desc = ROBOTO_DESC; break;
            case INTER:         desc = INTER_DESC; break;
            case JETBRAINS_MONO: desc = JETBRAINS_MONO_DESC; break;
            case NOTO_MONO:     desc = NOTO_MONO_DESC; break;
            case FIRA_CODE:     desc = FIRA_CODE_DESC; break;
            case OPEN_DYSLEXIC: desc = OPEN_DYSLEXIC_DESC; break;
            default:            return Component.literal(text);
        }
        return Component.literal(text).withStyle(Style.EMPTY.withFont(desc));
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        drawString(graphics, getCurrentFontType(), text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphics graphics, FontType fontType, String text, int x, int y, int color, boolean shadow) {
        double scale = ravex.modules.client.Fonts.INSTANCE.fontSize.getValue();
        if (Math.abs(scale - 1.0) < 0.01) {
            graphics.drawString(Minecraft.getInstance().font, getFontComponent(fontType, text), x, y, color, shadow);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale((float) scale, (float) scale);
            graphics.drawString(Minecraft.getInstance().font, getFontComponent(fontType, text), 0, 0, color, shadow);
            graphics.pose().popMatrix();
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
