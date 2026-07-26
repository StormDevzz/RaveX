package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import org.joml.Matrix4f;

import ravex.utility.render.Render3DUtility;

@ModuleInfo(name = "ChinaHat", category = "Render")
public class ChinaHat implements ModuleAccess {
public static final ChinaHat INSTANCE = new ChinaHat();

    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Parameter(name = "Alpha", min = 0.0, max = 255.0, step = 1.0)
    public double alpha = 200.0;
    @Parameter(name = "Radius", min = 0.3, max = 1.5, step = 0.05)
    public double radius = 0.6;
    @Parameter(name = "Height", min = 0.1, max = 1.0, step = 0.05)
    public double height = 0.4;

    private ChinaHat() {
        
    }

    public static void render(Matrix4f modelViewMatrix, net.minecraft.world.phys.Vec3 camPos) {
        ChinaHat ch = ravex.manager.ModuleManager.delegate(ChinaHat.class);
        if (ch == null || !ch.getEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int c = ch.color;
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;
        float a = ((c >> 24) & 0xFF) / 255.0f * (float)(ch.alpha / 255.0);
        if (a <= 0.01f) return;

        double R = ch.radius;
        double H = ch.height;
        int segments = 16;
        int layers = 5;
        double dotSize = 0.09;
        Matrix4f mat = new Matrix4f();

        for (net.minecraft.world.entity.player.Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isRemoved() || !player.isAlive()) continue;

            net.minecraft.world.phys.Vec3 pos = player.position();
            float headY = (float)(pos.y + player.getBbHeight() + 0.05);

            float px = (float)(pos.x - camPos.x);
            float py = (float)(headY - camPos.y);
            float pz = (float)(pos.z - camPos.z);

            for (int layer = 0; layer <= layers; layer++) {
                float ly = (float)(H * layer / layers);
                double rAtLayer = layer == layers ? 0.0 : R * (1.0 - (double)layer / layers);

                for (int seg = 0; seg < segments; seg++) {
                    double angle = 2.0 * Math.PI * (seg + 0.5 * (layer % 2)) / segments;
                    float bx = (float)(Math.cos(angle) * rAtLayer);
                    float bz = (float)(Math.sin(angle) * rAtLayer);

                    mat.identity();
                    modelViewMatrix.translate(px + bx, py + ly, pz + bz, mat);
                    Render3DUtility.batchFilledBox(mat, dotSize, r, g, b, a, false);
                }
            }
        }
    }


}