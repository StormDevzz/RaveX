package ravex.mixin.render;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas {
    @Redirect(
        method = "createTexture(III)V",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false)
    )
    private void suppressCreatedLog(Logger logger, String msg, Object[] args) {}
}
