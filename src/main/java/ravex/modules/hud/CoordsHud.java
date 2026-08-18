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

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var player = PlayerUtility.getPlayer();
        if (player == null) return;
        int ac = ColorUtility.getActiveColor();
        boolean shadow = this.shadow;
        boolean colored = this.coloredLabels;
        int bx = getX(), by = getY();
        String xStr = String.format("%.1f", player.getX());
        String yStr = String.format("%.1f", player.getY());
        String zStr = String.format("%.1f", player.getZ());
        String full = (colored ? "X " : "") + xStr + (colored ? " Y " : " / ") + yStr + (colored ? " Z " : " / ") + zStr;
        int pw = 4 + HudRendererUtility.textWidth(full) + 4 + IS + 4;
        int ph = 14;
        setWidth(pw);
        setHeight(ph);
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cx = bx + 4;
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
        if (colored) {
            HudRendererUtility.drawText(graphics, "X ", cx, by + 2, xColor, shadow);
            cx += HudRendererUtility.textWidth("X ");
            HudRendererUtility.drawText(graphics, xStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRendererUtility.textWidth(xStr);
            HudRendererUtility.drawText(graphics, " Y ", cx, by + 2, yColor, shadow);
            cx += HudRendererUtility.textWidth(" Y ");
            HudRendererUtility.drawText(graphics, yStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRendererUtility.textWidth(yStr);
            HudRendererUtility.drawText(graphics, " Z ", cx, by + 2, zColor, shadow);
            cx += HudRendererUtility.textWidth(" Z ");
            HudRendererUtility.drawText(graphics, zStr, cx, by + 2, 0xFFD0D0E0, shadow);
        } else {
            HudRendererUtility.drawText(graphics, full, cx, by + 2, 0xFFD0D0E0, shadow);
        }
    }
}
