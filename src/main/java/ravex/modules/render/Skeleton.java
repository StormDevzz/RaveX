package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.HumanoidModel;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.opengl.GlStateManager;
import ravex.utility.render.BlockRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class Skeleton extends Module {
<<<<<<< HEAD
=======
    public static final Skeleton INSTANCE = new Skeleton();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final NumberParameter lineWidth = new NumberParameter("LineWidth", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", true);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    public static net.minecraft.world.entity.LivingEntity getEntityBeingRendered(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) return null;
        Matrix4f matrix = poseStack.last().pose();
        float tx = matrix.m30();
        float ty = matrix.m31();
        float tz = matrix.m32();
        double camX = mc.gameRenderer.getMainCamera().position().x;
        double camY = mc.gameRenderer.getMainCamera().position().y;
        double camZ = mc.gameRenderer.getMainCamera().position().z;
        double worldX = tx + camX;
        double worldY = ty + camY;
        double worldZ = tz + camZ;
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestDist = 9.0; 
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity) {
                net.minecraft.world.entity.LivingEntity le = (net.minecraft.world.entity.LivingEntity) entity;
                double ex = le.getX();
                double ey = le.getY();
                double ez = le.getZ();
                double dx = worldX - ex;
                double dy = worldY - ey;
                double dz = worldZ - ez;
                double dist = dx*dx + dy*dy + dz*dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    closest = le;
                }
            }
        }
        return closest;
    }
    public static void renderSkeleton(PoseStack poseStack, HumanoidModel<?> model, int colorVal, float lineWidth, boolean throughWalls) {
        float r = ((colorVal >> 16) & 0xFF) / 255.0f;
        float g = ((colorVal >> 8) & 0xFF) / 255.0f;
        float b = (colorVal & 0xFF) / 255.0f;
        float a = ((colorVal >> 24) & 0xFF) / 255.0f;
        if (a == 0.0f) a = 1.0f;
        if (throughWalls) {
            GlStateManager._disableDepthTest();
        }
        RenderType lineType = net.minecraft.client.renderer.rendertype.RenderTypes.lines();
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, lineType.mode(), lineType.format());
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        float scale = 0.0625f; 
        float headX = model.head.x * scale;
        float headY = model.head.y * scale;
        float headZ = model.head.z * scale;
        float leftArmX = model.leftArm.x * scale;
        float leftArmY = model.leftArm.y * scale;
        float leftArmZ = model.leftArm.z * scale;
        float rightArmX = model.rightArm.x * scale;
        float rightArmY = model.rightArm.y * scale;
        float rightArmZ = model.rightArm.z * scale;
        float leftLegX = model.leftLeg.x * scale;
        float leftLegY = model.leftLeg.y * scale;
        float leftLegZ = model.leftLeg.z * scale;
        float rightLegX = model.rightLeg.x * scale;
        float rightLegY = model.rightLeg.y * scale;
        float rightLegZ = model.rightLeg.z * scale;
        float neckX = headX;
        float neckY = headY;
        float neckZ = headZ;
        float midShoulderX = (leftArmX + rightArmX) / 2.0f;
        float midShoulderY = (leftArmY + rightArmY) / 2.0f;
        float midShoulderZ = (leftArmZ + rightArmZ) / 2.0f;
        float midHipX = (leftLegX + rightLegX) / 2.0f;
        float midHipY = (leftLegY + rightLegY) / 2.0f;
        float midHipZ = (leftLegZ + rightLegZ) / 2.0f;
        Matrix4f rootMatrix = poseStack.last().pose();
        drawLine(builder, rootMatrix, leftArmX, leftArmY, leftArmZ, rightArmX, rightArmY, rightArmZ, ir, ig, ib, ia, lineWidth);
        drawLine(builder, rootMatrix, leftLegX, leftLegY, leftLegZ, rightLegX, rightLegY, rightLegZ, ir, ig, ib, ia, lineWidth);
        drawLine(builder, rootMatrix, neckX, neckY, neckZ, midShoulderX, midShoulderY, midShoulderZ, ir, ig, ib, ia, lineWidth);
        drawLine(builder, rootMatrix, midShoulderX, midShoulderY, midShoulderZ, midHipX, midHipY, midHipZ, ir, ig, ib, ia, lineWidth);
        poseStack.pushPose();
        poseStack.translate(headX, headY, headZ);
        if (model.head.zRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationZ(model.head.zRot));
        if (model.head.yRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationY(model.head.yRot));
        if (model.head.xRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationX(model.head.xRot));
        drawLine(builder, poseStack.last().pose(), 0, 0, 0, 0, -8f * scale, 0, ir, ig, ib, ia, lineWidth);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(leftArmX, leftArmY, leftArmZ);
        if (model.leftArm.zRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationZ(model.leftArm.zRot));
        if (model.leftArm.yRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationY(model.leftArm.yRot));
        if (model.leftArm.xRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationX(model.leftArm.xRot));
        drawLine(builder, poseStack.last().pose(), 0, 0, 0, 0, 12f * scale, 0, ir, ig, ib, ia, lineWidth);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(rightArmX, rightArmY, rightArmZ);
        if (model.rightArm.zRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationZ(model.rightArm.zRot));
        if (model.rightArm.yRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationY(model.rightArm.yRot));
        if (model.rightArm.xRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationX(model.rightArm.xRot));
        drawLine(builder, poseStack.last().pose(), 0, 0, 0, 0, 12f * scale, 0, ir, ig, ib, ia, lineWidth);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(leftLegX, leftLegY, leftLegZ);
        if (model.leftLeg.zRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationZ(model.leftLeg.zRot));
        if (model.leftLeg.yRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationY(model.leftLeg.yRot));
        if (model.leftLeg.xRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationX(model.leftLeg.xRot));
        drawLine(builder, poseStack.last().pose(), 0, 0, 0, 0, 12f * scale, 0, ir, ig, ib, ia, lineWidth);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(rightLegX, rightLegY, rightLegZ);
        if (model.rightLeg.zRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationZ(model.rightLeg.zRot));
        if (model.rightLeg.yRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationY(model.rightLeg.yRot));
        if (model.rightLeg.xRot != 0.0f) poseStack.mulPose(new Quaternionf().rotationX(model.rightLeg.xRot));
        drawLine(builder, poseStack.last().pose(), 0, 0, 0, 0, 12f * scale, 0, ir, ig, ib, ia, lineWidth);
        poseStack.popPose();
        MeshData mesh = builder.buildOrThrow();
        lineType.draw(mesh);
        if (throughWalls) {
            GlStateManager._enableDepthTest();
        }
    }
    private static void drawLine(BufferBuilder builder, Matrix4f matrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 int r, int g, int b, int a, float lineWidth) {
        BlockRenderer.renderLine3D(builder, matrix, x1, y1, z1, x2, y2, z2, r, g, b, a, lineWidth);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Skeleton.class);
    }

    public static Skeleton itz() {
        return ModuleManager.get(Skeleton.class);
    }
}
