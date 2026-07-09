package ravex.modules.hud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public class ArrayListHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_ARRAYLIST_WHITE;
    private final Map<String, Float> animProgress = new LinkedHashMap<>();
    private ArrayListHud() {
        super("ArrayList", 10, 320, 90, 100);
        addParameter(new BooleanParameter("Shadow", true));
        addParameter(new ModeParameter("Highlight", "ActiveColor",
            java.util.List.of("ActiveColor", "Rainbow", "Gradient", "GradientChar")));
        addParameter(new ModeParameter("Case", "Normal",
            java.util.List.of("Normal", "Lowercase", "UPPERCASE")));
        addParameter(new NumberParameter("AnimationSpeed", 4.0, 0.0, 12.0, 0.5));
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
        boolean shadow = true;
        String highlightMode = "ActiveColor";
        String caseMode = "Normal";
        double animSpeed = 4.0;
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
            if (p instanceof ModeParameter mp) {
                if (mp.getName().equals("Highlight")) highlightMode = mp.getValue();
                if (mp.getName().equals("Case")) caseMode = mp.getValue();
            }
            if (p instanceof NumberParameter np && np.getName().equals("AnimationSpeed")) animSpeed = np.getValue();
        }
        List<String> rawNames = new ArrayList<>();
        for (Module m : ModuleManager.INSTANCE.getClickGuiModules()) {
            if (m.getEnabled()) rawNames.add(m.getName());
        }
        if (rawNames.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (String n : rawNames) {
            switch (caseMode) {
                case "Lowercase": names.add(n.toLowerCase()); break;
                case "UPPERCASE": names.add(n.toUpperCase()); break;
                default: names.add(n); break;
            }
        }
        for (String n : names) {
            if (!animProgress.containsKey(n)) animProgress.put(n, 0f);
            float p = animProgress.get(n);
            p += (1f - p) * (float)(animSpeed * 0.025);
            if (p > 0.99f) p = 1f;
            animProgress.put(n, p);
        }
        animProgress.keySet().retainAll(names);
        int bx = getX(), by = getY();
        int lh = 10;
        int IS = HudRenderer.getIconSize();
        int pw = 10;
        for (String n : names) {
            int nw = HudRenderer.textWidth(n) + 10;
            if (nw > pw) pw = nw;
        }
        pw = 4 + pw + 4 + IS + 4;
        int ph = names.size() * lh + 6;
        setWidth(pw);
        setHeight(ph);
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int activeColor = ColorUtility.getActiveColor();
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + 4, activeColor);
        int tx = bx + 4;
        int cy = by + 4;
        int idx = 0;
        for (String n : names) {
            float prog = animProgress.getOrDefault(n, 1f);
            int offsetX = (int)((1f - prog) * -15);
            if (highlightMode.equals("GradientChar")) {
                int charX = tx + offsetX;
                for (int ci = 0; ci < n.length(); ci++) {
                    String ch = String.valueOf(n.charAt(ci));
                    int chColor = ColorUtility.getRainbowColor(idx * 10 + ci, 3000);
                    HudRenderer.drawText(graphics, ch, charX, cy, chColor, shadow);
                    charX += FontRenderUtility.getStringWidth(ch);
                }
            } else {
                int color;
                switch (highlightMode) {
                    case "Rainbow":
                        color = ColorUtility.getRainbowColor(idx, 4000);
                        break;
                    case "Gradient":
                        color = (idx % 2 == 0) ? activeColor : ColorUtility.darker(activeColor, 0.6f);
                        break;
                    default:
                        color = (idx % 2 == 0) ? activeColor : ColorUtility.darker(activeColor, 0.6f);
                        break;
                }
                HudRenderer.drawText(graphics, n, tx + offsetX, cy, color, shadow);
            }
            cy += lh;
            idx++;
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ArrayListHud.class);
    }

    public static ArrayListHud itz() {
        return ModuleManager.get(ArrayListHud.class);
    }
}
