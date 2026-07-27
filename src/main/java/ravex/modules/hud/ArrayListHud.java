package ravex.modules.hud;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRendererUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@ModuleInfo(name = "ArrayListHud", category = "HUD")
public class ArrayListHud extends ravex.modules.Module {
    @Parameter(name = "Shadow")
    public boolean shadow = true;
    @Parameter(name = "Case", modes = {"Normal", "Lowercase", "UPPERCASE"})
    public String _case = "Normal";
    @Parameter(name = "AnimationSpeed", min = 0.0, max = 12.0, step = 0.5)
    public double animationSpeed = 4.0;

    public int x;
    public int y;
    public int width;
    public int height;
private final Map<String, Float> animProgress = new LinkedHashMap<>();
    private final Map<String, Long> entryStartTime = new HashMap<>();
    private long enableTime = 0;

    protected void onEnable() {
        enableTime = System.currentTimeMillis();
        animProgress.clear();
        entryStartTime.clear();
    }
    protected void onDisable() {
        animProgress.clear();
        entryStartTime.clear();
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
        boolean shadow = true;
        String caseMode = "Normal";
        double animSpeed = 4.0;
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) shadow = bp.getValue();
            if (p instanceof ModeParameter mp && mp.getName().equals("Case")) caseMode = mp.getValue();
            if (p instanceof NumberParameter np && np.getName().equals("AnimationSpeed")) animSpeed = np.getValue();
        }
        List<ravex.modules.Module> allModules = ravex.manager.ModuleManager.INSTANCE.getClickGuiModules();

        long now = System.currentTimeMillis();
        java.util.Map<ravex.modules.Module, String> casedNames = new java.util.LinkedHashMap<>();
        for (ravex.modules.Module m : allModules) {
            if ("Client".equals(m.getCategory())) continue;
            String name = m.getName();
            switch (caseMode) {
                case "Lowercase": name = name.toLowerCase(); break;
                case "UPPERCASE": name = name.toUpperCase(); break;
            }
            casedNames.put(m, name);
        }

        List<java.util.Map.Entry<ravex.modules.Module, String>> modList = new ArrayList<>(casedNames.entrySet());
        modList.sort((a, b) -> Integer.compare(FontRenderUtility.getStringWidth(b.getValue()), FontRenderUtility.getStringWidth(a.getValue())));

        float speed = (float) (animSpeed * 0.035f);
        int idx = 0;
        List<String> activeNames = new ArrayList<>();
        for (var entry : modList) {
            String name = entry.getValue();
            boolean enabled = entry.getKey().getEnabled();

            if (enabled) {
                if (!entryStartTime.containsKey(name)) {
                    entryStartTime.put(name, now - (long)(idx * 30));
                }
                long startTime = entryStartTime.get(name);
                float elapsed = (now - startTime) / 600f;
                float prog = Math.min(1.0f, elapsed);
                prog = 1.0f - (1.0f - prog) * (1.0f - prog);
                animProgress.put(name, prog);
                if (prog > 0.01f) activeNames.add(name);
            } else {
                entryStartTime.remove(name);
                Float current = animProgress.get(name);
                if (current != null) {
                    current -= speed;
                    if (current <= 0.01f) {
                        animProgress.remove(name);
                    } else {
                        animProgress.put(name, current);
                        activeNames.add(name);
                    }
                }
            }
            idx++;
        }

        if (animProgress.isEmpty()) return;

        int bx = x, by = y;
        int lh = 12;
        int maxTextW = 10;
        for (String n : activeNames) {
            int nw = FontRenderUtility.getStringWidth(n);
            if (nw > maxTextW) maxTextW = nw;
        }
        int pw = 3 + maxTextW + 5;

        float totalH = 4f;
        for (String n : activeNames) {
            totalH += lh;
        }
        int ph = Math.round(totalH);

        width = pw;
        height = ph;

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int tx = bx + 3;
        int cy = by + 3;
        idx = 0;
        long time = System.currentTimeMillis();
        float angleBase = (time % 4000) / 4000f * (float) Math.PI * 2;
        for (String n : activeNames) {
            float prog = animProgress.get(n);
            int offsetY = Math.round((1.0f - prog) * 30);
            int itemAlpha = Math.round(255 * Math.min(1.0f, prog * 1.5f));

            int charX = tx;
            for (int ci = 0; ci < n.length(); ci++) {
                String ch = String.valueOf(n.charAt(ci));
                float angle = angleBase + (idx * 8f + ci) * 0.35f;
                float blend = (float) Math.sin(angle) * 0.5f + 0.5f;
                int chColor = ColorUtility.interpolate(0xFFFFFFFF, 0xFF0055EE, blend);
                HudRendererUtility.drawText(graphics, ch, charX, cy + offsetY, (chColor & 0xFFFFFF) | (itemAlpha << 24), shadow);
                charX += FontRenderUtility.getStringWidth(ch);
            }

            cy += lh;
            idx++;
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ArrayListHud").getEnabled();
    }

    public static ArrayListHud itz() {
        return ravex.manager.ModuleManager.delegate(ArrayListHud.class);
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