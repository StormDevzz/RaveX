package ravex.modules.hud;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.HudRenderer;
import java.util.ArrayList;
import java.util.List;
public class ActiveModulesHud extends Module {
    public static final ActiveModulesHud INSTANCE = new ActiveModulesHud();
    private ActiveModulesHud() {
        super("ActiveModules", 10, 40, 90, 100);
        addParameter(new BooleanParameter("Shadow", true));
        addParameter(new ModeParameter("Highlight", "ActiveColor",
            java.util.List.of("ActiveColor", "Rainbow", "Gradient")));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        boolean shadow = false;
        String highlightMode = "ActiveColor";
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
            if (p instanceof ModeParameter mp && mp.getName().equals("Highlight")) highlightMode = mp.getValue();
        }
        List<String> names = new ArrayList<>();
        for (Module m : ModuleManager.INSTANCE.getClickGuiModules()) {
            if (m.getEnabled()) names.add(m.getName());
        }
        if (names.isEmpty()) return;
        int bx = getX(), by = getY();
        int lh = 10;
        int pw = 10;
        for (String n : names) {
            int nw = HudRenderer.textWidth(n) + 2;
            if (nw > pw) pw = nw;
        }
        int cy = by;
        int idx = 0;
        int activeColor = ColorUtility.getActiveColor();
        for (String n : names) {
            int color;
            switch (highlightMode) {
                case "Rainbow":
                    color = ColorUtility.getRainbowColor(idx, 4000);
                    break;
                case "Gradient":
                    color = ColorUtility.getColorRGB(idx);
                    break;
                default:
                    color = (idx % 2 == 0) ? activeColor
                        : ColorUtility.darker(activeColor, 0.6f);
                    break;
            }
            HudRenderer.drawText(graphics, n, bx, cy, color, shadow);
            cy += lh;
            idx++;
        }
        setWidth(pw);
        setHeight(names.size() * lh);
    }
}
