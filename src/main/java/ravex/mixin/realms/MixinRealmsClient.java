package ravex.mixin.realms;

import com.mojang.realmsclient.client.RealmsClient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Silences the Realms client log spam.
 *
 * <p>{@code RealmsClient} pings the Realms backend as soon as the game starts and logs both a
 * feature-flag failure and an "executing" line for every request whenever the user is offline,
 * signed out, or on a launcher without Realms access. That output lands in the game log even
 * though it never affects gameplay.
 *
 * <p>Naming notes, because both details matter here:
 * <ul>
 *   <li>This used to target intermediary names ({@code net.minecraft.class_4341},
 *       {@code method_68466}, {@code method_20998}), which do not exist under the Mojang
 *       mappings this project compiles against. Mixin only warned and applied nothing.</li>
 *   <li>{@code remap = false} is set on {@link At}, not on {@link Redirect}. Putting it on the
 *       injector would also stop the method selector from being remapped, no refmap entry would
 *       be generated, and the mixin would break in a production (intermediary) runtime.</li>
 * </ul>
 */
@Mixin(RealmsClient.class)
public class MixinRealmsClient {
    /** Both of the {@code Logger.error} sites inside {@code fetchFeatureFlags()} are redirected. */
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
