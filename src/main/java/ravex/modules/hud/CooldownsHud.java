package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import java.util.*;
public class CooldownsHud extends Module {
    public static final CooldownsHud INSTANCE = new CooldownsHud();
    private CooldownsHud() {
        super("Cooldowns", 10, 260, 90, 14);
        addParameter(new ColorParameter("Color", 0xFFFFCC33));
        addParameter(new BooleanParameter("Shadow", true));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
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
            ItemStack stack = inv.getItem(slot);
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
            int nw = ravex.utility.render.HudRenderer.textWidth(line) + 10;
            if (nw > pw) pw = nw;
        }
        int ph = lines.size() * lh + 6;
        setWidth(pw);
        setHeight(ph);
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        int cy = by + 4;
        for (var line : lines) {
            ravex.utility.render.HudRenderer.drawText(graphics, line, bx + 5, cy, col, shadow);
            cy += lh;
        }
    }
}
