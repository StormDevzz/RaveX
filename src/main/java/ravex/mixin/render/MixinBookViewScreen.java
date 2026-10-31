package ravex.mixin.render;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.gui.book.EditWrittenBookScreen;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.player.InventoryUtility;

// book screen button opens the title/author editor
@Mixin(BookViewScreen.class)
public abstract class MixinBookViewScreen extends Screen {
    protected MixinBookViewScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ravexEditBookButton(CallbackInfo ci) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        int slot = InventoryUtility.getSelectedSlot(player);
        if (!stack.is(Items.WRITTEN_BOOK)) {
            stack = player.getOffhandItem();
            slot = 40;
            if (!stack.is(Items.WRITTEN_BOOK)) return;
        }
        ItemStack book = stack;
        int bookSlot = slot;
        addRenderableWidget(Button.builder(
                Component.literal(LanguageUtility.t("book_edit_button")),
                b -> mc.setScreen(new EditWrittenBookScreen(book, bookSlot)))
            .pos(4, 4).size(170, 20).build());
    }
}
