package ravex.mixin.player;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

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

    @Accessor("hoveredSlot")
    Slot getHoveredSlot();

    @Invoker("getHoveredSlot")
    Slot invokeGetHoveredSlot(double mouseX, double mouseY);
}
