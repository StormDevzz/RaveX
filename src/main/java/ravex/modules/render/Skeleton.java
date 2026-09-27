package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ravex.utility.misc.EntityUtility;
import ravex.utility.render.Render3DUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;
@Module(name = "Skeleton", category = "Render")
public class Skeleton {
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Parameter(name = "LineWidth", min = 0.5, max = 3.0, step = 0.1)
    public double lineWidth = 1.0;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;
    @Parameter(name = "Glow")
    public boolean glow = false;
    @Parameter(name = "Dick")
    public boolean dick = false;
    @Parameter(name = "Players")
    public boolean players = true;
    private static class FrameJob {
        final int color;
        final float width;
        final boolean throughWalls;
        final boolean glow;
        final java.util.List<float[]> segments;
        FrameJob(int color, float width, boolean throughWalls, boolean glow, java.util.List<float[]> segments) {
            this.color = color;
            this.width = width;
            this.throughWalls = throughWalls;
            this.glow = glow;
            this.segments = segments;
        }
    }
    private static final java.util.Map<Integer, FrameJob> frameJobs = new java.util.LinkedHashMap<>();

    public static void beginFrame() {
        frameJobs.clear();
    }

    public static boolean collectSegments(int entityId, java.util.List<float[]> modelSegments, Matrix4f rootMatrix, int colorVal, float lineWidth, boolean throughWalls, boolean glow) {
        if (frameJobs.containsKey(entityId)) return false;
        java.util.List<float[]> world = new java.util.ArrayList<>(modelSegments.size());
        for (float[] seg : modelSegments) {
            world.add(new float[]{
                rootMatrix.m00() * seg[0] + rootMatrix.m10() * seg[1] + rootMatrix.m20() * seg[2] + rootMatrix.m30(),
                rootMatrix.m01() * seg[0] + rootMatrix.m11() * seg[1] + rootMatrix.m21() * seg[2] + rootMatrix.m31(),
                rootMatrix.m02() * seg[0] + rootMatrix.m12() * seg[1] + rootMatrix.m22() * seg[2] + rootMatrix.m32(),
                rootMatrix.m00() * seg[3] + rootMatrix.m10() * seg[4] + rootMatrix.m20() * seg[5] + rootMatrix.m30(),
                rootMatrix.m01() * seg[3] + rootMatrix.m11() * seg[4] + rootMatrix.m21() * seg[5] + rootMatrix.m31(),
                rootMatrix.m02() * seg[3] + rootMatrix.m12() * seg[4] + rootMatrix.m22() * seg[5] + rootMatrix.m32()
            });
        }
        frameJobs.put(entityId, new FrameJob(colorVal, lineWidth, throughWalls, glow, world));
        return true;
    }

    public void renderWorld(Matrix4f modelViewMatrix) {
        if (frameJobs.isEmpty()) return;
        for (FrameJob job : frameJobs.values()) {
            float r = ((job.color >> 16) & 0xFF) / 255.0f;
            float g = ((job.color >> 8) & 0xFF) / 255.0f;
            float bl = (job.color & 0xFF) / 255.0f;
            float al = ((job.color >> 24) & 0xFF) / 255.0f;
            if (al == 0.0f) al = 1.0f;
            java.util.List<Vector3f> pts = new java.util.ArrayList<>(2);
            pts.add(new Vector3f());
            pts.add(new Vector3f());
            for (float[] seg : job.segments) {
                pts.get(0).set(seg[0], seg[1], seg[2]);
                pts.get(1).set(seg[3], seg[4], seg[5]);
                Render3DUtility.batchLineStrip(modelViewMatrix, pts, r, g, bl, al, job.width, job.throughWalls);
                if (job.glow) {
                    Render3DUtility.batchLineAdditive(modelViewMatrix, pts, r, g, bl, al * 0.35f, job.width * 2.5f, job.throughWalls);
                }
            }
        }
    }

    @Nullable
    public static net.minecraft.world.entity.LivingEntity getEntityBeingRendered(PoseStack poseStack) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getGameRenderer() == null || mc.getGameRenderer().getMainCamera() == null) return null;
        Matrix4f matrix = poseStack.last().pose();
        float tx = matrix.m30();
        float ty = matrix.m31();
        float tz = matrix.m32();
        double camX = mc.getGameRenderer().getMainCamera().position().x;
        double camY = mc.getGameRenderer().getMainCamera().position().y;
        double camZ = mc.getGameRenderer().getMainCamera().position().z;
        double worldX = tx + camX;
        double worldY = ty + camY;
        double worldZ = tz + camZ;
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestDist = 9.0;
        for (net.minecraft.world.entity.Entity entity : mc.getLevel().entitiesForRendering()) {
            net.minecraft.world.entity.LivingEntity le = EntityUtility.asLivingEntity(entity);
            if (le == null) continue;
            double dx = worldX - le.getX();
            double dy = worldY - le.getY();
            double dz = worldZ - le.getZ();
            double dist = dx*dx + dy*dy + dz*dz;
            if (dist < bestDist) {
                bestDist = dist;
                closest = le;
            }
        }
        return closest;
    }
    public static boolean shouldRender(net.minecraft.world.entity.Entity entity) {
        if (!Modules.enabled(Skeleton.class)) return false;
        Skeleton s = Modules.get(Skeleton.class);
        if (s == null || !s.players) return false;
        return EntityUtility.isSkeletonTarget(entity);
    }
    private static float[] part(net.minecraft.client.model.geom.ModelPart p) {
        return new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot};
    }
    private static void seg(java.util.List<float[]> out, float x1, float y1, float z1, float x2, float y2, float z2) {
        out.add(new float[]{x1, y1, z1, x2, y2, z2});
    }
    public static void drawHumanoid(PoseStack poseStack, HumanoidModel<?> model, int entityId, int colorVal, float lineWidth, boolean throughWalls, boolean dick, boolean glow) {
        float scale = 0.0625f;
        float[] head = part(model.head);
        float[] armL = part(model.leftArm);
        float[] armR = part(model.rightArm);
        float[] legL = part(model.leftLeg);
        float[] legR = part(model.rightLeg);
        float headX = head[0] * scale;
        float headY = head[1] * scale;
        float headZ = head[2] * scale;
        float leftArmX = armL[0] * scale;
        float leftArmY = armL[1] * scale;
        float leftArmZ = armL[2] * scale;
        float rightArmX = armR[0] * scale;
        float rightArmY = armR[1] * scale;
        float rightArmZ = armR[2] * scale;
        float leftLegX = legL[0] * scale;
        float leftLegY = legL[1] * scale;
        float leftLegZ = legL[2] * scale;
        float rightLegX = legR[0] * scale;
        float rightLegY = legR[1] * scale;
        float rightLegZ = legR[2] * scale;
        float midShoulderX = (leftArmX + rightArmX) / 2.0f;
        float midShoulderY = (leftArmY + rightArmY) / 2.0f;
        float midShoulderZ = (leftArmZ + rightArmZ) / 2.0f;
        float midHipX = (leftLegX + rightLegX) / 2.0f;
        float midHipY = (leftLegY + rightLegY) / 2.0f;
        float midHipZ = (leftLegZ + rightLegZ) / 2.0f;
        java.util.List<float[]> segments = new java.util.ArrayList<>();
        seg(segments, leftArmX, leftArmY, leftArmZ, rightArmX, rightArmY, rightArmZ);
        seg(segments, leftLegX, leftLegY, leftLegZ, rightLegX, rightLegY, rightLegZ);
        seg(segments, headX, headY, headZ, midShoulderX, midShoulderY, midShoulderZ);
        seg(segments, midShoulderX, midShoulderY, midShoulderZ, midHipX, midHipY, midHipZ);
        float headLen = 8f * scale;
        float armLen = 10f * scale;
        float legLen = 12f * scale;
        org.joml.Vector3f headTip = rotatedLimb(head, headX, headY, headZ, -headLen);
        seg(segments, headX, headY, headZ, headTip.x, headTip.y, headTip.z);
        org.joml.Vector3f armLTip = rotatedLimb(armL, leftArmX, leftArmY, leftArmZ, armLen);
        seg(segments, leftArmX, leftArmY, leftArmZ, armLTip.x, armLTip.y, armLTip.z);
        org.joml.Vector3f armRTip = rotatedLimb(armR, rightArmX, rightArmY, rightArmZ, armLen);
        seg(segments, rightArmX, rightArmY, rightArmZ, armRTip.x, armRTip.y, armRTip.z);
        org.joml.Vector3f legLTip = rotatedLimb(legL, leftLegX, leftLegY, leftLegZ, legLen);
        seg(segments, leftLegX, leftLegY, leftLegZ, legLTip.x, legLTip.y, legLTip.z);
        org.joml.Vector3f legRTip = rotatedLimb(legR, rightLegX, rightLegY, rightLegZ, legLen);
        seg(segments, rightLegX, rightLegY, rightLegZ, legRTip.x, legRTip.y, legRTip.z);
        if (dick) {
            float tipY = midHipY + 11f * scale;
            seg(segments, midHipX, midHipY, midHipZ, midHipX, tipY, midHipZ);
            float ball = 2.2f * scale;
            seg(segments, midHipX - ball, tipY, midHipZ, midHipX + ball, tipY, midHipZ);
            seg(segments, midHipX, tipY - ball, midHipZ, midHipX, tipY + ball, midHipZ);
        }
        collectSegments(entityId, segments, new Matrix4f(poseStack.last().pose()), colorVal, lineWidth, throughWalls, glow);
    }
    private static org.joml.Vector3f rotatedLimb(float[] p, float px, float py, float pz, float len) {
        org.joml.Vector3f v = new org.joml.Vector3f(0, len, 0);
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotationZ(p[5]);
        q.mul(new org.joml.Quaternionf().rotationY(p[4]));
        q.mul(new org.joml.Quaternionf().rotationX(p[3]));
        v.rotate(q);
        return new org.joml.Vector3f(px + v.x, py + v.y, pz + v.z);
    }
}