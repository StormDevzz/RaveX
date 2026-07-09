package ravex.mixin.render;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.CloudColor;

@Mixin(EnvironmentAttributeProbe.class)
public class MixinCloudColor {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private <Value> void onGetValue(EnvironmentAttribute<Value> attribute, float partialTick,
                                    CallbackInfoReturnable<Value> cir) {
<<<<<<< HEAD
        if (!CloudColor.maybeEnabled()) return;
        if (attribute == EnvironmentAttributes.CLOUD_COLOR) {
            cir.setReturnValue((Value) Integer.valueOf(CloudColor.itz().cloudColor.getValue()));
=======
        if (!CloudColor.INSTANCE.getEnabled()) return;
        if (attribute == EnvironmentAttributes.CLOUD_COLOR) {
            cir.setReturnValue((Value) Integer.valueOf(CloudColor.INSTANCE.cloudColor.getValue()));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
}
