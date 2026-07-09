package ravex.mixin.realms;

import com.mojang.realmsclient.client.RealmsClient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RealmsClient.class)
public class MixinRealmsClient {
    @Redirect(
        method = "fetchFeatureFlags",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"),
        remap = false
    )
    private void suppressRealmsError(Logger logger, String message, Throwable t) {}

    @Redirect(
        method = "execute(Lcom/mojang/realmsclient/client/Request;)Ljava/lang/String;",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"),
        remap = false
    )
    private void suppressRealmsExecuteInfo(Logger logger, String message, Object arg) {}
}
