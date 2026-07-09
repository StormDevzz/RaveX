package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
<<<<<<< HEAD
import net.minecraft.resources.Identifier;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.utility.render.HudRenderer;
<<<<<<< HEAD
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
public class CoordsHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_COORDS_WHITE;
    private static final int IS = HudRenderer.getIconSize();
=======
public class CoordsHud extends Module {
    public static final CoordsHud INSTANCE = new CoordsHud();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private CoordsHud() {
        super("Coords", 10, 200, 140, 14);
        addParameter(new BooleanParameter("Shadow", true));
        addParameter(new BooleanParameter("ColoredLabels", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
<<<<<<< HEAD
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
=======
        if (!Hud.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        int pw = 4 + HudRenderer.textWidth(full) + 4 + IS + 4;
        int ph = 14;
        setWidth(pw);
        setHeight(ph);
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int cx = bx + 4;
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
        if (colored) {
            HudRenderer.drawText(graphics, "X ", cx, by + 2, 0xFFFF4455, shadow);
            cx += HudRenderer.textWidth("X ");
            HudRenderer.drawText(graphics, xStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRenderer.textWidth(xStr);
            HudRenderer.drawText(graphics, " Y ", cx, by + 2, 0xFF44FF88, shadow);
            cx += HudRenderer.textWidth(" Y ");
            HudRenderer.drawText(graphics, yStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRenderer.textWidth(yStr);
            HudRenderer.drawText(graphics, " Z ", cx, by + 2, 0xFF44AAFF, shadow);
            cx += HudRenderer.textWidth(" Z ");
            HudRenderer.drawText(graphics, zStr, cx, by + 2, 0xFFD0D0E0, shadow);
        } else {
            HudRenderer.drawText(graphics, full, cx, by + 2, 0xFFD0D0E0, shadow);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(CoordsHud.class);
    }

    public static CoordsHud itz() {
        return ModuleManager.get(CoordsHud.class);
    }
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
