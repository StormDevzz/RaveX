package ravex.mixin.player;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AccessorContainerScreen {
    @Accessor("topPos")
    int getTopPos();

    @Accessor("leftPos")
    int getLeftPos();

    @Accessor("imageWidth")
    int getImageWidth();

    @Accessor("imageHeight")
    int getImageHeight();
}
