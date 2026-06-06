package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AccessorMinecraft {
    @Accessor("rightClickDelay")
    int getRightClickDelay();

    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);

    @org.spongepowered.asm.mixin.gen.Invoker("startUseItem")
    void invokeStartUseItem();
}
