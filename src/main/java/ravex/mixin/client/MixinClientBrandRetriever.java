package ravex.mixin.client;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    /**
     * @author Antigravity
     * @reason Custom client brand spoofing
     */
    @Overwrite(remap = false)
    public static String getClientModName() {
        return "fabric";
    }
}
