package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;

import ravex.utility.render.ColorUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Crosshair", category = "Render")
public class Crosshair {
    @Parameter(name = "Mode", modes = {"Normal", "Circle", "Triangle"})
    public String mode = "Normal";
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Parameter(name = "DotColor", color = true, visible = "dot")
    public int dotColor = 0xFFFF3333;
    @Parameter(name = "Size", min = 2.0, max = 10.0, step = 0.5)
    public double size = 4.0;
    @Parameter(name = "Gap", min = 1.0, max = 10.0, step = 0.5)
    public double gap = 3.0;
    @Parameter(name = "Thickness", min = 1.0, max = 4.0, step = 0.5)
    public double thickness = 1.5;
    @Parameter(name = "Dot")
    public boolean dot = true;
    @Parameter(name = "Dynamic")
    public boolean dynamic = true;
    @Parameter(name = "TargetEffect")
    public boolean targetEffect = true;
    @Parameter(name = "HitEffect", min = 0.0, max = 16.0, step = 0.5)
    public double hitEffect = 6.0;
    @Parameter(name = "HitDuration", min = 50.0, max = 500.0, step = 25.0)
    public double hitDuration = 250.0;
    @Parameter(name = "MoveEffect", min = 0.0, max = 10.0, step = 0.5)
    public double moveEffect = 3.0;
    private long lastHitTime = 0;
    private long lastFrameTime = 0;
    private float hitSpread = 0;
    private float targetProgress = 0f;
    private float continuousRotation = 0f;
    private float moveSpreadAnim = 0f;

    @Subscribe
    public void onAttack(AttackEvent event) {
        onHit();
    }

    public void onHit() {
        lastHitTime = System.currentTimeMillis();
    }
    public void onTick() {
    }

    public void render(GuiGraphics graphics) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getWindow() == null || mc.getPlayer() == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float delta = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (delta > 0.1f) delta = 0.016f;

        float currentMoveSpread = dynamic ? calcMoveSpread(mc) : 0f;
        moveSpreadAnim += (currentMoveSpread - moveSpreadAnim) * Math.min(1.0f, delta * 12f);

        long elapsed = now - lastHitTime;
        float dur = (float) hitDuration;
        float hitSpin = 0f;
        float hitScale = 1.0f;
        float hitFlashProgress = 0f;
        if (elapsed < dur) {
            float progress = elapsed / dur;
            float overshoot = 1.0f + 0.3f * (float) Math.sin(progress * Math.PI * 2) * (1.0f - progress);
            hitSpread = (float) hitEffect * (1.0f - progress) * overshoot;
            hitSpin = (float) Math.PI * 0.5f * (1.0f - progress) * (1.0f - progress);
            hitScale = 1.0f + 0.15f * (1.0f - progress);
            hitFlashProgress = 1.0f - progress;
        } else {
            hitSpread = 0;
        }

        boolean hasTarget = targetEffect && mc.getCrosshairPickEntity() != null && mc.getCrosshairPickEntity().isAlive();
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

        int col = color;
        int lockColor = dotColor;
        int currentColor = ColorUtility.interpolate(col, lockColor, targetProgress);

        if (hitFlashProgress > 0.01f) {
            currentColor = ColorUtility.overlay(currentColor, ColorUtility.withAlpha(0xFFFFFFFF, (int)(180 * hitFlashProgress)));
        }

        float baseSize = (float) size * hitScale;
        float baseGap = (float) gap;
        float thick = (float) thickness;
        float totalSpread = baseGap + hitSpread + moveSpreadAnim + targetProgress * 1.5f;

        float totalSpin = hitSpin + targetProgress * continuousRotation;

        switch (mode) {
            case "Normal" -> renderNormal(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
            case "Circle" -> renderCircle(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
            case "Triangle" -> renderTriangle(graphics, cx, cy, baseSize, totalSpread, thick, totalSpin, currentColor);
        }

        if (dot) {
            int dc = dotColor;
            float dotSize = 2.5f;
            net.minecraft.resources.Identifier dotTex = ravex.utility.render.Render2DUtility.getSmoothCircle();
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx - dotSize / 2f, cy - dotSize / 2f);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, dotTex, 0, 0, 0f, 0f, (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), (int) Math.ceil(dotSize), dc);
            graphics.pose().popMatrix();
        }
    }

    private float calcMoveSpread(MinecraftWrapper mc) {
        float moveEff = (float) moveEffect;
        if (moveEff <= 0) return 0;
        var player = mc.getPlayer();
        double velX = player.getX() - player.xo;
        double velZ = player.getZ() - player.zo;
        double hSpeed = Math.sqrt(velX * velX + velZ * velZ) * 20.0;
        float spread = 0;
        if (PlayerUtility.isSprinting(player)) spread += moveEff * 0.8f;
        else if (hSpeed > 0.05) spread += moveEff * 0.4f;
        if (!player.onGround()) spread += moveEff * 0.6f;
        return Math.min(spread, moveEff * 2);
    }

    private void renderNormal(GuiGraphics g, int cx, int cy, float size, float gap, float thick, float spin, int color) {
        float end = gap + size;
        float len = end - gap;
        if (len <= 0f || thick <= 0f) return;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (spin != 0) {
            g.pose().rotate(spin);
        }
        net.minecraft.resources.Identifier barTex = Render2DUtility.getSmoothBar();
        int w = Math.max(1, Math.round(len));
        int h = Math.max(1, Math.round(thick));
        float mid = (gap + end) * 0.5f;
        blitBar(g, barTex, -mid, 0f, w, h, color);
        blitBar(g, barTex, mid, 0f, w, h, color);
        blitBar(g, barTex, 0f, -mid, h, w, color);
        blitBar(g, barTex, 0f, mid, h, w, color);
        g.pose().popMatrix();
    }

    private void blitBar(GuiGraphics g, net.minecraft.resources.Identifier tex, float x, float y, int w, int h, int color) {
        int px = Math.round(x - w * 0.5f);
        int py = Math.round(y - h * 0.5f);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, px, py, 0f, 0f, w, h, w, h, color);
    }

    private void renderCircle(GuiGraphics g, int cx, int cy, float size, float gap, float thick, float spin, int color) {
        float radius = gap + size;
        float thicknessRatio = thick / radius;
        net.minecraft.resources.Identifier ringTex = ravex.utility.render.Render2DUtility.getSmoothRing(thicknessRatio);

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
        if (radius <= 0f || thick <= 0f) return;
        float thicknessRatio = thick / radius;
        net.minecraft.resources.Identifier triTex = Render2DUtility.getSmoothTriangle(thicknessRatio);
        int d = Math.max(2, Math.round(radius * 2f));

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (spin != 0) {
            g.pose().rotate(spin);
        }
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, triTex, -d / 2, -d / 2, 0f, 0f, d, d, d, d, color);
        g.pose().popMatrix();
    }
}
