package ravex.mixin.client;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    
    @Overwrite(remap = false)
    public static String getClientModName() {
        return "fabric";
    }
}
