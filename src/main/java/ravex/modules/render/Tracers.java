package ravex.modules.render;
import ravex.manager.ModuleManager;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class Tracers extends Module {
<<<<<<< HEAD
=======
    public static final Tracers INSTANCE = new Tracers();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Default", List.of("Default", "Arrows"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", false);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);
    public final NumberParameter maxDistance = new NumberParameter("Distance", 100.0, 10.0, 300.0, 10.0);
    public final NumberParameter lineWidth = new NumberParameter("Width", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter arrowSize = new NumberParameter("ArrowSize", 20.0, 8.0, 48.0, 2.0);
    public final NumberParameter arrowMargin = new NumberParameter("ArrowMargin", 4.0, 0.0, 30.0, 1.0);
    public final ColorParameter playerColor = new ColorParameter("PlayerColor", 0xFFFF3333);
    public final ColorParameter mobColor = new ColorParameter("MobColor", 0xFFFF3333);
    public final ColorParameter animalColor = new ColorParameter("AnimalColor", 0xFF33FF33);
    public final ColorParameter itemColor = new ColorParameter("ItemColor", 0xFFFFFF33);

    private static Identifier arrowTexture;
    private static boolean arrowLoaded = false;

    private Tracers() {
        super("Tracers");
        lineWidth.setVisible(() -> mode.getValue().equals("Default"));
        arrowSize.setVisible(() -> mode.getValue().equals("Arrows"));
        arrowMargin.setVisible(() -> mode.getValue().equals("Arrows"));
        playerColor.setVisible(players::getValue);
        mobColor.setVisible(monsters::getValue);
        animalColor.setVisible(animals::getValue);
        itemColor.setVisible(items::getValue);
    }

    private static Identifier getArrowTexture() {
        if (!arrowLoaded) {
            try (java.io.InputStream stream = Tracers.class.getResourceAsStream("/assets/ravex/textures/arrow.png")) {
                if (stream != null) {
                    NativeImage image = NativeImage.read(stream);
                    DynamicTexture tex = new DynamicTexture(() -> "tracers_arrow", image);
                    try {
                        Field f = AbstractTexture.class.getDeclaredField("sampler");
                        f.setAccessible(true);
                        GpuSampler sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                                .getClampToEdge(FilterMode.LINEAR);
                        f.set(tex, sampler);
                    } catch (Exception ignored) {
                    }
                    arrowTexture = Identifier.fromNamespaceAndPath("ravex", "tracers_arrow");
                    Minecraft.getInstance().getTextureManager().register(arrowTexture, tex);
                    arrowLoaded = true;
                }
            } catch (Exception e) {
                ravex.RaveX.LOGGER.warn("[Tracers] Failed to load arrow texture: {}", e.getMessage());
                arrowLoaded = true;
            }
        }
        return arrowTexture;
    }

    private static final java.util.HashMap<Integer, Float> arrowAngles = new java.util.HashMap<>();

    public static void renderArrows(GuiGraphics context, List<Entity> entities, List<Integer> colors,
            float pt, Vec3 cameraPos, Vec3 cameraLook,
            double guiWidth, double guiHeight) {
        Identifier tex = getArrowTexture();
        if (tex == null)
            return;

<<<<<<< HEAD
        Tracers t = ModuleManager.get(Tracers.class);
=======
        Tracers t = INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!t.getEnabled() || !t.mode.getValue().equals("Arrows"))
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        float size = t.arrowSize.getValue().floatValue();
        float margin = t.arrowMargin.getValue().floatValue();
        float radius = size * 0.7f + margin;
        float smoothSpeed = 0.12f;
        float minGap = 0.25f;
        int count = Math.min(entities.size(), colors.size());
        if (count == 0)
            return;

        double cx = guiWidth / 2.0;
        double cy = guiHeight / 2.0;
        float playerYawRad = (float) Math.toRadians(mc.player.getYRot());


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

            if (len < 0.01) {
                Integer id = target.getId();
                if (!arrowAngles.containsKey(id))
                    continue;
                targetAngles[i] = arrowAngles.get(id);
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
                currentAngle += diff * smoothSpeed;
            }
            arrowAngles.put(id, currentAngle);

            float px = (float) (cx + Math.cos(currentAngle - Math.PI / 2) * radius);
            float py = (float) (cy + Math.sin(currentAngle - Math.PI / 2) * radius);

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
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Tracers.class);
    }

    public static Tracers itz() {
        return ModuleManager.get(Tracers.class);
    }
=======

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
