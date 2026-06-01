package ravex.modules.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.HudModule;
import ravex.modules.render.Hud;

/**
 * IndicatorsHud – five circular progress gauges:
 * Health · Armor · TPS · Speed · Knockback
 */
public class IndicatorsHud extends HudModule {
    public static final IndicatorsHud INSTANCE = new IndicatorsHud();

    private static final int RADIUS     = 18;   // circle radius in pixels
    private static final int THICKNESS  = 3;    // arc thickness
    private static final int SPACING    = 50;   // center-to-center spacing
    private static final int COLS       = 5;

    // Server TPS tracking
    private long lastRealTime  = 0;
    private long lastGameTick  = -1;
    private float smoothedTPS  = 20.0f;

    // Knockback smoothing
    private float prevVelX = 0, prevVelZ = 0;
    private float smoothKB = 0;

    private IndicatorsHud() {
        super("Indicators", 10, 340, COLS * SPACING + 10, RADIUS * 2 + 30);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Player player = mc.player;

        updateTPS(mc);
        updateKnockback(player);

        float health  = Math.max(0, Math.min(1, player.getHealth() / player.getMaxHealth()));
        float armor   = Math.max(0, Math.min(1, player.getArmorValue() / 20.0f));
        float tps     = Math.max(0, Math.min(1, smoothedTPS / 20.0f));
        float speed   = (float) Math.min(1, Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                                                       player.getDeltaMovement().z * player.getDeltaMovement().z) / 0.3);
        float kb      = Math.max(0, Math.min(1, smoothKB / 0.4f));

        int bx = getX();
        int by = getY();
        int w  = getWidth();
        int h  = getHeight();

        // Background
        graphics.fill(bx, by, bx + w, by + h, 0xBB060610);
        graphics.fill(bx, by, bx + w, by + 1, ColorUtility.withAlpha(ColorUtility.getActiveColor(), 80));

        // Draw 5 gauges
        String[] labels = { "Health", "Armor", "TPS", "Speed", "Knockback" };
        float[] values  = { health,   armor,   tps,   speed,   kb };
        int[]   colors  = {
            0xFFFF4455,  // Health: red
            0xFF44AAFF,  // Armor: blue
            0xFF44FF88,  // TPS: green
            0xFFFFCC33,  // Speed: gold
            0xFFCC44FF   // Knockback: purple
        };

        // Text values to show
        String[] valueTexts = {
            (int)(player.getHealth()) + "§7/" + (int)(player.getMaxHealth()),
            String.valueOf(player.getArmorValue()),
            String.format("%.1f", smoothedTPS),
            String.format("%.1f", speed * 0.3 * 20 /* bps */),
            player.hurtTime > 0 ? String.format("%.2f", smoothKB) : "0.00"
        };

        for (int i = 0; i < COLS; i++) {
            int cx = bx + 5 + RADIUS + i * SPACING;
            int cy = by + RADIUS + 8;
            drawGauge(graphics, cx, cy, RADIUS, THICKNESS, values[i], colors[i]);

            // Center text
            String valStr = valueTexts[i];
            int tw = ravex.utility.render.FontRenderUtility.getStringWidth(valStr);
            ravex.utility.render.FontRenderUtility.drawString(graphics, valStr, cx - tw / 2, cy - 4, colors[i], false);

            // Label below
            int lw = ravex.utility.render.FontRenderUtility.getStringWidth(labels[i]);
            ravex.utility.render.FontRenderUtility.drawString(graphics, labels[i], cx - lw / 2, by + h - 10, 0xFF8080A0, false);
        }
    }

    /**
     * Draws a circular progress arc with a dark background ring.
     * Starts at 12 o'clock (top), goes clockwise.
     */
    private void drawGauge(GuiGraphics g, int cx, int cy, int r, int thick, float progress, int color) {
        double step = 0.06; // radians per segment — smaller = smoother

        // Background ring (full circle, dark)
        for (double a = 0; a < Math.PI * 2; a += step) {
            fillArcSegment(g, cx, cy, r, thick, a, 0x33FFFFFF);
        }

        // Progress arc (clockwise from top)
        double total = progress * Math.PI * 2;
        for (double a = -Math.PI / 2; a < -Math.PI / 2 + total; a += step) {
            fillArcSegment(g, cx, cy, r, thick, a, color);
        }

        // Endpoint dot (bright cap)
        if (progress > 0.01f) {
            double endA = -Math.PI / 2 + total;
            int ex = (int)(cx + r * Math.cos(endA));
            int ey = (int)(cy + r * Math.sin(endA));
            g.fill(ex - 1, ey - 1, ex + 2, ey + 2, color | 0xFF000000);
        }
    }

    private void fillArcSegment(GuiGraphics g, int cx, int cy, int r, int thick, double angle, int color) {
        // Fill from r-thick+1 to r+1 pixels
        for (int dr = 0; dr <= thick; dr++) {
            int rr = r - thick / 2 + dr;
            int px = (int)(cx + rr * Math.cos(angle));
            int py = (int)(cy + rr * Math.sin(angle));
            // Single pixel fill, slightly bigger for outer radius
            int size = dr == thick / 2 ? 2 : 1;
            g.fill(px, py, px + size, py + size, color);
        }
    }

    private void updateTPS(Minecraft mc) {
        long now = System.currentTimeMillis();
        long gameTick = mc.level.getGameTime();
        if (lastGameTick < 0) {
            lastGameTick = gameTick;
            lastRealTime = now;
            return;
        }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick;
            lastRealTime = now;
        }
    }

    private void updateKnockback(Player player) {
        float vx = (float) player.getDeltaMovement().x;
        float vz = (float) player.getDeltaMovement().z;
        float deltaV = (float) Math.sqrt((vx - prevVelX) * (vx - prevVelX) + (vz - prevVelZ) * (vz - prevVelZ));
        if (player.hurtTime > 0 && deltaV > 0.05f) {
            smoothKB = smoothKB * 0.4f + deltaV * 0.6f;
        } else {
            smoothKB = smoothKB * 0.85f; // decay
        }
        prevVelX = vx;
        prevVelZ = vz;
    }
}
