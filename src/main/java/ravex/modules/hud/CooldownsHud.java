package ravex.modules.hud;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import java.util.*;

@ModuleInfo(name = "CooldownsHud", category = "HUD")
public class CooldownsHud extends ravex.modules.Module {
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFCC33;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_COOLDOWN_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

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
            int nw = HudRendererUtility.textWidth(line) + 10;
            if (nw > pw) pw = nw;
        }
        pw = 4 + pw + 4 + IS + 4;
        int ph = lines.size() * lh + 8;
        width = pw;
        height = ph;
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cy = by + 5;
        for (var line : lines) {
            HudRendererUtility.drawText(graphics, line, bx + 4, cy, col, shadow);
            cy += lh;
        }
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("CooldownsHud").getEnabled();
    }

    public static CooldownsHud itz() {
        return ravex.manager.ModuleManager.delegate(CooldownsHud.class);
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