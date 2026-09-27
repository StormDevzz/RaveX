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
import ravex.gui.hudeditor.HudEditorScreen;
import ravex.mcwrapper.MinecraftWrapper;

@HudModule("SpeedometerHud")
public class SpeedometerHud extends ravex.modules.Module {
    @Parameter(name = "Unit", modes = {"BPS", "KMH"})
    public String unit = "BPS";
    @Parameter(name = "Shadow")
    public boolean shadow = true;

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("ravex", "hud_white/speedometer");

    public SpeedometerHud() {
        super("SpeedometerHud", 10, 70, 70, 14);
        setX(10);
        setY(70);
        setWidth(70);
        setHeight(14);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var player = PlayerUtility.getPlayer();
        boolean inEditor = MinecraftWrapper.getWrapper().getScreen() instanceof HudEditorScreen;
        if (player == null && !inEditor) return;

        String unitMode = this.unit;
        boolean shadow = this.shadow;

        double displaySpeed = 0.0;
        if (player != null) {
            double dX = player.getX() - player.xo;
            double dZ = player.getZ() - player.zo;
            double speedBps = Math.sqrt(dX * dX + dZ * dZ) * 20.0;
            displaySpeed = unitMode.equals("KMH") ? speedBps * 3.6 : speedBps;
        }

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
