package ravex.mixin.render;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.WorldColor;

@Mixin(EnvironmentAttributeProbe.class)
public class MixinCloudColor {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private <Value> void onGetValue(EnvironmentAttribute<Value> attribute, float partialTick,
                                    CallbackInfoReturnable<Value> cir) {
        if (!WorldColor.maybeEnabled() || !WorldColor.itz().cloud.getValue()) return;
        if (attribute == EnvironmentAttributes.CLOUD_COLOR) {
            cir.setReturnValue((Value) Integer.valueOf(WorldColor.itz().cloudColor.getValue()));
        }
    }
}
