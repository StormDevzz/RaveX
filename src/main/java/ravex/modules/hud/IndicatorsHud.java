package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
<<<<<<< HEAD
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
public class IndicatorsHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_INDICATORS_WHITE;
    private static final int IS = HudRenderer.getIconSize();
=======
public class IndicatorsHud extends Module {
    public static final IndicatorsHud INSTANCE = new IndicatorsHud();
    private static final int RADIUS     = 16;
    private static final int THICKNESS  = 4;
    private static final int SPACING    = 48;
    private static final int COLS       = 5;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private long lastRealTime  = 0;
    private long lastGameTick  = -1;
    private float smoothedTPS  = 20.0f;
    private float prevVelX = 0, prevVelZ = 0;
    private float smoothKB = 0;
<<<<<<< HEAD
    private float animHealth = 1f, animArmor = 1f, animTPS = 1f, animSpeed = 0f, animKB = 0f;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private IndicatorsHud() {
        super("Indicators", 10, 340, 10, 10);
        addParameter(new ColorParameter("HealthColor", 0xFFFF4455));
        addParameter(new ColorParameter("ArmorColor", 0xFF44AAFF));
        addParameter(new ColorParameter("TPSColor", 0xFF44FF88));
        addParameter(new ColorParameter("SpeedColor", 0xFFFFCC33));
        addParameter(new ColorParameter("KBColor", 0xFFCC44FF));
        addParameter(new BooleanParameter("Shadow", true));
    }
    private int getGaugeColor(int index) {
        String[] names = {"HealthColor", "ArmorColor", "TPSColor", "SpeedColor", "KBColor"};
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals(names[index])) return cp.getValue();
        }
        int[] def = {0xFFFF4455, 0xFF44AAFF, 0xFF44FF88, 0xFFFFCC33, 0xFFCC44FF};
        return def[index];
    }
    private boolean getShadow() {
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("Shadow")) return bp.getValue();
        }
        return true;
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Player player = mc.player;
        updateTPS(mc);
        updateKnockback(player);
<<<<<<< HEAD
        boolean shadow = getShadow();
        float health = player.getHealth() / player.getMaxHealth();
        float armor  = Math.min(1, player.getArmorValue() / 20.0f);
        float tps    = smoothedTPS / 20.0f;
        float speed  = (float) Math.min(1, Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                                                       player.getDeltaMovement().z * player.getDeltaMovement().z) / 0.3);
        float kb     = Math.max(0, Math.min(1, smoothKB / 0.4f));
        float smooth = 0.3f;
        animHealth += (health - animHealth) * smooth;
        animArmor  += (armor - animArmor) * smooth;
        animTPS    += (tps - animTPS) * smooth;
        animSpeed  += (speed - animSpeed) * smooth;
        animKB     += (kb - animKB) * smooth;
        float[] values = { animHealth, animArmor, animTPS, animSpeed, animKB };
        String[][] data = {
            {"Health", (int)(player.getHealth()) + "/" + (int)(player.getMaxHealth())},
            {"Armor",  String.valueOf(player.getArmorValue())},
            {"TPS",    String.format("%.1f", smoothedTPS)},
            {"Speed",  String.format("%.0f", speed * 0.3 * 20)},
            {"KB",     player.hurtTime > 0 ? String.format("%.2f", smoothKB) : "0.00"}
        };
        int lineH = 11;
        int dotR = 3;
        int barW = 50;
        int labelMax = 0, valueMax = 0;
        for (int i = 0; i < 5; i++) {
            int lw = FontRenderUtility.getStringWidth(data[i][0]);
            int vw = FontRenderUtility.getStringWidth(data[i][1]);
            if (lw > labelMax) labelMax = lw;
            if (vw > valueMax) valueMax = vw;
        }
        int pw = 4 + 9 + labelMax + 4 + valueMax + 4 + barW + 4 + IS + 4;
        int ph = 6 + 5 * lineH;
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
        int cx = bx + 4;
        int cy = by + 4;
        for (int i = 0; i < 5; i++) {
            int col = getGaugeColor(i);
            int dotX = cx + dotR;
            int dotY = cy + lineH / 2;
            Render2DEngine.fillCircle(graphics, dotX, dotY, dotR, col);
            FontRenderUtility.drawString(graphics, data[i][0], cx + dotR * 2 + 3, cy, 0xFF8080A0, false);
            int vx = cx + dotR * 2 + 3 + labelMax + 4;
            FontRenderUtility.drawString(graphics, data[i][1], vx, cy, col, shadow);
            int barX = vx + valueMax + 4;
            int barY = cy + 3;
            int barH = lineH - 6;
            int availW = (bx + pw - 4 - IS - 4) - barX;
            if (availW < 0) availW = 0;
            int fillW = Math.min(barW, availW);
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0x22FFFFFF);
            int fillPct = (int) (values[i] * fillW);
            if (fillPct > 0) graphics.fill(barX, barY, barX + fillPct, barY + barH, col);
            cy += lineH;
        }
    }
=======
        float health  = Math.max(0, Math.min(1, player.getHealth() / player.getMaxHealth()));
        float armor   = Math.max(0, Math.min(1, player.getArmorValue() / 20.0f));
        float tps     = Math.max(0, Math.min(1, smoothedTPS / 20.0f));
        float speed   = (float) Math.min(1, Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                                                        player.getDeltaMovement().z * player.getDeltaMovement().z) / 0.3);
        float kb      = Math.max(0, Math.min(1, smoothKB / 0.4f));
        boolean shadow = getShadow();
        int pw = COLS * SPACING + 5;
        int ph = RADIUS * 2 + 28;
        setWidth(pw);
        setHeight(ph);
        int bx = getX();
        int by = getY();
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        String[] labels = { "Health", "Armor", "TPS", "Speed", "Knockback" };
        float[] values  = { health,   armor,   tps,   speed,   kb };
        String[] valueTexts = {
            (int)(player.getHealth()) + "\u00A77/" + (int)(player.getMaxHealth()),
            String.valueOf(player.getArmorValue()),
            String.format("%.1f", smoothedTPS),
            String.format("%.0f", speed * 0.3 * 20),
            player.hurtTime > 0 ? String.format("%.2f", smoothKB) : "0.00"
        };
        for (int i = 0; i < COLS; i++) {
            int cx = bx + 3 + RADIUS + i * SPACING;
            int cy = by + RADIUS + 8;
            int col = getGaugeColor(i);
            drawGauge(graphics, cx, cy, RADIUS, THICKNESS, values[i], col);
            String valStr = valueTexts[i];
            int tw = ravex.utility.render.FontRenderUtility.getStringWidth(valStr);
            ravex.utility.render.FontRenderUtility.drawString(graphics, valStr, cx - tw / 2, cy - 5, col, shadow);
            int lw = ravex.utility.render.FontRenderUtility.getStringWidth(labels[i]);
            ravex.utility.render.FontRenderUtility.drawString(graphics, labels[i], cx - lw / 2, by + ph - 11, 0xFF8080A0, false);
        }
    }
    private void drawGauge(GuiGraphics g, int cx, int cy, int r, int thick, float progress, int color) {
        int bg = 0x22FFFFFF;
        double step = Math.PI / 48;
        double total = progress * Math.PI * 2;
        for (double a = 0; a < Math.PI * 2; a += step) {
            boolean filled = a >= -Math.PI / 2 && a < -Math.PI / 2 + total;
            int c = filled ? color : bg;
            for (int t = 0; t < thick; t++) {
                int rr = r - thick / 2 + t;
                int px = (int)(cx + rr * Math.cos(a));
                int py = (int)(cy + rr * Math.sin(a));
                g.fill(px, py, px + 1, py + 1, c);
            }
        }
        if (progress > 0.01f) {
            double endA = -Math.PI / 2 + total;
            int dotR = 2;
            for (int dy = -dotR; dy <= dotR; dy++) {
                for (int dx = -dotR; dx <= dotR; dx++) {
                    if (dx * dx + dy * dy <= dotR * dotR) {
                        int px = (int)(cx + (r - thick / 2.0 + 0.5) * Math.cos(endA)) + dx;
                        int py = (int)(cy + (r - thick / 2.0 + 0.5) * Math.sin(endA)) + dy;
                        g.fill(px, py, px + 1, py + 1, color);
                    }
                }
            }
        }
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private void updateTPS(Minecraft mc) {
        long now = System.currentTimeMillis();
        long gameTick = mc.level.getGameTime();
        if (lastGameTick < 0) { lastGameTick = gameTick; lastRealTime = now; return; }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick; lastRealTime = now;
        }
    }
    private void updateKnockback(Player player) {
        float vx = (float) player.getDeltaMovement().x;
        float vz = (float) player.getDeltaMovement().z;
        float deltaV = (float) Math.sqrt((vx - prevVelX) * (vx - prevVelX) + (vz - prevVelZ) * (vz - prevVelZ));
        if (player.hurtTime > 0 && deltaV > 0.05f) {
            smoothKB = smoothKB * 0.4f + deltaV * 0.6f;
        } else {
            smoothKB = smoothKB * 0.85f;
        }
        prevVelX = vx; prevVelZ = vz;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(IndicatorsHud.class);
    }

    public static IndicatorsHud itz() {
        return ModuleManager.get(IndicatorsHud.class);
    }
}
