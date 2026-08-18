package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("FpsHud")
public class FpsHud extends ravex.modules.Module {
    @Parameter(name = "HighColor", color = true)
    public int highColor = 0xFF44FF88;
    @Parameter(name = "MidColor", color = true)
    public int midColor = 0xFFFFCC33;
    @Parameter(name = "LowColor", color = true)
    public int lowColor = 0xFFFF4455;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

private static final Identifier ICON = TextureLoaderUtility.HUD_FPS_WHITE;

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        int fps = MinecraftWrapper.getWrapper().getFps();
        int ac = ColorUtility.getActiveColor();
        boolean shadow = this.shadow;
        int highColor = this.highColor, midColor = this.midColor, lowColor = this.lowColor;
        int bx = getX(), by = getY();
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
        setWidth(pw);
        setHeight(ph);
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int ix = bx + 4;
        HudRendererUtility.drawText(graphics, fpsStr, ix, by + 2, fpsColor, shadow);
        ix += HudRendererUtility.textWidth(fpsStr);
        HudRendererUtility.drawText(graphics, suffix, ix, by + 2, 0xFF8080A0, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
    }
}
