package volthack.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import volthack.event.EventBus;
import volthack.event.Render3DEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void onRenderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, net.minecraft.client.renderer.state.BlockOutlineRenderState blockOutlineRenderState, int i, float g, CallbackInfo ci) {
        if (volthack.modules.render.BlockOutline.INSTANCE.getEnabled()) {
            if (volthack.modules.render.BlockOutline.INSTANCE.getOutlineMode().equals("Full")) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(
        method = "renderHitOutline",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer modifyVertexConsumer(VertexConsumer original) {
        if (volthack.modules.render.BlockOutline.INSTANCE.getEnabled()) {
            return new volthack.util.render.ColoredVertexConsumer(original, () -> {
                return volthack.modules.render.BlockOutline.INSTANCE.getCurrentColor();
            });
        }
        return original;
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderLevel(
            com.mojang.blaze3d.resource.GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            Matrix4f matrix4f3,
            com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
            org.joml.Vector4f vector4f,
            boolean bl2,
            CallbackInfo ci
    ) {
        EventBus.INSTANCE.emit(new Render3DEvent(
            deltaTracker.getGameTimeDeltaPartialTick(true),
            modelViewMatrix,
            projectionMatrix
        ));
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void onAddWeatherPass(com.mojang.blaze3d.framegraph.FrameGraphBuilder frameGraphBuilder, com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
        if (volthack.modules.render.NoRender.INSTANCE.getEnabled() && volthack.modules.render.NoRender.INSTANCE.getWeather()) {
            ci.cancel();
        }
    }
}
