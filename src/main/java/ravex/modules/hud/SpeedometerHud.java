package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;

@ModuleInfo(name = "SpeedometerHud", category = "HUD")
public class SpeedometerHud extends ravex.modules.Module {
    public final ModeParameter unit = new ModeParameter("Unit", "BPS", java.util.List.of("BPS", "KMH"));
    public final BooleanParameter shadow = new BooleanParameter("Shadow", true);

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = Identifier.fromNamespaceAndPath("ravex", "hud_white/speedometer");

    public static SpeedometerHud itz() {
        return ravex.manager.ModuleManager.delegate(SpeedometerHud.class);
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SpeedometerHud").getEnabled();
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
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

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
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