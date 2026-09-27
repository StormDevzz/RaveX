package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.utility.player.PlayerUtility;
import ravex.modules.Modules;
import ravex.gui.hudeditor.HudEditorScreen;
import ravex.mcwrapper.MinecraftWrapper;

@HudModule("CoordsHud")
public class CoordsHud extends ravex.modules.Module {
    @Parameter(name = "Shadow")
    public boolean shadow = true;
    @Parameter(name = "ColoredLabels")
    public boolean coloredLabels = true;
    @Parameter(name = "XColor", color = true)
    public int xColor = 0xFFFF4455;
    @Parameter(name = "YColor", color = true)
    public int yColor = 0xFF44FF88;
    @Parameter(name = "ZColor", color = true)
    public int zColor = 0xFF44AAFF;

    private static final Identifier ICON = TextureLoaderUtility.HUD_COORDS_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

    public CoordsHud() {
        super("CoordsHud", 10, 10, 140, 18);
        setX(10);
        setY(10);
        setWidth(140);
        setHeight(18);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var player = PlayerUtility.getPlayer();
        boolean inEditor = MinecraftWrapper.getWrapper().getScreen() instanceof HudEditorScreen;
        if (player == null && !inEditor) return;

        int ac = ColorUtility.getActiveColor();
        boolean shadow = this.shadow;
        boolean colored = this.coloredLabels;
        int bx = getX(), by = getY();

        String xStr = player != null ? String.format("%.1f", player.getX()) : "100.0";
        String yStr = player != null ? String.format("%.1f", player.getY()) : "64.0";
        String zStr = player != null ? String.format("%.1f", player.getZ()) : "-200.0";

        int textW;
        if (colored) {
            textW = HudRendererUtility.textWidth("X ") + HudRendererUtility.textWidth(xStr)
                + HudRendererUtility.textWidth("  Y ") + HudRendererUtility.textWidth(yStr)
                + HudRendererUtility.textWidth("  Z ") + HudRendererUtility.textWidth(zStr);
        } else {
            textW = HudRendererUtility.textWidth(xStr + " / " + yStr + " / " + zStr);
        }

        int pw = 6 + textW + 6 + IS + 4;
        int ph = 18;
        setWidth(pw);
        setHeight(ph);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int cx = bx + 6;
        int textY = by + (ph - HudRendererUtility.fontHeight()) / 2;

        if (colored) {
            HudRendererUtility.drawText(graphics, "X ", cx, textY, xColor, shadow);
            cx += HudRendererUtility.textWidth("X ");
            HudRendererUtility.drawText(graphics, xStr, cx, textY, 0xFFE0E0E8, shadow);
            cx += HudRendererUtility.textWidth(xStr);

            HudRendererUtility.drawText(graphics, "  Y ", cx, textY, yColor, shadow);
            cx += HudRendererUtility.textWidth("  Y ");
            HudRendererUtility.drawText(graphics, yStr, cx, textY, 0xFFE0E0E8, shadow);
            cx += HudRendererUtility.textWidth(yStr);

            HudRendererUtility.drawText(graphics, "  Z ", cx, textY, zColor, shadow);
            cx += HudRendererUtility.textWidth("  Z ");
            HudRendererUtility.drawText(graphics, zStr, cx, textY, 0xFFE0E0E8, shadow);
        } else {
            String plain = xStr + " / " + yStr + " / " + zStr;
            HudRendererUtility.drawText(graphics, plain, cx, textY, 0xFFE0E0E8, shadow);
        }

        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
    }
}
