package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("CooldownsHud")
public class CooldownsHud extends ravex.modules.Module {
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFCC33;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

private static final Identifier ICON = TextureLoaderUtility.HUD_COOLDOWN_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        int col = this.color;
        boolean shadow = this.shadow;
        var cd = mc.getPlayer().getCooldowns();
        Set<String> seen = new HashSet<>();
        List<String> lines = new ArrayList<>();
        var inv = mc.getPlayer().getInventory();
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
        int bx = getX(), by = getY();
        int lh = 10;
        int pw = 10;
        for (var line : lines) {
            int nw = HudRendererUtility.textWidth(line) + 10;
            if (nw > pw) pw = nw;
        }
        pw = 4 + pw + 4 + IS + 4;
        int ph = lines.size() * lh + 8;
        setWidth(pw);
        setHeight(ph);
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cy = by + 5;
        for (var line : lines) {
            HudRendererUtility.drawText(graphics, line, bx + 4, cy, col, shadow);
            cy += lh;
        }
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }
}
