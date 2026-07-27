package ravex.mixin.render;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.gui.tooltip.ShulkerDataTooltipComponent;
import ravex.modules.render.ToolTips;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ravex.modules.Modules;

@Mixin(ItemStack.class)
public class MixinItemStack {

    private static final ThreadLocal<Boolean> IN_RECURSION = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private void onGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        if (!Modules.enabled(ToolTips.class)) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;
        if (IN_RECURSION.get()) return;

        List<Component> original = cir.getReturnValue();
        List<Component> modified = new ArrayList<>(original);

        if (Modules.get(ToolTips.class).showShulker && Modules.get(ToolTips.class).isShulker(self)) {
            IN_RECURSION.set(true);
            try {
                ItemStack copy = self.copy();
                copy.remove(DataComponents.CONTAINER);
                List<Component> cleanLines = copy.getTooltipLines(context, player, flag);
                modified = new ArrayList<>(cleanLines);
            } finally {
                IN_RECURSION.set(false);
            }
        }

        modified.addAll(Modules.get(ToolTips.class).getTooltip(self));
        cir.setReturnValue(modified);
    }

    @Inject(method = "getTooltipImage()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void onGetTooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!Modules.enabled(ToolTips.class) || !Modules.get(ToolTips.class).showShulker) return;
        ItemStack self = (ItemStack) (Object) this;
        if (Modules.get(ToolTips.class).isShulker(self)) {
            ItemContainerContents contents = self.get(DataComponents.CONTAINER);
            if (contents != null) {
                cir.setReturnValue(Optional.of(new ShulkerDataTooltipComponent(contents.stream().toList())));
            }
        }
    }
}
