package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.player.PlayerUtility;
import ravex.utility.render.ColorUtility;
import ravex.modules.client.Hud;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.gui.hudeditor.HudEditorScreen;

@HudModule("IndicatorsHud")
public class IndicatorsHud extends ravex.modules.Module {
    @Parameter(name = "HealthColor", color = true)
    public int healthColor = 0xFFFF4455;
    @Parameter(name = "ArmorColor", color = true)
    public int armorColor = 0xFF44AAFF;
    @Parameter(name = "TPSColor", color = true)
    public int tPSColor = 0xFF44FF88;
    @Parameter(name = "SpeedColor", color = true)
    public int speedColor = 0xFFFFCC33;
    @Parameter(name = "KBColor", color = true)
    public int kBColor = 0xFFCC44FF;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

    private static final Identifier ICON = TextureLoaderUtility.HUD_INDICATORS_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private long lastRealTime  = 0;
    private long lastGameTick  = -1;
    private float smoothedTPS  = 20.0f;
    private float prevVelX = 0, prevVelZ = 0;
    private float smoothKB = 0;
    private float animHealth = 1f, animArmor = 1f, animTPS = 1f, animSpeed = 0f, animKB = 0f;

    public IndicatorsHud() {
        super("IndicatorsHud", 10, 100, 160, 61);
        setX(10);
        setY(100);
        setWidth(160);
        setHeight(61);
    }

    private int getGaugeColor(int index) {
        int[] colors = {this.healthColor, this.armorColor, this.tPSColor, this.speedColor, this.kBColor};
        return colors[index];
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        boolean inEditor = mc.getScreen() instanceof HudEditorScreen;
        if ((mc.getPlayer() == null || mc.getLevel() == null) && !inEditor) return;

        var player = mc.getPlayer();
        if (mc.getLevel() != null) {
            updateTPS(mc);
        }
        if (player != null) {
            updateKnockback(player);
        }

        boolean shadow = this.shadow;
        float health = player != null ? PlayerUtility.getHealth(player) / PlayerUtility.getMaxHealth(player) : 1f;
        float armor  = player != null ? Math.min(1, PlayerUtility.getArmorValue(player) / 20.0f) : 1f;
        float tps    = smoothedTPS / 20.0f;
        float speed  = player != null ? (float) Math.min(1, Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                                                       player.getDeltaMovement().z * player.getDeltaMovement().z) / 0.3) : 0f;
        float kb     = Math.max(0, Math.min(1, smoothKB / 0.4f));
        float smooth = 0.3f;
        animHealth += (health - animHealth) * smooth;
        animArmor  += (armor - animArmor) * smooth;
        animTPS    += (tps - animTPS) * smooth;
        animSpeed  += (speed - animSpeed) * smooth;
        animKB     += (kb - animKB) * smooth;
        float[] values = { animHealth, animArmor, animTPS, animSpeed, animKB };

        String[][] data;
        if (player != null) {
            data = new String[][]{
                {"Health", (int)(PlayerUtility.getHealth(player)) + "/" + (int)(PlayerUtility.getMaxHealth(player))},
                {"Armor",  String.valueOf(PlayerUtility.getArmorValue(player))},
                {"TPS",    String.format("%.1f", smoothedTPS)},
                {"Speed",  String.format("%.0f", speed * 0.3 * 20)},
                {"KB",     player.hurtTime > 0 ? String.format("%.2f", smoothKB) : "0.00"}
            };
        } else {
            data = new String[][]{
                {"Health", "20/20"},
                {"Armor",  "20"},
                {"TPS",    "20.0"},
                {"Speed",  "0"},
                {"KB",     "0.00"}
            };
        }

        int lineH = 11;
        int padX = 6;
        int padTop = 5;
        int gap = 8;
        int[] colW = new int[5];
        int pw = padX * 2;
        for (int i = 0; i < 5; i++) {
            int wv = FontRenderUtility.getStringWidth(data[i][1]);
            int wl = FontRenderUtility.getStringWidth(data[i][0]);
            colW[i] = Math.max(wv, wl);
            pw += colW[i] + (i < 4 ? gap : 0);
        }
        int barY = padTop + 10;
        int labelY = barY + 6;
        int ph = labelY + 8 + padTop;
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cx = bx + padX;
        for (int i = 0; i < 5; i++) {
            int col = getGaugeColor(i);
            String value = data[i][1];
            int vw = FontRenderUtility.getStringWidth(value);
            FontRenderUtility.drawString(graphics, value, cx + (colW[i] - vw) / 2, by + padTop, 0xFFF2F2F7, shadow);
            Render2DUtility.drawRound(graphics, cx, by + barY, colW[i], 3, 1, 0x22FFFFFF);
            int fill = (int) (values[i] * colW[i]);
            if (fill > 0) Render2DUtility.drawRound(graphics, cx, by + barY, fill, 3, 1, col);
            int lw = FontRenderUtility.getStringWidth(data[i][0]);
            FontRenderUtility.drawString(graphics, data[i][0], cx + (colW[i] - lw) / 2, by + labelY, 0xFF6E6E85, false);
            cx += colW[i] + gap;
        }
    }

    private void updateTPS(MinecraftWrapper mc) {
        long now = System.currentTimeMillis();
        long gameTick = mc.getLevel().getGameTime();
        if (lastGameTick < 0) { lastGameTick = gameTick; lastRealTime = now; return; }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick; lastRealTime = now;
        }
    }

    private void updateKnockback(net.minecraft.world.entity.player.Player player) {
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
}
