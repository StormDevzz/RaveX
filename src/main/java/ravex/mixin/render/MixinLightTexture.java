package ravex.mixin.render;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ravex.modules.render.Fullbright;
import ravex.modules.player.Xray;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    private static boolean isFullbright() {
        return Fullbright.maybeEnabled() || Xray.maybeEnabled();
    }

    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0, remap = false),
        index = 0
    )
    private float modifyAmbientLight(float value) {
<<<<<<< HEAD
        return isFullbright() ? Fullbright.itz().brightness.getValue().floatValue() : value;
=======
        return isFullbright() ? Fullbright.INSTANCE.brightness.getValue().floatValue() : value;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }

    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3, remap = false),
        index = 0
    )
    private float modifyNightVision(float value) {
<<<<<<< HEAD
        return isFullbright() ? Fullbright.itz().brightness.getValue().floatValue() : value;
=======
        return isFullbright() ? Fullbright.INSTANCE.brightness.getValue().floatValue() : value;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }

    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 4, remap = false),
        index = 0
    )
    private float modifyDarkness(float value) {
<<<<<<< HEAD
        return isFullbright() ? Fullbright.itz().darknessMult.getValue().floatValue() : value;
=======
        return isFullbright() ? Fullbright.INSTANCE.darknessMult.getValue().floatValue() : value;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
