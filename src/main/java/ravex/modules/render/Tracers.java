package ravex.modules.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ravex.RaveX;
import ravex.cmd.cmds.GpsCmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

@Module(name = "Tracers", category = "Render")
public class Tracers {
    @Parameter(name = "Mode", modes = {"Default", "ArrowOld", "ArrowNew"})
    public String mode = "Default";
    @Parameter(name = "ColorMode", modes = {"Custom", "Rainbow", "Astolfo", "Client"}, visible = "mode=ArrowNew")
    public String colorMode = "Custom";
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Monsters")
    public boolean monsters = false;
    @Parameter(name = "Animals")
    public boolean animals = false;
    @Parameter(name = "Items")
    public boolean items = false;
    @Parameter(name = "Distance", min = 10.0, max = 300.0, step = 10.0)
    public double maxDistance = 100.0;
    @Parameter(name = "ArrowSize", min = 6.0, max = 36.0, step = 1.0)
    public double arrowSize = 14.0;
    @Parameter(name = "ArrowMargin", min = 0.0, max = 30.0, step = 1.0)
    public double arrowMargin = 4.0;
    @Parameter(name = "PlayerColor", color = true, visible = "players")
    public int playerColor = 0xFFFF3333;
    @Parameter(name = "MobColor", color = true, visible = "monsters")
    public int mobColor = 0xFFFF3333;
    @Parameter(name = "AnimalColor", color = true, visible = "animals")
    public int animalColor = 0xFF33FF33;
    @Parameter(name = "ItemColor", color = true, visible = "items")
    public int itemColor = 0xFFFFFF33;

    private static Identifier arrowTexture;
    private static boolean arrowLoaded = false;

    private static Identifier getArrowTexture() {
        if (!arrowLoaded) {
            try (java.io.InputStream stream = Tracers.class.getResourceAsStream("/assets/ravex/textures/arrow.png")) {
                if (stream != null) {
                    NativeImage image = NativeImage.read(stream);
                    DynamicTexture tex = new DynamicTexture(() -> "tracers_arrow", image);
                    Render2DUtility.setLinearSampler(tex);
                    arrowTexture = Identifier.fromNamespaceAndPath("ravex", "tracers_arrow");
                    MinecraftWrapper.getWrapper().getTextureManager().register(arrowTexture, tex);
                    arrowLoaded = true;
                }
            } catch (Exception e) {
                RaveX.LOGGER.warn("[Tracers] Failed to load arrow texture: {}", e.getMessage());
                arrowLoaded = true;
            }
        }
        return arrowTexture;
    }

    private static final HashMap<Integer, Float> arrowAngles = new HashMap<>();
    private static float gpsCurrentAngle = 0f;
    private static boolean gpsAngleInitialized = false;

    public static void renderArrows(GuiGraphics context, List<Entity> entities, List<Integer> colors,
            float pt, Vec3 cameraPos, Vec3 cameraLook,
            double guiWidth, double guiHeight) {
        Tracers t = Modules.get(Tracers.class);
        if (!Modules.enabled(Tracers.class) || (!t.mode.equals("ArrowOld") && !t.mode.equals("ArrowNew")))
            return;

        boolean useNewArrow = t.mode.equals("ArrowNew");
        Identifier tex = null;
        if (!useNewArrow) {
            tex = getArrowTexture();
            if (tex == null) return;
        }

        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null)
            return;

        float size = (float) t.arrowSize;
        float margin = (float) t.arrowMargin;
        float radius = size * 0.7f + margin;
        float smoothSpeed = 0.12f;
        float minGap = 0.25f;

        double cx = guiWidth / 2.0;
        double cy = guiHeight / 2.0;
        float playerYawRad = (float) Math.toRadians(mc.getPlayer().getYRot());

        if (useNewArrow && GpsCmd.getGpsTarget() != null) {
            BlockPos gpsPos = GpsCmd.getGpsTarget();
            double gdx = gpsPos.getX() + 0.5 - cameraPos.x;
            double gdz = gpsPos.getZ() + 0.5 - cameraPos.z;
            double gLen = Math.sqrt(gdx * gdx + gdz * gdz);
            if (gLen < 5.0) {
                GpsCmd.setGpsTarget(null);
                gpsAngleInitialized = false;
                CmdReg.print("§a[GPS] You have reached your destination!");
            } else {
                float targetAngle = -(float) Math.atan2(gdx, gdz) - playerYawRad;
                while (targetAngle > Math.PI) targetAngle -= 2 * Math.PI;
                while (targetAngle < -Math.PI) targetAngle += 2 * Math.PI;

                if (!gpsAngleInitialized) {
                    gpsCurrentAngle = targetAngle;
                    gpsAngleInitialized = true;
                } else {
                    float diff = targetAngle - gpsCurrentAngle;
                    while (diff > Math.PI) diff -= 2 * Math.PI;
                    while (diff < -Math.PI) diff += 2 * Math.PI;
                    float step = Math.abs(diff) > 1.8f ? 0.6f : 0.35f;
                    gpsCurrentAngle += diff * step;
                }

                float gpsRadius = radius + size + 8.0f;
                float px = (float) (cx + Math.cos(gpsCurrentAngle - Math.PI / 2) * gpsRadius);
                float py = (float) (cy + Math.sin(gpsCurrentAngle - Math.PI / 2) * gpsRadius);

                int gpsColor = ColorUtility.getActiveColor();
                if (!t.colorMode.equals("Custom")) {
                    float deg = (float) Math.toDegrees(gpsCurrentAngle);
                    gpsColor = switch (t.colorMode) {
                        case "Rainbow" -> ColorUtility.rainbow(3000, (int) (deg * 4), 1.0f, 1.0f, 1.0f);
                        case "Astolfo" -> ColorUtility.astolfo(3000, (int) (deg * 4), 0.7f, 1.0f, 1.0f);
                        case "Client" -> ColorUtility.getActiveColor();
                        default -> gpsColor;
                    };
                }

                Render2DUtility.drawSharpArrow(context, px, py, gpsCurrentAngle, size, gpsColor);

                String label = "gps (" + (int) gLen + "m)";
                int tw = FontRenderUtility.getStringWidth(label);
                float tx = (float) (cx + Math.cos(gpsCurrentAngle - Math.PI / 2) * (gpsRadius + size * 0.7f));
                float ty = (float) (cy + Math.sin(gpsCurrentAngle - Math.PI / 2) * (gpsRadius + size * 0.7f));
                var pose = context.pose();
                pose.pushMatrix();
                pose.translate(tx, ty);
                pose.scale(0.65f, 0.65f);
                FontRenderUtility.drawString(context, label, -tw / 2, -4, 0xFFFFFFFF, true);
                pose.popMatrix();
            }
        } else {
            gpsAngleInitialized = false;
        }

        int count = Math.min(entities.size(), colors.size());
        if (count == 0)
            return;

        float[] targetAngles = new float[count];
        int[] ids = new int[count];
        int[] colorArr = new int[count];
        boolean[] valid = new boolean[count];
        int validCount = 0;

        for (int i = 0; i < count; i++) {
            Entity target = entities.get(i);
            int color = colors.get(i);
            colorArr[i] = color;
            if ((color >> 24 & 0xFF) == 0)
                continue;

            Vec3 basePos = target.getPosition(pt);
            double dx = basePos.x - cameraPos.x;
            double dz = basePos.z - cameraPos.z;
            double len = Math.sqrt(dx * dx + dz * dz);

            ids[i] = target.getId();

            if (len < 0.20) {
                int id = target.getId();
                float prev = arrowAngles.getOrDefault(id, 0.0f);
                targetAngles[i] = prev;
                valid[i] = true;
                validCount++;
                continue;
            }

            float targetAngle = -(float) Math.atan2(dx, dz) - playerYawRad;
            while (targetAngle > Math.PI)
                targetAngle -= 2 * Math.PI;
            while (targetAngle < -Math.PI)
                targetAngle += 2 * Math.PI;

            targetAngles[i] = targetAngle;
            valid[i] = true;
            validCount++;
        }

        if (validCount == 0)
            return;

        float[] adjustedAngles = targetAngles.clone();
        if (validCount > 1) {
            Integer[] order = new Integer[validCount];
            int idx = 0;
            for (int i = 0; i < count; i++) {
                if (valid[i])
                    order[idx++] = i;
            }
            Arrays.sort(order, (a, b) -> Float.compare(targetAngles[a], targetAngles[b]));

            for (int i = 0; i < validCount - 1; i++) {
                int prev = order[i];
                int curr = order[i + 1];
                float diff = targetAngles[curr] - targetAngles[prev];
                if (diff < minGap) {
                    adjustedAngles[curr] = adjustedAngles[prev] + minGap;
                }
            }

            for (int i = 0; i < validCount; i++) {
                int oi = order[i];
                while (adjustedAngles[oi] > Math.PI)
                    adjustedAngles[oi] -= 2 * Math.PI;
                while (adjustedAngles[oi] < -Math.PI)
                    adjustedAngles[oi] += 2 * Math.PI;
            }
        }

        for (int i = 0; i < count; i++) {
            if (!valid[i])
                continue;
            int color = colorArr[i];
            if ((color >> 24 & 0xFF) == 0)
                continue;

            float targetAngle = adjustedAngles[i];

            int id = ids[i];
            Float currentRaw = arrowAngles.get(id);
            float currentAngle;
            if (currentRaw == null) {
                currentAngle = targetAngle;
            } else {
                currentAngle = currentRaw;
                float diff = targetAngle - currentAngle;
                while (diff > Math.PI)
                    diff -= 2 * Math.PI;
                while (diff < -Math.PI)
                    diff += 2 * Math.PI;
                float stepSpeed = Math.abs(diff) > 1.8f ? 0.6f : 0.25f;
                currentAngle += diff * stepSpeed;
            }
            arrowAngles.put(id, currentAngle);

            float px = (float) (cx + Math.cos(currentAngle - Math.PI / 2) * radius);
            float py = (float) (cy + Math.sin(currentAngle - Math.PI / 2) * radius);

            if (useNewArrow) {
                int finalColor = color;
                if (!t.colorMode.equals("Custom")) {
                    float deg = (float) Math.toDegrees(currentAngle);
                    finalColor = switch (t.colorMode) {
                        case "Rainbow" -> ColorUtility.rainbow(3000, (int) (deg * 4), 1.0f, 1.0f, 1.0f);
                        case "Astolfo" -> ColorUtility.astolfo(3000, (int) (deg * 4), 0.7f, 1.0f, 1.0f);
                        case "Client" -> ColorUtility.getActiveColor();
                        default -> color;
                    };
                }
                Render2DUtility.drawSharpArrow(context, px, py, currentAngle, size, finalColor);
            } else {
                context.pose().pushMatrix();
                context.pose().translate(px, py);
                context.pose().rotate(currentAngle);
                float hs = size / 2f;
                context.blit(RenderPipelines.GUI_TEXTURED, tex,
                        (int) -hs, (int) -hs, 0f, 0f,
                        (int) size, (int) size, (int) size, (int) size, color);
                context.pose().popMatrix();
            }
        }
    }
}