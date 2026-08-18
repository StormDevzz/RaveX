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

@HudModule("SpeedometerHud")
public class SpeedometerHud extends ravex.modules.Module {
    @Parameter(name = "Unit", modes = {"BPS", "KMH"})
    public String unit = "BPS";
    @Parameter(name = "Shadow")
    public boolean shadow = true;

private static final Identifier ICON = Identifier.fromNamespaceAndPath("ravex", "hud_white/speedometer");

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var player = PlayerUtility.getPlayer();
        if (player == null) return;

        String unitMode = this.unit;
        boolean shadow = this.shadow;

        double dX = player.getX() - player.xo;
        double dZ = player.getZ() - player.zo;
        double speedBps = Math.sqrt(dX * dX + dZ * dZ) * 20.0;
        double displaySpeed = unitMode.equals("KMH") ? speedBps * 3.6 : speedBps;

        int activeColor = ColorUtility.getActiveColor();
        String valStr = String.format("%.1f", displaySpeed);
        String labelStr = " " + unitMode.toLowerCase();

        int tw = HudRendererUtility.textWidth(valStr) + HudRendererUtility.textWidth(labelStr);
        int IS = HudRendererUtility.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;

        setWidth(pw);
        setHeight(ph);

        TextureLoaderUtility.getHudIconWhite("speedometer");

        int bx = getX(), by = getY();
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int ix = bx + 4;
        HudRendererUtility.drawText(graphics, valStr, ix, by + 2, activeColor, shadow);
        ix += HudRendererUtility.textWidth(valStr);
        HudRendererUtility.drawText(graphics, labelStr, ix, by + 2, 0xFF8080A0, false);

        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, activeColor);
    }
}
