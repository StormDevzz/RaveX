package ravex.mixin.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import ravex.gui.tooltip.BookDataTooltipComponent;
import ravex.gui.tooltip.BundleDataTooltipComponent;
import ravex.gui.tooltip.MapDataTooltipComponent;
import ravex.gui.tooltip.ShulkerDataTooltipComponent;
import ravex.modules.Modules;
import ravex.modules.render.ToolTips;

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

        ToolTips toolTips = Modules.get(ToolTips.class);
        if (toolTips == null) return;

        List<Component> original = cir.getReturnValue();
        List<Component> modified = new ArrayList<>(original);

        boolean isShulker = toolTips.isShulker(self);
        boolean isContainer = false;
        boolean hideContainerLines = (isShulker && toolTips.shulkerRegear);
        boolean isBook = toolTips.isBook(self);
        boolean hideBookLines = (isBook && toolTips.showBooks);

        if (hideContainerLines) {
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

        if (hideBookLines) {
            modified.clear();
        }

        if (isShulker && toolTips.shulkerRegear) {
            modified.clear();
            modified.add(Component.literal("§7" + self.getHoverName().getString()));
        }

        if (toolTips.showId && !modified.isEmpty() && !hideContainerLines && !hideBookLines && !(isShulker && toolTips.shulkerRegear)) {
            String id = toolTips.idSuffix(self);
            modified.set(0, Component.literal("").append(modified.get(0)).append(Component.literal(" §8" + id)));
        }

        cir.setReturnValue(modified);
    }

    @Inject(method = "getTooltipImage()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void onGetTooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!Modules.enabled(ToolTips.class)) return;
        ToolTips toolTips = Modules.get(ToolTips.class);
        if (toolTips == null) return;

        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;

        if (toolTips.isMap(self) && toolTips.showMaps) {
            cir.setReturnValue(Optional.of(new MapDataTooltipComponent(self)));
            return;
        }

        if (toolTips.isBook(self) && toolTips.showBooks) {
            cir.setReturnValue(Optional.of(new BookDataTooltipComponent(self)));
            return;
        }

        if (toolTips.isBundle(self)) {
            cir.setReturnValue(Optional.of(new BundleDataTooltipComponent(self)));
            return;
        }

        if (self.has(DataComponents.CONTAINER)) {
            boolean isShulker = toolTips.isShulker(self);
            if ((isShulker && toolTips.shulkerRegear) || (!isShulker && toolTips.showStorage)) {
                ItemContainerContents contents = self.get(DataComponents.CONTAINER);
                if (contents != null && !contents.stream().toList().isEmpty()) {
                    int tint = isShulker ? ToolTips.getBoxColor(self) : 0xFF7D8C99;
                    cir.setReturnValue(Optional.of(new ShulkerDataTooltipComponent(contents.stream().toList(), tint)));
                }
            }
        }
    }
}
