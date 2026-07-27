package ravex.modules.hud;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "SpeedometerHud", category = "HUD")
public class SpeedometerHud extends ravex.modules.Module {
    @Parameter(name = "Unit", modes = {"BPS", "KMH"})
    public String unit = "BPS";
    @Parameter(name = "Shadow")
    public boolean shadow = true;

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = Identifier.fromNamespaceAndPath("ravex", "hud_white/speedometer");




    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        String unitMode = "BPS";
        boolean shadow = true;
        for (var p : getParameters()) {
            if (p instanceof ModeParameter mp && mp.getName().equals("Unit")) unitMode = mp.getValue();
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
        }

        double dX = mc.player.getX() - mc.player.xo;
        double dZ = mc.player.getZ() - mc.player.zo;
        double speedBps = Math.sqrt(dX * dX + dZ * dZ) * 20.0;
        double displaySpeed = unitMode.equals("KMH") ? speedBps * 3.6 : speedBps;

        int activeColor = ColorUtility.getActiveColor();
        String valStr = String.format("%.1f", displaySpeed);
        String labelStr = " " + unitMode.toLowerCase();

        int tw = HudRendererUtility.textWidth(valStr) + HudRendererUtility.textWidth(labelStr);
        int IS = HudRendererUtility.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;

        width = pw;
        height = ph;

        TextureLoaderUtility.getHudIconWhite("speedometer");

        int bx = x, by = y;
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int ix = bx + 4;
        HudRendererUtility.drawText(graphics, valStr, ix, by + 2, activeColor, shadow);
        ix += HudRendererUtility.textWidth(valStr);
        HudRendererUtility.drawText(graphics, labelStr, ix, by + 2, 0xFF8080A0, false);

        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, activeColor);
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