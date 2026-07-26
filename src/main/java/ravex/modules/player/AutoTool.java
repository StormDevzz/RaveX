package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
@ModuleInfo(name = "AutoTool", category = "net.minecraft.world.entity.player.Player")
public class AutoTool extends ravex.modules.Module {
public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(mc.player, mc.level.getBlockState(pos));
        if (slot < 0) return;
        if ("Silent".equals(swap.getValue()))
            InventoryUtility.silentSelectSlot(mc.player, slot);
        else
            InventoryUtility.selectSlot(mc.player, slot);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoTool").getEnabled();
    }
    public static AutoTool itz() {
        return ravex.manager.ModuleManager.delegate(AutoTool.class);
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