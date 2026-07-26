package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.mixin.client.AccessorMinecraft;
import ravex.mixin.client.AccessorLivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
@ModuleInfo(name = "NoDelay", category = "net.minecraft.world.entity.player.Player")
public class NoDelay extends ravex.modules.Module {
public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 4.0, 1.0);
    public final BooleanParameter blocks = new BooleanParameter("net.minecraft.world.level.block.Blocks", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter noJumpDelay = new BooleanParameter("NoJumpDelay", false);

    public NoDelay() {
        
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean isBlock = InventoryUtility.isHoldingBlock(p);
        if ((isBlock && blocks.getValue()) || (!isBlock && items.getValue())) {
            AccessorMinecraft accessor = (AccessorMinecraft) mc;
            int target = (int) delay.getValue().doubleValue();
            if (accessor.getRightClickDelay() > target)
                accessor.setRightClickDelay(target);
        }
        if (noJumpDelay.getValue()) {
            ((AccessorLivingEntity) p).setNoJumpDelay(0);
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoDelay").getEnabled();
    }
    public static NoDelay itz() {
        return ravex.manager.ModuleManager.delegate(NoDelay.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}