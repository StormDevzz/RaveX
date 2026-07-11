package ravex.modules.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;

public class SpeedometerHud extends Module {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("ravex", "hud_white/speedometer");

    private SpeedometerHud() {
        super("SpeedometerHud", 10, 240, 70, 14);
        addParameter(new ModeParameter("Unit", "BPS", java.util.List.of("BPS", "KMH")));
        addParameter(new BooleanParameter("Shadow", true));
    }

    public static SpeedometerHud itz() {
        return ModuleManager.get(SpeedometerHud.class);
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(SpeedometerHud.class);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
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

        int tw = HudRenderer.textWidth(valStr) + HudRenderer.textWidth(labelStr);
        int IS = HudRenderer.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;

        setWidth(pw);
        setHeight(ph);

        TextureLoader.getHudIconWhite("speedometer");

        int bx = getX(), by = getY();
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);

        int ix = bx + 4;
        HudRenderer.drawText(graphics, valStr, ix, by + 2, activeColor, shadow);
        ix += HudRenderer.textWidth(valStr);
        HudRenderer.drawText(graphics, labelStr, ix, by + 2, 0xFF8080A0, false);

        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, activeColor);
    }
}
