package ravex.mixin.render;

import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.manager.ShaderManager;

@Mixin(PerspectiveProjectionMatrixBuffer.class)
public class MixinPerspectiveProjectionMatrixBuffer {

    @Inject(method = "getBuffer", at = @At("HEAD"))
    private void onGetBuffer(Matrix4f matrix, CallbackInfoReturnable<GpuBufferSlice> cir) {
        ShaderManager.INSTANCE.setProjectionMatrix(matrix);
    }
}
