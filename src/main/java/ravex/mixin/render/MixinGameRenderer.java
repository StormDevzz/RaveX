package ravex.mixin.render;

import com.mojang.blaze3d.platform.Window;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.AspectRatio;
import ravex.modules.Modules;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow
    private RenderBuffers renderBuffers;

    @ModifyArg(
        method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;", remap = false),
        index = 1
    )
    private float modifyAspectRatio(float aspect) {
        Window window = MinecraftWrapper.getInstance().getWindow();
        float original = (float) window.getWidth() / (float) window.getHeight();
        return Modules.get(AspectRatio.class).getAspectRatio(original);
    }

    @Inject(
        method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;",
        at = @At("RETURN")
    )
    private void onGetProjectionMatrix(float fov, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<org.joml.Matrix4f> cir) {
        ravex.manager.ShaderManager.INSTANCE.setProjectionMatrix(cir.getReturnValue());
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(net.minecraft.client.Camera camera, float f, boolean bl, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (!Modules.enabled(ravex.modules.render.Zoom.class)) return;
        ravex.modules.render.Zoom zoom = Modules.get(ravex.modules.render.Zoom.class);
        if (zoom == null) return;
        cir.setReturnValue(zoom.applyFov(cir.getReturnValue(), bl));
    }

    @Inject(
        method = "renderItemInHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterHandItemFlush(CallbackInfo ci) {
        renderBuffers.outlineBufferSource().endOutlineBatch();
    }
}
