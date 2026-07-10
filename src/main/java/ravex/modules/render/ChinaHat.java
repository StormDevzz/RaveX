package ravex.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtils;

public class ChinaHat extends Module {
    public static final ChinaHat INSTANCE = new ChinaHat();

    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final NumberParameter alpha = new NumberParameter("Alpha", 200.0, 0.0, 255.0, 1.0);
    public final NumberParameter radius = new NumberParameter("Radius", 0.6, 0.3, 1.5, 0.05);
    public final NumberParameter height = new NumberParameter("Height", 0.4, 0.1, 1.0, 0.05);

    private ChinaHat() {
        super("ChinaHat");
    }

    public static void render(Matrix4f modelViewMatrix, Vec3 camPos) {
        ChinaHat ch = ModuleManager.get(ChinaHat.class);
        if (ch == null || !ch.getEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int c = ch.color.getValue();
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;
        float a = ((c >> 24) & 0xFF) / 255.0f * (float)(ch.alpha.getValue() / 255.0);
        if (a <= 0.01f) return;

        double R = ch.radius.getValue();
        double H = ch.height.getValue();
        int segments = 16;
        int layers = 5;
        double dotSize = 0.09;
        Matrix4f mat = new Matrix4f();

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isRemoved() || !player.isAlive()) continue;

            Vec3 pos = player.position();
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
                    Render3DUtils.batchFilledBox(mat, dotSize, r, g, b, a, false);
                }
            }
        }
    }
}
