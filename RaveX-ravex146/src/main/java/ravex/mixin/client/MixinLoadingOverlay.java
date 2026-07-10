package ravex.mixin.client;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/IntSupplier;getAsInt()I"
        )
    )
    private int redirectBrandBackground(IntSupplier instance) {
        long time = net.minecraft.util.Util.getMillis();

        double wave = Math.sin(time * 0.003) * 0.5 + 0.5;

        int r1 = 0xEF, g1 = 0x32, b1 = 0x3D;
        int r2 = 0x1A, g2 = 0x3A, b2 = 0x7A;

        int r = (int) (r1 + (r2 - r1) * wave);
        int g = (int) (g1 + (g2 - g1) * wave);
        int b = (int) (b1 + (b2 - b1) * wave);

        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
}
