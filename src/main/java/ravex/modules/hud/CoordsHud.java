package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.utility.render.HudRenderer;
public class CoordsHud extends Module {
    public static final CoordsHud INSTANCE = new CoordsHud();
    private CoordsHud() {
        super("Coords", 10, 200, 140, 14);
        addParameter(new BooleanParameter("Shadow", true));
        addParameter(new BooleanParameter("ColoredLabels", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int ac = ColorUtility.getActiveColor();
        boolean shadow = true;
        boolean colored = true;
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
            if (p instanceof BooleanParameter bp && bp.getName().equals("ColoredLabels")) colored = bp.getValue();
        }
        int bx = getX(), by = getY();
        String xStr = String.format("%.1f", player.getX());
        String yStr = String.format("%.1f", player.getY());
        String zStr = String.format("%.1f", player.getZ());
        String full = (colored ? "X " : "") + xStr + (colored ? " Y " : " / ") + yStr + (colored ? " Z " : " / ") + zStr;
        int pw = HudRenderer.textWidth(full) + 10;
        HudRenderer.drawPanel(graphics, bx, by, pw, 14, ac);
        int cx = bx + 5;
        if (colored) {
            HudRenderer.drawText(graphics, "X ", cx, by + 3, 0xFFFF4455, shadow);
            cx += HudRenderer.textWidth("X ");
            HudRenderer.drawText(graphics, xStr, cx, by + 3, 0xFFD0D0E0, shadow);
            cx += HudRenderer.textWidth(xStr);
            HudRenderer.drawText(graphics, " Y ", cx, by + 3, 0xFF44FF88, shadow);
            cx += HudRenderer.textWidth(" Y ");
            HudRenderer.drawText(graphics, yStr, cx, by + 3, 0xFFD0D0E0, shadow);
            cx += HudRenderer.textWidth(yStr);
            HudRenderer.drawText(graphics, " Z ", cx, by + 3, 0xFF44AAFF, shadow);
            cx += HudRenderer.textWidth(" Z ");
            HudRenderer.drawText(graphics, zStr, cx, by + 3, 0xFFD0D0E0, shadow);
        } else {
            HudRenderer.drawText(graphics, full, cx, by + 3, 0xFFD0D0E0, shadow);
        }
    }
}
