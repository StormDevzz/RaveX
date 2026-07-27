package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "Tracers", category = "Render")
public class Tracers implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Default", "Arrows"})
    public String mode = "Default";
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
    @Parameter(name = "Width", min = 0.1, max = 5.0, step = 0.1)
    public double lineWidth = 1.0;
    @Parameter(name = "ArrowSize", min = 8.0, max = 48.0, step = 2.0)
    public double arrowSize = 20.0;
    @Parameter(name = "ArrowMargin", min = 0.0, max = 30.0, step = 1.0)
    public double arrowMargin = 4.0;
    @Parameter(name = "PlayerColor", color = true)
    public int playerColor = 0xFFFF3333;
    @Parameter(name = "MobColor", color = true)
    public int mobColor = 0xFFFF3333;
    @Parameter(name = "AnimalColor", color = true)
    public int animalColor = 0xFF33FF33;
    @Parameter(name = "ItemColor", color = true)
    public int itemColor = 0xFFFFFF33;

    private static Identifier arrowTexture;
    private static boolean arrowLoaded = false;

    private Tracers() {
        
    }

    private static Identifier getArrowTexture() {
        if (!arrowLoaded) {
            try (java.io.InputStream stream = Tracers.class.getResourceAsStream("/assets/ravex/textures/arrow.png")) {
                if (stream != null) {
                    NativeImage image = NativeImage.read(stream);
                    DynamicTexture tex = new DynamicTexture(() -> "tracers_arrow", image);
                    try {
                        GpuSampler sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                                .getClampToEdge(FilterMode.LINEAR);
                        for (Field f : AbstractTexture.class.getDeclaredFields()) {
                            if (GpuSampler.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                f.set(tex, sampler);
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    arrowTexture = Identifier.fromNamespaceAndPath("ravex", "tracers_arrow");
                    MinecraftWrapper.getInstance().getTextureManager().register(arrowTexture, tex);
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

    public static void renderArrows(GuiGraphics context, List<net.minecraft.world.entity.Entity> entities, List<Integer> colors,
            float pt, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.world.phys.Vec3 cameraLook,
            double guiWidth, double guiHeight) {
        Identifier tex = getArrowTexture();
        if (tex == null)
            return;

        Tracers t = ravex.manager.ModuleManager.delegate(Tracers.class);
        if (!t.getEnabled() || !t.mode.equals("Arrows"))
            return;

        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null)
            return;

        float size = (float) t.arrowSize;
        float margin = (float) t.arrowMargin;
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
            net.minecraft.world.entity.Entity target = entities.get(i);
            int color = colors.get(i);
            colorArr[i] = color;
            if ((color >> 24 & 0xFF) == 0)
                continue;

            net.minecraft.world.phys.Vec3 basePos = target.getPosition(pt);
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Tracers").getEnabled();
    }

    public static Tracers itz() {
        return ravex.manager.ModuleManager.delegate(Tracers.class);
    }


}