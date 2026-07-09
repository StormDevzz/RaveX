package ravex.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class Crosshair extends Module {
    public static final Crosshair INSTANCE = new Crosshair();

    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
        java.util.List.of("Normal", "Circle", "Triangle"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final ColorParameter dotColor = new ColorParameter("Dot Color", 0xFFFF3333);
    public final NumberParameter size = new NumberParameter("Size", 4.0, 2.0, 10.0, 0.5);
    public final NumberParameter gap = new NumberParameter("Gap", 3.0, 1.0, 10.0, 0.5);
    public final NumberParameter thickness = new NumberParameter("Thickness", 1.5, 1.0, 4.0, 0.5);
    public final BooleanParameter dot = new BooleanParameter("Dot", true);
    public final BooleanParameter dynamic = new BooleanParameter("Dynamic", true);
    public final NumberParameter hitEffect = new NumberParameter("Hit Effect", 6.0, 0.0, 16.0, 0.5);
    public final NumberParameter hitDuration = new NumberParameter("Hit Duration", 250.0, 50.0, 500.0, 25.0);
    public final NumberParameter moveEffect = new NumberParameter("Move Effect", 3.0, 0.0, 10.0, 0.5);

    private long lastHitTime = 0;
    private float hitSpread = 0;

    private Crosshair() {
        super("Crosshair", Category.RENDER);
        addParameter(mode);
        addParameter(color);
        addParameter(dotColor);
        addParameter(size);
        addParameter(gap);
        addParameter(thickness);
        addParameter(dot);
        addParameter(dynamic);
        addParameter(hitEffect);
        addParameter(hitDuration);
        addParameter(moveEffect);
        dotColor.setVisible(dot::getValue);
    }

    public void onHit() {
        lastHitTime = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!dynamic.getValue()) {
            hitSpread = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTime;
        float dur = hitDuration.getValue().floatValue();

        if (elapsed < dur) {
            float progress = elapsed / dur;
            float overshoot = 1.0f + 0.3f * (float) Math.sin(progress * Math.PI * 2) * (1.0f - progress);
            hitSpread = hitEffect.getValue().floatValue() * (1.0f - progress) * overshoot;
        } else {
            hitSpread = 0;
        }
    }

    public void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null || mc.player == null) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        int col = color.getValue();
        int r = (col >> 16) & 0xFF;
        int g = (col >> 8) & 0xFF;
        int b = col & 0xFF;
        int a = (col >> 24) & 0xFF;
        if (a == 0) a = 255;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        float baseSize = size.getValue().floatValue();
        float baseGap = gap.getValue().floatValue();
        float thick = thickness.getValue().floatValue();
        float moveSpread = dynamic.getValue() ? calcMoveSpread(mc) : 0;
        float totalSpread = baseGap + hitSpread + moveSpread;

        switch (mode.getValue()) {
            case "Normal" -> renderNormal(graphics, cx, cy, baseSize, totalSpread, thick, argb);
            case "Circle" -> renderCircle(graphics, cx, cy, baseSize, totalSpread, thick, argb);

            case "Triangle" -> renderTriangle(graphics, cx, cy, baseSize, totalSpread, thick, argb);
        }

        if (dot.getValue()) {
            int dc = dotColor.getValue();
            int dr = (dc >> 16) & 0xFF;
            int dg = (dc >> 8) & 0xFF;
            int db = dc & 0xFF;
            int da = (dc >> 24) & 0xFF;
            if (da == 0) da = 255;
            graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, (da << 24) | (dr << 16) | (dg << 8) | db);
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

    private void renderNormal(GuiGraphics g, int cx, int cy, float size, float gap, float thick, int color) {
        float end = gap + size;
        float half = thick / 2.0f;
        int ih = Math.max(1, (int) Math.ceil(half));
        g.fill((int) (cx - end), cy - ih, (int) (cx - gap), cy + ih, color);
        g.fill((int) (cx + gap), cy - ih, (int) (cx + end), cy + ih, color);
        g.fill(cx - ih, (int) (cy - end), cx + ih, (int) (cy - gap), color);
        g.fill(cx - ih, (int) (cy + gap), cx + ih, (int) (cy + end), color);
    }

    private void renderCircle(GuiGraphics g, int cx, int cy, float size, float gap, float thick, int color) {
        float radius = gap + size;
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float y1 = cy + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius);
            float y2 = cy + (float) (Math.sin(a2) * radius);
            drawThickLine(g, x1, y1, x2, y2, thick, color);
        }
    }

    private void renderTriangle(GuiGraphics g, int cx, int cy, float size, float gap, float thick, int color) {
        float end = gap + size;
        float tipY = cy - end;
        float botY = cy + end * 0.6f;
        float leftX = cx - end * 0.8f;
        float rightX = cx + end * 0.8f;

        drawThickLine(g, cx, tipY, leftX, botY, thick, color);
        drawThickLine(g, leftX, botY, rightX, botY, thick, color);
        drawThickLine(g, rightX, botY, cx, tipY, thick, color);
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
        g.fill(0, 0, (int) len, 1, color);
        g.pose().popMatrix();
    }
}
