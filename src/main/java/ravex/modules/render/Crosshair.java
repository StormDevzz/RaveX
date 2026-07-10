package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.gui.clickgui.ColorUtility;
public class Crosshair extends Module {
<<<<<<< HEAD
=======
    public static final Crosshair INSTANCE = new Crosshair();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
        java.util.List.of("Normal", "Circle", "Triangle"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final ColorParameter dotColor = new ColorParameter("DotColor", 0xFFFF3333);
    public final NumberParameter size = new NumberParameter("Size", 4.0, 2.0, 10.0, 0.5);
    public final NumberParameter gap = new NumberParameter("Gap", 3.0, 1.0, 10.0, 0.5);
    public final NumberParameter thickness = new NumberParameter("Thickness", 1.5, 1.0, 4.0, 0.5);
    public final BooleanParameter dot = new BooleanParameter("Dot", true);
    public final BooleanParameter dynamic = new BooleanParameter("Dynamic", true);
    public final NumberParameter hitEffect = new NumberParameter("HitEffect", 6.0, 0.0, 16.0, 0.5);
    public final NumberParameter hitDuration = new NumberParameter("HitDuration", 250.0, 50.0, 500.0, 25.0);
    public final NumberParameter moveEffect = new NumberParameter("MoveEffect", 3.0, 0.0, 10.0, 0.5);
    private long lastHitTime = 0;
    private long lastFrameTime = 0;
    private float hitSpread = 0;
    private float targetProgress = 0f;
    private float continuousRotation = 0f;
    private float moveSpreadAnim = 0f;

    private Crosshair() {
        super("Crosshair");
        dotColor.setVisible(dot::getValue);
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======

>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
    @Subscribe
    public void onAttack(AttackEvent event) {
        onHit();
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public void onHit() {
        lastHitTime = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
    }

    public void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float delta = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (delta > 0.1f) delta = 0.016f;

        float currentMoveSpread = dynamic.getValue() ? calcMoveSpread(mc) : 0f;
        moveSpreadAnim += (currentMoveSpread - moveSpreadAnim) * Math.min(1.0f, delta * 12f);

        long elapsed = now - lastHitTime;
        float dur = hitDuration.getValue().floatValue();
        float hitSpin = 0f;
        float hitScale = 1.0f;
        float hitFlashProgress = 0f;
        if (elapsed < dur) {
            float progress = elapsed / dur;
            float overshoot = 1.0f + 0.3f * (float) Math.sin(progress * Math.PI * 2) * (1.0f - progress);
            hitSpread = hitEffect.getValue().floatValue() * (1.0f - progress) * overshoot;
            hitSpin = (float) Math.PI * 0.5f * (1.0f - progress) * (1.0f - progress);
            hitScale = 1.0f + 0.15f * (1.0f - progress);
            hitFlashProgress = 1.0f - progress;
        } else {
            hitSpread = 0;
        }

        boolean hasTarget = mc.crosshairPickEntity != null && mc.crosshairPickEntity.isAlive();
        if (hasTarget) {
            targetProgress = Math.min(1.0f, targetProgress + delta * 6.0f);
        } else {
            targetProgress = Math.max(0.0f, targetProgress - delta * 6.0f);
        }

        if (targetProgress > 0.01f) {
            continuousRotation += delta * 2.0f;
        } else {
            continuousRotation = 0f;
        }

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        int col = color.getValue();
        int lockColor = 0xFFFF3333;
        int currentColor = lerpColor(col, lockColor, targetProgress);

        if (hitFlashProgress > 0.01f) {
            currentColor = blendSrcOver(currentColor, ColorUtility.withAlpha(0xFFFFFFFF, (int)(180 * hitFlashProgress)));
        }

        float baseSize = size.getValue().floatValue() * hitScale;
        float baseGap = gap.getValue().floatValue();
        float thick = thickness.getValue().floatValue();
        float totalSpread = baseGap + hitSpread + moveSpreadAnim + targetProgress * 1.5f;

        float totalSpin = hitSpin + targetProgress * continuousRotation;

        switch (mode.getValue()) {
            case "Normal" -> renderNormal(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
            case "Circle" -> renderCircle(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
            case "Triangle" -> renderTriangle(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
        }

        if (dot.getValue()) {
            int dc = dotColor.getValue();
            float dotSize = 2.5f;
            net.minecraft.resources.Identifier dotTex = ravex.utility.render.Render2DEngine.getSmoothCircle();
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx - dotSize / 2f, cy - dotSize / 2f);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, dotTex, 0, 0, 0f, 0f, (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), dc);
            graphics.pose().popMatrix();
        }
    }

    private float calcMoveSpread(Minecraft mc) {
        float moveEff = moveEffect.getValue().floatValue();
        if (moveEff <= 0) return 0;
        var player = mc.player;
        double velX = player.getX() - player.xo;
        double velZ = player.getZ() - player.zo;
        double hSpeed = Math.sqrt(velX * velX + velZ * velZ) * 20.0;
        float spread = 0;
        if (player.isSprinting()) spread += moveEff * 0.8f;
        else if (hSpeed > 0.05) spread += moveEff * 0.4f;
        if (!player.onGround()) spread += moveEff * 0.6f;
        return Math.min(spread, moveEff * 2);
    }

    private void renderNormal(GuiGraphics g, int cx, int cy, float size, float gap, float thick, float spin, int color) {
        float end = gap + size;
        float half = thick / 2.0f;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (spin != 0) {
            g.pose().rotate(spin);
        }
        g.fill((int) -end, (int) -half, (int) -gap, (int) Math.ceil(half), color);
        g.fill((int) gap, (int) -half, (int) end, (int) Math.ceil(half), color);
        g.fill((int) -half, (int) -end, (int) Math.ceil(half), (int) -gap, color);
        g.fill((int) -half, (int) gap, (int) Math.ceil(half), (int) end, color);
        g.pose().popMatrix();
    }

    private void renderCircle(GuiGraphics g, int cx, int cy, float size, float gap, float thick, float spin, int color) {
        float radius = gap + size;
        float thicknessRatio = thick / radius;
        net.minecraft.resources.Identifier ringTex = ravex.utility.render.Render2DEngine.getSmoothRing(thicknessRatio);

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (spin != 0) {
            g.pose().rotate(spin);
        }
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ringTex, (int) -radius, (int) -radius, 0f, 0f, (int) (radius * 2), (int) (radius * 2), (int) (radius * 2), (int) (radius * 2), color);
        g.pose().popMatrix();
    }

    private void renderTriangle(GuiGraphics g, int cx, int cy, float size, float gap, float thick, float spin, int color) {
        float radius = gap + size;
        float tipY = -radius;
        float botY = radius * 0.6f;
        float leftX = -radius * 0.8f;
        float rightX = radius * 0.8f;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (spin != 0) {
            g.pose().rotate(spin);
        }
        drawThickLine(g, 0, tipY, leftX, botY, thick, color);
        drawThickLine(g, leftX, botY, rightX, botY, thick, color);
        drawThickLine(g, rightX, botY, 0, tipY, thick, color);
        g.pose().popMatrix();
    }

    private void drawThickLine(GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        float angle = (float) Math.atan2(dy, dx);
        g.pose().pushMatrix();
        g.pose().translate(x1, y1);
        g.pose().rotate(angle);
        g.pose().scale(1.0f, thickness);
        g.pose().translate(0.0f, -0.5f);
        g.fill(0, 0, (int) Math.ceil(len), 1, color);
        g.pose().popMatrix();
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======

    private static int lerpColor(int from, int to, float ratio) {
        if (ratio <= 0f) return from;
        if (ratio >= 1f) return to;
        int a1 = (from >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;

        int a2 = (to >> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendSrcOver(int dst, int src) {
        int sa = (src >> 24) & 0xFF;
        if (sa == 255) return src;
        if (sa == 0) return dst;
        int da = (dst >> 24) & 0xFF;
        int dr = (dst >> 16) & 0xFF;
        int dg = (dst >> 8) & 0xFF;
        int db = dst & 0xFF;

        int sr = (src >> 16) & 0xFF;
        int sg = (src >> 8) & 0xFF;
        int sb = src & 0xFF;

        int a = sa + da * (255 - sa) / 255;
        int r = (sr * sa + dr * da * (255 - sa) / 255) / (a == 0 ? 1 : a);
        int g = (sg * sa + dg * da * (255 - sa) / 255) / (a == 0 ? 1 : a);
        int b = (sb * sa + db * da * (255 - sa) / 255) / (a == 0 ? 1 : a);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
    public static boolean maybeEnabled() {
        return maybeEnabled(Crosshair.class);
    }

    public static Crosshair itz() {
        return ModuleManager.get(Crosshair.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
