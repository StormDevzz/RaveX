package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.AirPlace;
import ravex.modules.world.Scaffold;
import ravex.utility.render.BlockRenderer;
import ravex.utility.render.Render3DUtils;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    /**
     * Inject into renderBlockOutline — this is called once per frame during the block-outline
     * pass. It already has a correct PoseStack (camera-relative) and a BufferSource.
     * The LevelRenderState gives us the camera position via cameraRenderState.pos.
     */
    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    private void onRenderBlockOutline(
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        boolean highContrast,
        LevelRenderState levelRenderState,
        CallbackInfo ci
    ) {
        CameraRenderState cam = levelRenderState.cameraRenderState;
        if (cam == null || !cam.initialized) return;
        Vec3 camPos = cam.pos;

        // Get partial ticks for ultra-smooth interpolated animations!
        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        // ── AirPlace highlight ─────────────────────────────────────────
        if (AirPlace.INSTANCE.getEnabled()) {
            float alpha = AirPlace.INSTANCE.getRenderAlpha(partialTicks);
            if (alpha > 0.001f) {
                Vec3 hp = AirPlace.INSTANCE.getHighlightPos(partialTicks);
                if (hp != null) {
                    double size = AirPlace.INSTANCE.getRenderSize(partialTicks);
                    try {
                        VertexConsumer consumer = Render3DUtils.getLinesConsumer(bufferSource);
                        VertexConsumer quadsConsumer = Render3DUtils.getQuadsConsumer(bufferSource);
                        if (consumer != null && quadsConsumer != null) {
                            poseStack.pushPose();
                            try {
                                poseStack.translate(hp.x - camPos.x, hp.y - camPos.y, hp.z - camPos.z);
                                
                                // Center block and apply slow, premium spinning animation
                                poseStack.translate(0.5, 0.5, 0.5);
                                float rotationTime = (float) ((System.currentTimeMillis() % 4000) / 4000.0 * 2.0 * Math.PI);
                                poseStack.mulPose(new org.joml.Quaternionf().rotationY(rotationTime));
                                poseStack.translate(-0.5, -0.5, -0.5);
                                
                                // Render filled 3D block faces
                                BlockRenderer.renderFilled(
                                    quadsConsumer, poseStack.last().pose(),
                                    size, 0.3f, 0.75f, 1.0f, alpha * 0.15f
                                );
                                
                                // Render outer wireframe
                                BlockRenderer.renderWireframe(
                                    consumer, poseStack.last().pose(),
                                    size, 0.3f, 0.75f, 1.0f, alpha * 0.95f, 2.0f
                                );
                                
                                // Render smaller internal box for depth/3D effect
                                BlockRenderer.renderWireframe(
                                    consumer, poseStack.last().pose(),
                                    size * 0.99, 0.3f, 0.75f, 1.0f, alpha * 0.3f, 2.0f
                                );
                            } finally {
                                poseStack.popPose();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // ── Scaffold target highlight ──────────────────────────────────
        if (Scaffold.INSTANCE.getEnabled()) {
            float alpha = Scaffold.INSTANCE.getRenderAlpha(partialTicks);
            if (alpha > 0.001f) {
                Vec3 hp = Scaffold.INSTANCE.getHighlightPos(partialTicks);
                if (hp != null) {
                    try {
                        VertexConsumer consumer = Render3DUtils.getLinesConsumer(bufferSource);
                        VertexConsumer quadsConsumer = Render3DUtils.getQuadsConsumer(bufferSource);
                        if (consumer != null && quadsConsumer != null) {
                            poseStack.pushPose();
                            try {
                                poseStack.translate(hp.x - camPos.x, hp.y - camPos.y, hp.z - camPos.z);
                                
                                // Render filled 3D block faces
                                BlockRenderer.renderFilled(
                                    quadsConsumer, poseStack.last().pose(),
                                    1.0, 1.0f, 0.55f, 0.0f, alpha * 0.15f
                                );
                                
                                // Thick orange wireframe outline
                                BlockRenderer.renderWireframe(
                                    consumer, poseStack.last().pose(),
                                    1.0, 1.0f, 0.55f, 0.0f, alpha * 0.9f, 2.5f
                                );
                                
                                // Inner wireframe glow
                                BlockRenderer.renderWireframe(
                                    consumer, poseStack.last().pose(),
                                    0.99, 1.0f, 0.55f, 0.0f, alpha * 0.35f, 2.5f
                                );
                            } finally {
                                poseStack.popPose();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // ── Scaffold placed blocks tail animations ─────────────────────
        if (Scaffold.INSTANCE.getEnabled() && Scaffold.INSTANCE.render.getValue()) {
            String style = Scaffold.INSTANCE.animStyle.getValue();
            for (Scaffold.PlacedBlock pb : Scaffold.placedBlocks) {
                float alpha = pb.getAlpha(partialTicks);
                if (alpha > 0.001f) {
                    try {
                        VertexConsumer consumer = Render3DUtils.getLinesConsumer(bufferSource);
                        VertexConsumer quadsConsumer = Render3DUtils.getQuadsConsumer(bufferSource);
                        if (consumer != null && quadsConsumer != null) {
                            poseStack.pushPose();
                            try {
                                poseStack.translate(pb.pos.getX() - camPos.x, pb.pos.getY() - camPos.y, pb.pos.getZ() - camPos.z);

                                if ("Fill Up".equals(style)) {
                                    double fillProg = pb.getFillProgress(partialTicks);
                                    // A vertical rising fill animation from y=0 to y=fillProg
                                    BlockRenderer.renderFilledBounds(
                                        quadsConsumer, poseStack.last().pose(),
                                        0.0, 0.0, 0.0, 1.0, fillProg, 1.0,
                                        1.0f, 0.3f, 0.0f, alpha * 0.12f
                                    );
                                    
                                    BlockRenderer.renderWireframeBounds(
                                        consumer, poseStack.last().pose(),
                                        0.0, 0.0, 0.0, 1.0, fillProg, 1.0,
                                        1.0f, 0.3f, 0.0f, alpha * 0.7f, 1.5f
                                    );
                                    
                                    BlockRenderer.renderWireframeBounds(
                                        consumer, poseStack.last().pose(),
                                        0.005, 0.005, 0.005, 0.995, fillProg - 0.005 > 0 ? fillProg - 0.005 : 0, 0.995,
                                        1.0f, 0.3f, 0.0f, alpha * 0.2f, 1.5f
                                    );
                                } else if ("Expand".equals(style)) {
                                    double fillProg = pb.getFillProgress(partialTicks);
                                    double min = (1.0 - fillProg) / 2.0;
                                    double max = (1.0 + fillProg) / 2.0;
                                    
                                    BlockRenderer.renderFilledBounds(
                                        quadsConsumer, poseStack.last().pose(),
                                        min, min, min, max, max, max,
                                        1.0f, 0.3f, 0.0f, alpha * 0.12f
                                    );
                                    
                                    BlockRenderer.renderWireframeBounds(
                                        consumer, poseStack.last().pose(),
                                        min, min, min, max, max, max,
                                        1.0f, 0.3f, 0.0f, alpha * 0.7f, 1.5f
                                    );
                                    
                                    BlockRenderer.renderWireframeBounds(
                                        consumer, poseStack.last().pose(),
                                        min + 0.005, min + 0.005, min + 0.005, max - 0.005, max - 0.005, max - 0.005,
                                        1.0f, 0.3f, 0.0f, alpha * 0.2f, 1.5f
                                    );
                                } else if ("Shrink".equals(style)) {
                                    double size = pb.getSize(partialTicks);
                                    BlockRenderer.renderFilled(
                                        quadsConsumer, poseStack.last().pose(),
                                        size, 1.0f, 0.3f, 0.0f, alpha * 0.12f
                                    );
                                    
                                    BlockRenderer.renderWireframe(
                                        consumer, poseStack.last().pose(),
                                        size, 1.0f, 0.3f, 0.0f, alpha * 0.7f, 1.5f
                                    );
                                    
                                    BlockRenderer.renderWireframe(
                                        consumer, poseStack.last().pose(),
                                        size * 0.99, 1.0f, 0.3f, 0.0f, alpha * 0.2f, 1.5f
                                    );
                                } else {
                                    BlockRenderer.renderFilled(
                                        quadsConsumer, poseStack.last().pose(),
                                        1.0, 1.0f, 0.3f, 0.0f, alpha * 0.12f
                                    );
                                    
                                    BlockRenderer.renderWireframe(
                                        consumer, poseStack.last().pose(),
                                        1.0, 1.0f, 0.3f, 0.0f, alpha * 0.7f, 1.5f
                                    );
                                    
                                    BlockRenderer.renderWireframe(
                                        consumer, poseStack.last().pose(),
                                        0.99, 1.0f, 0.3f, 0.0f, alpha * 0.2f, 1.5f
                                    );
                                }
                            } finally {
                                poseStack.popPose();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
