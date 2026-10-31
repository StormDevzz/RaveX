package ravex.mixin.realms;

import com.mojang.realmsclient.client.RealmsClient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// silences the realms client log spam
// the old intermediary names do not exist under the mojang mappings
// remap false belongs on the injector, not on the redirect
@Mixin(RealmsClient.class)
public class MixinRealmsClient {
    // both of the Logger.error sites inside fetchFeatureFlags() are redirected.
    @Redirect(
        method = "fetchFeatureFlags",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V",
            remap = false
        )
    )
    private void suppressRealmsError(Logger logger, String message, Throwable t) {}

    @Redirect(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
        )
    )
    private void suppressRealmsExecuteInfo(Logger logger, String message, Object arg) {}
}
