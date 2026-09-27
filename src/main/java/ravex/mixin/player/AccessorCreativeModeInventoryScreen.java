package ravex.mixin.player;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeInventoryScreen.class)
public interface AccessorCreativeModeInventoryScreen {
    @Accessor("CONTAINER")
    static SimpleContainer getContainer() {
        throw new AssertionError();
    }
}
