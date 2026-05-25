package volthack.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Inject(
        method = "renderArmWithItem",
        at = @At("HEAD")
    )
    private void onRenderArmWithItem(
        AbstractClientPlayer player, float partialTicks, float pitch,
        InteractionHand hand, float swingProgress, ItemStack itemStack,
        float equipProgress, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int combinedLight, CallbackInfo ci
    ) {
        if (volthack.modules.render.ViewModel.INSTANCE.getEnabled()) {
            volthack.modules.render.ViewModel.INSTANCE.transform(poseStack, hand);
        }
    }
}
