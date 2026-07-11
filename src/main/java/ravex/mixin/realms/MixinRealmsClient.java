package ravex.mixin.realms;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.class_4341")
public class MixinRealmsClient {
    @Redirect(
        method = "method_68466",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"),
        remap = false
    )
    private void suppressRealmsError(Logger logger, String message, Throwable t) {}

    @Redirect(
        method = "method_20998",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"),
        remap = false
    )
    private void suppressRealmsExecuteInfo(Logger logger, String message, Object arg) {}
}
