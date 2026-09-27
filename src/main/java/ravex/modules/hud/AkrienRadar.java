package ravex.modules.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import ravex.manager.FriendManager;
import ravex.modules.Module;
import ravex.modules.Modules;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Hud;
import ravex.utility.player.PlayerUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

@HudModule("AkrienRadar")
public class AkrienRadar extends Module {

    @Parameter(name = "TracerHeight", min = 0.1, max = 5.0, step = 0.1)
    public double tracerHeight = 2.28;

    @Parameter(name = "TracerRadius", min = 20, max = 100, step = 1)
    public int tracerRadius = 68;

    @Parameter(name = "PitchLock", min = 0, max = 90, step = 1)
    public int pitchLock = 42;

    @Parameter(name = "Glow")
    public boolean glow = false;

    @Parameter(name = "GlowRadius", min = 1, max = 20, step = 1)
    public int glowRadius = 10;

    @Parameter(name = "GlowAlpha", min = 0, max = 255, step = 1)
    public int glowAlpha = 170;

    @Parameter(name = "TracerColorMode", modes = {"Custom", "Astolfo", "Rainbow", "Client"})
    public String tracerColorMode = "Astolfo";

    @Parameter(name = "CircleColorMode", modes = {"Custom", "Astolfo", "Rainbow", "Client"})
    public String circleColorMode = "Astolfo";

    @Parameter(name = "CompassRadius", min = 0.1, max = 70.0, step = 0.1)
    public double compassRadius = 47.0;

    @Parameter(name = "CompassColor", color = true)
    public int compassColor = 0xFFFFFFFF;

    @Parameter(name = "CircleColor", color = true)
    public int circleColor = 0xFFFFFFFF;

    @Parameter(name = "FriendColor", color = true)
    public int friendColor = 0xFF00E800;

    @Parameter(name = "TracerColor", color = true)
    public int tracerColor = 0xFFFFFF00;

    @Parameter(name = "ArrowSpeed", min = 1, max = 20, step = 1)
    public double arrowSpeed = 8;

    private final java.util.Map<java.util.UUID, Float> arrowYawSmooth = new java.util.HashMap<>();

    private AkrienRadar() {
        super("AkrienRadar", 50, 50, (int) (47.0 * 2), (int) (47.0 * 2));
    }

    private static float getRotations(Player target, float pt) {
        Player player = PlayerUtility.getPlayer();
        if (player == null) return 0;
        double x = lerp(target.getX(), target.xo, pt) - lerp(player.getX(), player.xo, pt);
        double z = lerp(target.getZ(), target.zo, pt) - lerp(player.getZ(), player.zo, pt);
        return (float) -(Math.atan2(x, z) * (180.0 / Math.PI));
    }

    private static double lerp(double current, double prev, float pt) {
        return prev + (current - prev) * pt;
    }

    private static float clampf(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private int resolveTracerColor(boolean isFriend, float angleDeg) {
        if (isFriend) return friendColor;
        return switch (tracerColorMode) {
            case "Astolfo" -> ColorUtility.astolfo(3000, (int) (angleDeg * 4), 0.7f, 1.0f, 1.0f);
            case "Rainbow" -> ColorUtility.rainbow(3000, (int) (angleDeg * 4), 1.0f, 1.0f, 1.0f);
            case "Client" -> ColorUtility.getActiveColor();
            default -> tracerColor;
        };
    }

    private int resolveCircleColor() {
        return switch (circleColorMode) {
            case "Astolfo" -> ColorUtility.astolfo(3000, 0, 0.7f, 1.0f, 1.0f);
            case "Rainbow" -> ColorUtility.rainbow(3000, 0, 1.0f, 1.0f, 1.0f);
            case "Client" -> ColorUtility.getActiveColor();
            default -> circleColor;
        };
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        Player player = PlayerUtility.getPlayer();
        if (player == null || player.level() == null) return;

        float cx = getX() + (float) compassRadius;
        float cy = getY() + (float) compassRadius;

        float yaw = -player.getYRot();
        float pitch = clampf(player.getXRot(), pitchLock, 90);
        float pitchFactor = Math.abs(90f / pitch);
        float radius = (float) compassRadius;

        var pose = graphics.pose();

        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(1.0f, 1.0f / pitchFactor);
        pose.translate(-cx, -cy);

        int circleCol = resolveCircleColor();
        drawEllipseArc(graphics, cx, cy, radius + 2, yaw, 15, 60, ColorUtility.withAlpha(circleCol, 170), 1);
        drawEllipseArc(graphics, cx, cy, radius + 2, yaw, 105, 60, ColorUtility.withAlpha(circleCol, 170), 1);
        drawEllipseArc(graphics, cx, cy, radius + 2, yaw, 195, 60, ColorUtility.withAlpha(circleCol, 170), 1);
        drawEllipseArc(graphics, cx, cy, radius + 2, yaw, 285, 60, ColorUtility.withAlpha(circleCol, 170), 1);

        drawEllipseArc(graphics, cx, cy, radius, yaw, 15, 60, compassColor, 3);
        drawEllipseArc(graphics, cx, cy, radius, yaw, 105, 60, compassColor, 3);
        drawEllipseArc(graphics, cx, cy, radius, yaw, 195, 60, compassColor, 3);
        drawEllipseArc(graphics, cx, cy, radius, yaw, 285, 60, compassColor, 3);

        pose.popMatrix();

        drawDirectionLabel(graphics, cx, cy, radius, pitchFactor, yaw + 15, "W");
        drawDirectionLabel(graphics, cx, cy, radius, pitchFactor, yaw + 105, "N");
        drawDirectionLabel(graphics, cx, cy, radius, pitchFactor, yaw + 195, "E");
        drawDirectionLabel(graphics, cx, cy, radius, pitchFactor, yaw + 285, "S");

        float arrowSize = (float) (tracerHeight * 6.0);
        float r = (float) tracerRadius;

        record ArrowData(float posX, float posY, float angle, float size, int color) {}
        java.util.List<ArrowData> arrows = new java.util.ArrayList<>();

        for (Player target : player.level().players()) {
            if (target == player) continue;

            float tyaw = getRotations(target, partialTicks) - player.getYRot();

            java.util.UUID uid = target.getUUID();
            Float prev = arrowYawSmooth.get(uid);
            float smoothed;
            if (prev == null) {
                smoothed = tyaw;
            } else {
                float diff = tyaw - prev;
                while (diff > 180) diff -= 360;
                while (diff < -180) diff += 360;
                smoothed = prev + diff * (float) (arrowSpeed / 100.0);
            }
            arrowYawSmooth.put(uid, smoothed);

            float rad = (float) Math.toRadians(smoothed);
            float sin = (float) Math.sin(rad);
            float cos = (float) Math.cos(rad);
            float posX = cx + sin * r;
            float posY = cy - cos * (r / pitchFactor);
            float arrowAngle = (float) Math.atan2(posY - cy, posX - cx) + (float) (Math.PI / 2.0);

            int color = resolveTracerColor(FriendManager.INSTANCE.isFriend(target.getName().getString()), smoothed);
            arrows.add(new ArrowData(posX, posY, arrowAngle, arrowSize, color));
        }

        for (ArrowData a : arrows) {
            if (glow) {
                float hs = a.size() / 2f;
                Render2DUtility.drawBlurredShadow(graphics, a.posX() - hs, a.posY() - hs, a.size(), a.size(), glowRadius,
                        ColorUtility.applyAlpha(a.color(), glowAlpha / 255f));
            }
            Render2DUtility.drawSharpArrow(graphics, a.posX(), a.posY(), a.angle(), a.size(), a.color());
        }

        setWidth((int) (compassRadius * 2));
        setHeight((int) (compassRadius * 2));
    }

    private void drawEllipseArc(GuiGraphics graphics, float cx, float cy, float radius, float yaw, float startDeg, float arcLen, int color, float thickness) {
        Render2DUtility.drawSmoothArc(graphics, cx, cy, radius, thickness, yaw + startDeg, arcLen, color);
    }

    private void drawDirectionLabel(GuiGraphics graphics, float cx, float cy, float radius, float pitchFactor, float angleDeg, String label) {
        float rad = (float) Math.toRadians(angleDeg - 15);
        int lx = (int) (cx + (float) Math.cos(rad) * radius) - 2;
        int ly = (int) (cy + (float) Math.sin(rad) * (radius / pitchFactor)) - 1;
        FontRenderUtility.drawString(graphics, label, lx, ly, -1, true);
    }
}
