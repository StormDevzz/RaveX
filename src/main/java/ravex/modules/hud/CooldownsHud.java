package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import java.util.*;
import ravex.manager.ModuleManager;
public class CooldownsHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_COOLDOWN_WHITE;
    private static final int IS = HudRenderer.getIconSize();
    private CooldownsHud() {
        super("Cooldowns", 10, 260, 90, 14);
        addParameter(new ColorParameter("Color", 0xFFFFCC33));
        addParameter(new BooleanParameter("Shadow", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
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
        int bx = getX(), by = getY();
        int lh = 10;
        int pw = 10;
        for (var line : lines) {
            int nw = HudRenderer.textWidth(line) + 10;
            if (nw > pw) pw = nw;
        }
        pw = 4 + pw + 4 + IS + 4;
        int ph = lines.size() * lh + 8;
        setWidth(pw);
        setHeight(ph);
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int cy = by + 5;
        for (var line : lines) {
            HudRenderer.drawText(graphics, line, bx + 4, cy, col, shadow);
            cy += lh;
        }
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(CooldownsHud.class);
    }

    public static CooldownsHud itz() {
        return ModuleManager.get(CooldownsHud.class);
    }
}
