package ravex.mixin.render;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.Modules;
import ravex.modules.render.ToolTips;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinCreativeModeInventoryScreen {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void onGetTooltipFromContainerItem(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (!Modules.enabled(ToolTips.class)) return;
        ToolTips tips = Modules.get(ToolTips.class);
        if (!tips.showShulker || !tips.isShulker(stack)) return;

        List<Component> original = cir.getReturnValue();
        Set<String> tabNames = new HashSet<>();
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            tabNames.add(tab.getDisplayName().getString());
        }

        List<Component> filtered = new ArrayList<>(original.size());
        for (Component line : original) {
            if (!tabNames.contains(line.getString())) {
                filtered.add(line);
            }
        }
        if (filtered.size() != original.size()) {
            cir.setReturnValue(filtered);
        }
    }
}
