package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class TpsHud extends Module {
    public static final TpsHud INSTANCE = new TpsHud();
    private long lastRealTime = 0;
    private long lastGameTick = -1;
    private float smoothedTPS = 20.0f;
    private TpsHud() {
        super("TPS", 10, 240, 70, 30);
        addParameter(new ColorParameter("Color", 0xFF44FF88));
        addParameter(new BooleanParameter("Shadow", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        updateTPS(mc);
        int bx = getX(), by = getY();
        int col = 0xFF44FF88;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) col = cp.getValue();
        }
        boolean shadow = true;
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
        }
        int pw = 66;
        int ph = 26;
        setWidth(pw);
        setHeight(ph);
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        String text = String.format("%.1f", smoothedTPS);
        int tw = ravex.utility.render.FontRenderUtility.getStringWidth(text);
        ravex.utility.render.FontRenderUtility.drawString(graphics, text, bx + (pw - tw) / 2, by + 4, col, shadow);
        String label = "TPS";
        int lw = ravex.utility.render.FontRenderUtility.getStringWidth(label);
        ravex.utility.render.FontRenderUtility.drawString(graphics, label, bx + (pw - lw) / 2, by + 16, 0xFF8080A0, false);
    }
    private void updateTPS(Minecraft mc) {
        long now = System.currentTimeMillis();
        long gameTick = mc.level.getGameTime();
        if (lastGameTick < 0) {
            lastGameTick = gameTick;
            lastRealTime = now;
            return;
        }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick;
            lastRealTime = now;
        }
    }
}
