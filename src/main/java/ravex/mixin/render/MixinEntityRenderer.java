package ravex.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.NameTags;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (NameTags.INSTANCE.getEnabled() && entity instanceof LivingEntity) {
            cir.setReturnValue(false); // Hide standard nametags if Custom NameTags ESP is active!
        }
    }
}
