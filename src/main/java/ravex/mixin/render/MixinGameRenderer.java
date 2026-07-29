package ravex.mixin.render;

import com.mojang.blaze3d.platform.Window;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ravex.modules.render.AspectRatio;
import ravex.modules.Modules;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

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

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;",
        at = @At("RETURN")
    )
    private void onGetProjectionMatrix(float fov, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<org.joml.Matrix4f> cir) {
        ravex.manager.ShaderManager.INSTANCE.setProjectionMatrix(cir.getReturnValue());
    }
}
