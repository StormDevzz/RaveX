package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.client.Hud;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("BindListHud")
public class BindListHud extends ravex.modules.Module {
    @Parameter(name = "Mode", modes = {"EnabledBinds", "AllBinds"})
    public String mode = "EnabledBinds";
    @Parameter(name = "ShowKey")
    public boolean showKey = true;
    @Parameter(name = "Shadow")
    public boolean shadow = true;
    @Parameter(name = "AnimationSpeed", min = 0.0, max = 12.0, step = 0.5)
    public double animationSpeed = 4.0;

    private static final String IGNORED = "ClickGui";

    private record Row(String key, String name, String bind, boolean enabled) {}

    private static class EntryAnim {
        final AnimationUtility.SpringAnimation spring = new AnimationUtility.SpringAnimation(0f);
        float progress = 0f;
        EntryAnim() {
            spring.stiffness(120f).damping(2f * (float) Math.sqrt(120f)).mass(1f);
            spring.setTarget(0f);
        }
    }

    private final Map<String, EntryAnim> animations = new HashMap<>();
    private final AnimationUtility.SpringAnimation panel = new AnimationUtility.SpringAnimation(0f);
    private long lastFrameNs = System.nanoTime();

    private BindListHud() {
        super("BindListHud", 10, 10, 120, 22);
    }

    protected void onEnable() {
        animations.clear();
        panel.setTarget(0f);
    }

    protected void onDisable() {
        animations.clear();
        panel.setTarget(0f);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        boolean enabledOnly = "EnabledBinds".equals(mode);

        List<Row> rows = new ArrayList<>();
        for (ravex.modules.Module m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
            if (IGNORED.equals(m.getName())) continue;
            int bind = m.getKeyBind();
            if (bind <= 0) continue;
            if (enabledOnly && !m.getEnabled()) continue;
            // keyed by shown text, renames fade back in
            String name = m.getName();
            String bindName = ravex.parameter.KeybindParameter.getKeyName(bind);
            rows.add(new Row(name + "|" + bindName, name, bindName, m.getEnabled()));
        }
        rows.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        var mc = MinecraftWrapper.getWrapper();
        boolean inEditor = mc.getScreen() instanceof ravex.gui.hudeditor.HudEditorScreen;

        if (rows.isEmpty() && !inEditor) {
            animations.clear();
            panel.setTarget(0f);
            return;
        }

        // own clock, other hooks eat the shared timer
        long now = System.nanoTime();
        float delta = Math.min(50f, (now - lastFrameNs) / 1_000_000f);
        lastFrameNs = now;
        float stiffness = (float) (180f + animationSpeed * 40f);
        float damping = (float) (2f * Math.sqrt(stiffness));

        for (Row row : rows) {
            EntryAnim anim = animations.computeIfAbsent(row.key(), k -> new EntryAnim());
            anim.spring.stiffness(stiffness).damping(damping);
            anim.spring.setTarget(1f);
            anim.spring.update(delta);
            anim.progress = Math.max(0f, Math.min(1f, anim.spring.getValue()));
        }
        for (Iterator<Map.Entry<String, EntryAnim>> it = animations.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EntryAnim> entry = it.next();
            boolean present = false;
            for (Row row : rows) {
                if (row.key().equals(entry.getKey())) {
                    present = true;
                    break;
                }
            }
            if (present) continue;
            EntryAnim anim = entry.getValue();
            anim.spring.stiffness(stiffness).damping(damping);
            anim.spring.setTarget(0f);
            anim.spring.update(delta);
            anim.progress = Math.max(0f, Math.min(1f, anim.spring.getValue()));
            if (anim.progress < 0.004f && anim.spring.isSettled()) it.remove();
        }

        boolean anyVisible = !rows.isEmpty();
        for (EntryAnim anim : animations.values()) {
            if (anim.progress > 0.004f) {
                anyVisible = true;
                break;
            }
        }
        if (!anyVisible && !inEditor) {
            animations.clear();
            panel.setTarget(0f);
            return;
        }

        panel.stiffness(90f).damping(2f * (float) Math.sqrt(90f));
        panel.setTarget(anyVisible ? 1f : 0f);
        panel.update(delta);
        float panelAlpha = Math.max(0f, Math.min(1f, panel.getValue()));

        int bx = getX();
        int by = getY();
        if (rows.isEmpty()) {
            // editor placeholder, keeps the element selectable
            setWidth(88);
            setHeight(18);
            drawPanel(graphics, bx, by, 88, 18, 1f);
            HudRendererUtility.drawText(graphics, "BindList", bx + 6, by + 5, 0x88FFFFFF, shadow);
            return;
        }

        int lineHeight = 12;
        int keyExtra = 0;
        if (showKey) {
            for (Row row : rows) {
                int w = FontRenderUtility.getStringWidth("[" + row.bind() + "]");
                if (w > keyExtra) keyExtra = w;
            }
            keyExtra += 5;
        }
        int maxTextW = 60;
        for (Row row : rows) {
            int w = FontRenderUtility.getStringWidth(row.name()) + keyExtra;
            if (w > maxTextW) maxTextW = w;
        }

        float rowsHeight = 0f;
        for (Map.Entry<String, EntryAnim> entry : animations.entrySet()) {
            rowsHeight += lineHeight * entry.getValue().progress;
        }

        int panelWidth = 3 + maxTextW + 6;
        int panelHeight = Math.max(8, 4 + Math.round(rowsHeight * panelAlpha));
        setWidth(panelWidth);
        setHeight(panelHeight);
        drawPanel(graphics, bx, by, panelWidth, panelHeight, panelAlpha);

        int ty = by + 3;
        for (Row row : rows) {
            EntryAnim anim = animations.get(row.key());
            float progress = anim != null ? anim.progress : 1f;
            if (progress <= 0.004f) continue;
            float eased = AnimationUtility.Easing.CUBIC_OUT.apply(Math.min(1f, progress));
            float rowAlpha = Math.min(1f, progress * 1.5f) * panelAlpha;
            int y = ty + Math.round((1f - eased) * 6f);
            // name types in first, bind letters follow
            float nameReveal = Math.min(1f, progress * 1.6f);
            float keyReveal = Math.max(0f, Math.min(1f, (progress - 0.45f) / 0.55f));
            int nameColor = ColorUtility.withAlpha(row.enabled() ? 0xFFFFFFFF : 0xFF9A9AA5, Math.round(255 * rowAlpha));
            drawRevealText(graphics, row.name(), bx + 3, y, nameColor, shadow, nameReveal);
            if (showKey) {
                int keyColor = ColorUtility.withAlpha(row.enabled() ? 0xFF3F8CFF : 0xFF808090, Math.round(255 * rowAlpha));
                drawRevealText(graphics, "[" + row.bind() + "]",
                    bx + 3 + FontRenderUtility.getStringWidth(row.name()) + 5, y, keyColor, shadow, keyReveal);
            }
            ty += Math.round(lineHeight * eased);
        }
    }

    private static void drawRevealText(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow, float reveal) {
        if (reveal <= 0f) return;
        int baseAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            float charReveal = Math.max(0f, Math.min(1f, reveal * text.length() - i));
            if (charReveal <= 0f) break;
            String ch = String.valueOf(text.charAt(i));
            int alpha = Math.round(baseAlpha * charReveal);
            HudRendererUtility.drawText(graphics, ch, cx, y, rgb | (alpha << 24), shadow);
            cx += FontRenderUtility.getStringWidth(ch);
        }
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, float alpha) {
        HudRendererUtility.drawBackground(graphics, x, y, w, h, alpha);
    }
}
