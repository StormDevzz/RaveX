package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
public class FpsHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_FPS_WHITE;
    private FpsHud() {
        super("Fps", 10, 220, 60, 14);
        addParameter(new ColorParameter("HighColor", 0xFF44FF88));
        addParameter(new ColorParameter("MidColor", 0xFFFFCC33));
        addParameter(new ColorParameter("LowColor", 0xFFFF4455));
        addParameter(new BooleanParameter("Shadow", true));
    }

    public static FpsHud itz() {
        return ModuleManager.get(FpsHud.class);
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(FpsHud.class);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
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
        String fpsStr = String.valueOf(fps);
        String suffix = " FPS";
        int tw = HudRenderer.textWidth(fpsStr) + HudRenderer.textWidth(suffix);
        int IS = HudRenderer.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;
        setWidth(pw);
        setHeight(ph);
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int ix = bx + 4;
        HudRenderer.drawText(graphics, fpsStr, ix, by + 2, fpsColor, shadow);
        ix += HudRenderer.textWidth(fpsStr);
        HudRenderer.drawText(graphics, suffix, ix, by + 2, 0xFF8080A0, false);
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
    }
}