package ravex.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.utility.render.BlockRenderer;
import ravex.utility.render.Render3DUtils;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderLevel(
        com.mojang.blaze3d.resource.GraphicsResourceAllocator graphicsResourceAllocator,
        net.minecraft.client.DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        net.minecraft.client.Camera camera,
        org.joml.Matrix4f modelViewMatrix,
        org.joml.Matrix4f projectionMatrix,
        org.joml.Matrix4f matrix3,
        com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
        org.joml.Vector4f vector4f,
        boolean bool2,
        CallbackInfo ci
    ) {
        if (!ravex.modules.player.AirPlace.INSTANCE.getEnabled()) return;
        Vec3 hp = ravex.modules.player.AirPlace.highlightPos;
        if (hp == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        try {
            // camera.position() is the public API — no reflection needed.
            // modelViewMatrix already encodes camera rotation. We just translate by
            // (worldPos - cameraPos) to place the wireframe at the correct world location.
            Vec3 camPos = camera.position();

            Matrix4f matrix = new Matrix4f(modelViewMatrix)
                .translate(
                    (float)(hp.x - camPos.x),
                    (float)(hp.y - camPos.y),
                    (float)(hp.z - camPos.z)
                );

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            VertexConsumer vertexConsumer = Render3DUtils.getLinesConsumer(bufferSource);
            if (vertexConsumer != null) {
                float r = 0.4f;
                float g = 0.8f;
                float b = 1.0f;
                float a = ravex.modules.player.AirPlace.renderAlpha * 0.9f;
                double size = ravex.modules.player.AirPlace.renderSize;
                BlockRenderer.renderWireframe(vertexConsumer, matrix, size, r, g, b, a);
                Render3DUtils.endLinesBatch(bufferSource);
            }
        } catch (Exception e) {
            // Fail silently to prevent spam
        }
    }
}
