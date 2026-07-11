package ravex.mixin.client;

import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.utility.sound.SoundUtility;

@Mixin(BuiltInRegistries.class)
public class MixinBuiltInRegistries {
    @Inject(method = "freeze", at = @At("HEAD"))
    private static void beforeFreeze(CallbackInfo ci) {
        SoundUtility.register();
    }
}
