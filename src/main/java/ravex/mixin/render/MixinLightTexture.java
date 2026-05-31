package ravex.mixin.render;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ravex.modules.render.Fullbright;

@Mixin(LightTexture.class)
public class MixinLightTexture {
    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0),
        index = 0,
        remap = false
    )
    private float modifyAmbientLight(float value) {
        return Fullbright.INSTANCE.getEnabled() ? 1.0f : value;
    }

    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3),
        index = 0,
        remap = false
    )
    private float modifyNightVision(float value) {
        return Fullbright.INSTANCE.getEnabled() ? 1.0f : value;
    }

    @ModifyArg(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 4),
        index = 0,
        remap = false
    )
    private float modifyDarkness(float value) {
        return Fullbright.INSTANCE.getEnabled() ? 0.0f : value;
    }
}
