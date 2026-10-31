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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ravex.modules.Modules;
import ravex.mcwrapper.MinecraftWrapper;
@HudModule("ArrayListHud")
public class ArrayListHud extends ravex.modules.Module {
    @Parameter(name = "Shadow")
    public boolean shadow = true;
    @Parameter(name = "Case", modes = {"Normal", "Lowercase", "UPPERCASE"})
    public String _case = "Normal";
    @Parameter(name = "Sort", modes = {"Biggest", "Smallest", "A-Z", "Z-A", "Default"})
    public String sort = "Biggest";
    @Parameter(name = "Align", modes = {"Left", "Right", "Auto"})
    public String align = "Left";

    private static class EntryAnim {
        final AnimationUtility.SpringAnimation spring;
        float opacity;
        EntryAnim() {
            this.spring = new AnimationUtility.SpringAnimation(0f);
            this.spring.stiffness(120f).damping(2f * (float) Math.sqrt(120f)).mass(1f);
            this.spring.setTarget(0f);
            this.opacity = 0f;
        }
    }

    private final Map<String, EntryAnim> entryAnims = new HashMap<>();
    private final AnimationUtility.SpringAnimation panelOpacity = new AnimationUtility.SpringAnimation(0f);
    private boolean hadEnabled = false;
    private long lastFrameNanos = System.nanoTime();

    {
        panelOpacity.stiffness(80f).damping(2f * (float) Math.sqrt(80f)).mass(1f);
    }

    protected void onEnable() {
        entryAnims.clear();
        hadEnabled = false;
    }
    protected void onDisable() {
        entryAnims.clear();
        hadEnabled = false;
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        boolean shadow = this.shadow;
        String caseMode = this._case;
        String sortMode = this.sort;
        List<ravex.modules.Module> allModules = ravex.manager.ModuleManager.INSTANCE.getClickGuiModules();

        // own frame clock, shared delta gets eaten
        long now = System.nanoTime();
        float delta = Math.min(50f, (now - lastFrameNanos) / 1_000_000f);
        lastFrameNanos = now;
        float stiffness = 340f;
        // critically damped, fast and no bounce
        float damping = (float) (2f * Math.sqrt(stiffness));

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
        switch (sortMode) {
            case "Smallest" -> modList.sort((a, b) -> {
                int byWidth = Integer.compare(FontRenderUtility.getStringWidth(a.getValue()), FontRenderUtility.getStringWidth(b.getValue()));
                return byWidth != 0 ? byWidth : String.CASE_INSENSITIVE_ORDER.compare(a.getValue(), b.getValue());
            });
            case "A-Z" -> modList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getValue(), b.getValue()));
            case "Z-A" -> modList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(b.getValue(), a.getValue()));
            case "Default" -> { }
            default -> modList.sort((a, b) -> {
                int byWidth = Integer.compare(FontRenderUtility.getStringWidth(b.getValue()), FontRenderUtility.getStringWidth(a.getValue()));
                return byWidth != 0 ? byWidth : String.CASE_INSENSITIVE_ORDER.compare(a.getValue(), b.getValue());
            });
        }

        boolean anyEnabled = false;
        int idx = 0;
        for (var entry : modList) {
            String name = entry.getValue();
            boolean enabled = entry.getKey().getEnabled();

            EntryAnim anim = entryAnims.computeIfAbsent(name, k -> new EntryAnim());
            anim.spring.stiffness(stiffness).damping(damping);

            if (enabled) {
                anim.spring.setTarget(1f);
                anyEnabled = true;
            } else {
                anim.spring.setTarget(0f);
            }

            anim.spring.update(delta);
            anim.opacity = Math.max(0f, Math.min(1f, anim.spring.getValue()));
            idx++;
        }

        entryAnims.entrySet().removeIf(e -> {
            float val = e.getValue().spring.getValue();
            return val < 0.005f && e.getValue().spring.isSettled() && !isModuleEnabled(e.getKey(), modList);
        });

        panelOpacity.stiffness(40f).damping(2f * (float) Math.sqrt(40f));
        if (anyEnabled) {
            panelOpacity.setTarget(1f);
            hadEnabled = true;
        } else if (hadEnabled) {
            panelOpacity.setTarget(0f);
        }
        panelOpacity.update(delta);

        float panelAlpha = Math.max(0f, Math.min(1f, panelOpacity.getValue()));
        var mcEditor = MinecraftWrapper.getWrapper();
        boolean inEditor = mcEditor.getScreen() instanceof ravex.gui.hudeditor.HudEditorScreen;
        if (panelAlpha < 0.01f && !anyEnabled && !inEditor) return;

        List<String> activeNames = new ArrayList<>();
        for (var entry : modList) {
            String name = entry.getValue();
            EntryAnim anim = entryAnims.get(name);
            if (anim != null && anim.opacity > 0.005f) {
                activeNames.add(name);
            }
        }

        // editor needs a handle to drag when nothing is on
        if (activeNames.isEmpty() && !inEditor) return;

        float panelProg = AnimationUtility.Easing.CUBIC_OUT.apply(Math.min(1f, panelAlpha * 2f));
        int bx = getX(), by = getY();
        int lh = 12;
        if (activeNames.isEmpty()) {
            int ph2 = 18;
            setWidth(80);
            setHeight(ph2);
            boolean rightSide = isRightSide(graphics, bx, 80);
            HudRendererUtility.drawBackground(graphics, bx, by, 80, ph2);
            int labelW = FontRenderUtility.getStringWidth("ArrayList");
            FontRenderUtility.drawString(graphics, "ArrayList", rightSide ? bx + 80 - 6 - labelW : bx + 6, by + 5, 0x88FFFFFF, shadow);
            return;
        }
        int maxTextW = 10;
        for (String n : activeNames) {
            int nw = FontRenderUtility.getStringWidth(n);
            if (nw > maxTextW) maxTextW = nw;
        }
        int pw = 3 + maxTextW + 5;

        float totalH = 4f;
        for (String n : activeNames) {
            EntryAnim anim = entryAnims.get(n);
            totalH += lh * Math.max(0.01f, anim != null ? anim.opacity : 1f);
        }
        int ph = Math.round(totalH * panelProg);

        setWidth(pw);
        setHeight(ph);

        boolean rightSide = isRightSide(graphics, bx, pw);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int tx = rightSide ? bx + pw - 3 : bx + 3;
        int cy = by + 3;
        idx = 0;
        long time = System.currentTimeMillis();
        float angleBase = (time % 4000) / 4000f * (float) Math.PI * 2;

        // mirror mode: rows are anchored to the right edge and clipped to the panel
        // so the reversed slide-in never pokes outside
        Render2DUtility.pushScissor(graphics, bx, by, pw, ph);
        for (String n : activeNames) {
            EntryAnim anim = entryAnims.get(n);
            if (anim == null) continue;
            float prog = anim.opacity;
            float springVal = AnimationUtility.Easing.CUBIC_OUT.apply(Math.min(1f, prog * 3f));

            int offsetY = Math.round((1f - springVal) * 20 * (1f - prog * 0.5f));
            int itemAlpha = Math.round(255 * Math.min(1f, prog * 2f));
            int slideOffset = Math.round((1f - springVal) * 30);

            int startX = rightSide ? tx - FontRenderUtility.getStringWidth(n) : tx;
            // left: enters shifted right of its spot, right: enters shifted left of it
            int charX = rightSide ? startX - slideOffset : startX + slideOffset;
            for (int ci = 0; ci < n.length(); ci++) {
                String ch = String.valueOf(n.charAt(ci));
                float angle = angleBase + (idx * 8f + ci) * 0.35f;
                float blend = (float) Math.sin(angle) * 0.5f + 0.5f;
                int chColor = ColorUtility.interpolate(0xFFFFFFFF, 0xFF0055EE, blend);
                HudRendererUtility.drawText(graphics, ch, charX, cy + offsetY, ColorUtility.setAlpha(chColor, itemAlpha), shadow);
                charX += FontRenderUtility.getStringWidth(ch);
            }

            cy += lh * springVal;
            idx++;
        }
        Render2DUtility.popScissor(graphics);
    }

    // right aligns to the panel's right edge; Auto picks the side by screen half
    private boolean isRightSide(GuiGraphics graphics, int bx, int pw) {
        if ("Right".equals(align)) return true;
        if ("Auto".equals(align)) return bx + pw / 2 > graphics.guiWidth() / 2;
        return false;
    }

    private boolean isModuleEnabled(String name, List<java.util.Map.Entry<ravex.modules.Module, String>> modList) {
        for (var entry : modList) {
            if (entry.getValue().equals(name)) {
                return entry.getKey().getEnabled();
            }
        }
        return false;
    }
}
