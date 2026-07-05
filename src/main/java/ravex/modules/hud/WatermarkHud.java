package ravex.modules.hud;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRenderer;
public class WatermarkHud extends Module {
    public static final WatermarkHud INSTANCE = new WatermarkHud();
    private WatermarkHud() {
        super("Watermark", 10, 10, 80, 14);
        addParameter(new ColorParameter("Color", 0xFF1E88E5));
        addParameter(new BooleanParameter("Shadow", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        int ac = 0xFF1E88E5;
        boolean shadow = true;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) ac = cp.getValue();
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
        }
        int bx = getX(), by = getY();
        String text = "RaveXV" + ravex.RaveX.version + " NextGen";
        int tw = HudRenderer.textWidth(text);
        int pw = tw + 12;
        int ph = 14;
        HudRenderer.drawPanel(graphics, bx, by, pw, ph, ac);
        HudRenderer.drawText(graphics, text, bx + 6, by + 3, ac, shadow);
    }
}
