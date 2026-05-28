package ravex.utility.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class Render3DUtils {
    public static double[] getCameraPos(net.minecraft.client.Camera camera) {
        net.minecraft.world.phys.Vec3 posVec = camera.position();
        return new double[]{posVec.x, posVec.y, posVec.z};
    }

    public static float getPitch(net.minecraft.client.Camera camera) {
        return camera.xRot();
    }

    public static float getYaw(net.minecraft.client.Camera camera) {
        return camera.yRot();
    }

    public static VertexConsumer getLinesConsumer(MultiBufferSource.BufferSource bufferSource) {
        // In 1.21.11, the lines() method in RenderTypes returns the correct lines RenderType
        return bufferSource.getBuffer(RenderTypes.lines());
    }

    public static VertexConsumer getQuadsConsumer(MultiBufferSource.BufferSource bufferSource) {
        // Returns the correct debugQuads RenderType for filled translucent block shapes
        return bufferSource.getBuffer(RenderTypes.debugQuads());
    }

    public static void endLinesBatch(MultiBufferSource.BufferSource bufferSource) {
        // Do nothing in 1.21.11 to prevent crashes during active render passes.
        // Minecraft's BufferSource will automatically flush all builders at the end of the pass.
    }
}
