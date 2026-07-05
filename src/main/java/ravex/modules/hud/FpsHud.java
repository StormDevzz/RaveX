package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.render.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRenderer;
public class FpsHud extends Module {
    public static final FpsHud INSTANCE = new FpsHud();
    private FpsHud() {
        super("Fps", 10, 220, 60, 14);
        addParameter(new ColorParameter("HighColor", 0xFF44FF88));
        addParameter(new ColorParameter("MidColor", 0xFFFFCC33));
        addParameter(new ColorParameter("LowColor", 0xFFFF4455));
        addParameter(new BooleanParameter("Shadow", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        int fps = Minecraft.getInstance().getFps();
        int ac = ColorUtility.getActiveColor();
        boolean shadow = true;
        int highColor = 0xFF44FF88, midColor = 0xFFFFCC33, lowColor = 0xFFFF4455;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp) {
                switch (cp.getName()) {
                    case "HighColor" -> highColor = cp.getValue();
                    case "MidColor" -> midColor = cp.getValue();
                    case "LowColor" -> lowColor = cp.getValue();
                }
            }
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
        }
        int bx = getX(), by = getY();
        int fpsColor;
        if (fps >= 60) fpsColor = highColor;
        else if (fps >= 30) fpsColor = midColor;
        else fpsColor = lowColor;
        String text = fps + " FPS";
        int pw = HudRenderer.textWidth(text) + 10;
        HudRenderer.drawPanel(graphics, bx, by, pw, 14, ac);
        int ix = bx + 5;
        HudRenderer.drawText(graphics, String.valueOf(fps), ix, by + 3, fpsColor, shadow);
        ix += HudRenderer.textWidth(String.valueOf(fps));
        HudRenderer.drawText(graphics, " FPS", ix, by + 3, 0xFF8080A0, false);
    }
}
