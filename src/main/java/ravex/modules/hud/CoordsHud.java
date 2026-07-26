package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;

@ModuleInfo(name = "CoordsHud", category = "HUD")
public class CoordsHud extends ravex.modules.Module {
    public final BooleanParameter shadow = new BooleanParameter("Shadow", true);
    public final BooleanParameter coloredLabels = new BooleanParameter("ColoredLabels", true);

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_COORDS_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int ac = ColorUtility.getActiveColor();
        boolean shadow = true;
        boolean colored = true;
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
            if (p instanceof BooleanParameter bp && bp.getName().equals("ColoredLabels")) colored = bp.getValue();
        }
        int bx = x, by = y;
        String xStr = String.format("%.1f", player.getX());
        String yStr = String.format("%.1f", player.getY());
        String zStr = String.format("%.1f", player.getZ());
        String full = (colored ? "X " : "") + xStr + (colored ? " Y " : " / ") + yStr + (colored ? " Z " : " / ") + zStr;
        int pw = 4 + HudRendererUtility.textWidth(full) + 4 + IS + 4;
        int ph = 14;
        width = pw;
        height = ph;
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cx = bx + 4;
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
        if (colored) {
            HudRendererUtility.drawText(graphics, "X ", cx, by + 2, 0xFFFF4455, shadow);
            cx += HudRendererUtility.textWidth("X ");
            HudRendererUtility.drawText(graphics, xStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRendererUtility.textWidth(xStr);
            HudRendererUtility.drawText(graphics, " Y ", cx, by + 2, 0xFF44FF88, shadow);
            cx += HudRendererUtility.textWidth(" Y ");
            HudRendererUtility.drawText(graphics, yStr, cx, by + 2, 0xFFD0D0E0, shadow);
            cx += HudRendererUtility.textWidth(yStr);
            HudRendererUtility.drawText(graphics, " Z ", cx, by + 2, 0xFF44AAFF, shadow);
            cx += HudRendererUtility.textWidth(" Z ");
            HudRendererUtility.drawText(graphics, zStr, cx, by + 2, 0xFFD0D0E0, shadow);
        } else {
            HudRendererUtility.drawText(graphics, full, cx, by + 2, 0xFFD0D0E0, shadow);
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("CoordsHud").getEnabled();
    }

    public static CoordsHud itz() {
        return ravex.manager.ModuleManager.delegate(CoordsHud.class);
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