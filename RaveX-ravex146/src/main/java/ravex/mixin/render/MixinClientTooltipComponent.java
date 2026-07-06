package ravex.mixin.render;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.gui.tooltip.ShulkerDataTooltipComponent;
import ravex.gui.tooltip.ShulkerTooltipComponent;

@Mixin(ClientTooltipComponent.class)
public interface MixinClientTooltipComponent {

    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
            at = @At("HEAD"), cancellable = true)
    private static void onCreateComponent(TooltipComponent tooltipComponent, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (tooltipComponent instanceof ShulkerDataTooltipComponent shulkerData) {
            cir.setReturnValue(new ShulkerTooltipComponent(shulkerData.getItems()));
        }
    }
}
