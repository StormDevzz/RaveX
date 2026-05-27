package ravex.mixin.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Glint module hook — marks when an item is being rendered for future use.
 * All static fields/methods must be private in mixins.
 */
@Mixin(GuiGraphics.class)
public class MixinItemRenderer {

    @Inject(
        method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("HEAD"),
        require = 0
    )
    private void onRenderItemHead(ItemStack stack, int x, int y, CallbackInfo ci) {
        // Hook point for Glint module — no-op until render API is confirmed
    }
}
