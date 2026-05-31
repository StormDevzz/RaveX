package ravex.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.utility.render.Render3DUtils;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
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
        renderHighlights(camera);
    }

    private void renderHighlights(net.minecraft.client.Camera camera) {
        Vec3 hp = null;
        float alpha = 0.0f;
        float r = 0.2f;
        float g = 0.8f;
        float b = 1.0f;

        if (ravex.modules.player.AirPlace.INSTANCE.getEnabled()) {
            hp = ravex.modules.player.AirPlace.highlightPos;
            alpha = ravex.modules.player.AirPlace.renderAlpha;
            r = ravex.modules.player.AirPlace.renderR;
            g = ravex.modules.player.AirPlace.renderG;
            b = ravex.modules.player.AirPlace.renderB;
        } else if (ravex.modules.world.Scaffold.INSTANCE.getEnabled()) {
            hp = ravex.modules.world.Scaffold.highlightPos;
            alpha = ravex.modules.world.Scaffold.renderAlpha;
            r = ravex.modules.world.Scaffold.renderR;
            g = ravex.modules.world.Scaffold.renderG;
            b = ravex.modules.world.Scaffold.renderB;
        }

        System.out.println("[RaveX] renderHighlights: hp=" + hp + " alpha=" + alpha);
        if (hp == null || alpha <= 0.01f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        try {
            Vec3 camPos = camera.position();
            Matrix4f matrix = new Matrix4f()
                .rotate(camera.rotation())
                .translate(
                    (float)(hp.x - camPos.x),
                    (float)(hp.y - camPos.y),
                    (float)(hp.z - camPos.z)
                );

            float filledAlpha = alpha * 0.3f;
            float lineAlpha = alpha * 0.95f;

            Render3DUtils.renderFilledBox(matrix, 1.0, r, g, b, filledAlpha);
            Render3DUtils.renderWireframe(matrix, 1.0, r, g, b, lineAlpha);
            Render3DUtils.renderWireframe(matrix, 1.02, r, g, b, lineAlpha * 0.4f);
        } catch (Exception e) {
            System.out.println("[RaveX] Highlight error: " + e.getMessage());
        }
    }
}
