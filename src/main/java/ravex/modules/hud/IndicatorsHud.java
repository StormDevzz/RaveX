package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.TextureLoader;

@ModuleInfo(name = "IndicatorsHud", category = "HUD")
public class IndicatorsHud extends ravex.modules.Module {
    public final ColorParameter healthColor = new ColorParameter("HealthColor", 0xFFFF4455);
    public final ColorParameter armorColor = new ColorParameter("ArmorColor", 0xFF44AAFF);
    public final ColorParameter tPSColor = new ColorParameter("TPSColor", 0xFF44FF88);
    public final ColorParameter speedColor = new ColorParameter("SpeedColor", 0xFFFFCC33);
    public final ColorParameter kBColor = new ColorParameter("KBColor", 0xFFCC44FF);
    public final BooleanParameter shadow = new BooleanParameter("Shadow", true);

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoader.HUD_INDICATORS_WHITE;
    private static final int IS = HudRenderer.getIconSize();
    private long lastRealTime  = 0;
    private long lastGameTick  = -1;
    private float smoothedTPS  = 20.0f;
    private float prevVelX = 0, prevVelZ = 0;
    private float smoothKB = 0;
    private float animHealth = 1f, animArmor = 1f, animTPS = 1f, animSpeed = 0f, animKB = 0f;

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
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Player player = mc.player;
        updateTPS(mc);
        updateKnockback(player);
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
        width = pw;
        height = ph;
        int bx = x, by = y;
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("IndicatorsHud").getEnabled();
    }

    public static IndicatorsHud itz() {
        return ravex.manager.ModuleManager.delegate(IndicatorsHud.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
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