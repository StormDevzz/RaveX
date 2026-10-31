package ravex.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.NoRender;
import ravex.modules.Modules;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void ravex$hideEntities(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (!Modules.enabled(NoRender.class)) return;
        NoRender nr = Modules.get(NoRender.class);
        if (nr.armorStands && entity instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            cir.setReturnValue(false);
            return;
        }
        if (nr.frames && entity instanceof net.minecraft.world.entity.decoration.ItemFrame) {
            cir.setReturnValue(false);
        }
    }
}
