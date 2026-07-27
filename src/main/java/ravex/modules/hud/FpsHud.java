package ravex.modules.hud;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "FpsHud", category = "HUD")
public class FpsHud extends ravex.modules.Module {
    @Parameter(name = "HighColor", color = true)
    public int highColor = 0xFF44FF88;
    @Parameter(name = "MidColor", color = true)
    public int midColor = 0xFFFFCC33;
    @Parameter(name = "LowColor", color = true)
    public int lowColor = 0xFFFF4455;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_FPS_WHITE;




    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        int fps = MinecraftWrapper.getInstance().getFps();
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
        int bx = x, by = y;
        int fpsColor;
        if (fps >= 60) fpsColor = highColor;
        else if (fps >= 30) fpsColor = midColor;
        else fpsColor = lowColor;
        String fpsStr = String.valueOf(fps);
        String suffix = " FPS";
        int tw = HudRendererUtility.textWidth(fpsStr) + HudRendererUtility.textWidth(suffix);
        int IS = HudRendererUtility.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;
        width = pw;
        height = ph;
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int ix = bx + 4;
        HudRendererUtility.drawText(graphics, fpsStr, ix, by + 2, fpsColor, shadow);
        ix += HudRendererUtility.textWidth(fpsStr);
        HudRendererUtility.drawText(graphics, suffix, ix, by + 2, 0xFF8080A0, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
    }


    

    @Override
    public int getX() { return x; }
    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public int getY() { return y; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public int getHeight() { return height; }
    @Override
    public void setHeight(int h) { this.height = h; }

    public boolean isHud() {
        return hud;
    }
}