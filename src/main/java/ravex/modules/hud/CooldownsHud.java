package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import java.util.*;

@ModuleInfo(name = "CooldownsHud", category = "HUD")
public class CooldownsHud extends ravex.modules.Module {
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFCC33);
    public final BooleanParameter shadow = new BooleanParameter("Shadow", true);

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoader.HUD_COOLDOWN_WHITE;
    private static final int IS = HudRenderer.getIconSize();

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int col = 0xFFFFCC33;
        boolean shadow = true;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) col = cp.getValue();
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
        }
        ItemCooldowns cd = mc.player.getCooldowns();
        Set<String> seen = new HashSet<>();
        List<String> lines = new ArrayList<>();
        var inv = mc.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            var stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getDescriptionId();
            if (seen.add(id) && cd.isOnCooldown(stack)) {
                float pct = cd.getCooldownPercent(stack, 0);
                if (pct <= 0.001f) continue;
                lines.add(stack.getHoverName().getString() + " " + Math.round(pct * 100) + "%");
            }
        }
        if (lines.isEmpty()) return;
        int bx = x, by = y;
        int lh = 10;
        int pw = 10;
        for (var line : lines) {
            int nw = HudRenderer.textWidth(line) + 10;
            if (nw > pw) pw = nw;
        }
        pw = 4 + pw + 4 + IS + 4;
        int ph = lines.size() * lh + 8;
        width = pw;
        height = ph;
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int cy = by + 5;
        for (var line : lines) {
            HudRenderer.drawText(graphics, line, bx + 4, cy, col, shadow);
            cy += lh;
        }
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("CooldownsHud").getEnabled();
    }

    public static CooldownsHud itz() {
        return ravex.manager.ModuleManager.delegate(CooldownsHud.class);
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