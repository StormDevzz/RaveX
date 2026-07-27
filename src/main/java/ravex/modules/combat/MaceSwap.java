package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import java.util.List;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "MaceSwap", category = "Combat")
public class MaceSwap implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Basic", "Smart"})
    public String mode = "Smart";
    @Parameter(name = "FallSpeed", min = 0.1, max = 3.0, step = 0.05)
    public double fallSpeed = 0.5;
    private int previousSlot = -1;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        double velY = mc.getPlayer().getDeltaMovement().y;
        boolean falling = velY < -fallSpeed && !mc.getPlayer().onGround();
        if ("Smart".equals(mode)) {
            boolean targetingEntity = mc.getCrosshairPickEntity() != null;
            if (falling && targetingEntity) {
                swapToMace(mc);
            } else if (!falling && previousSlot != -1 && mc.getPlayer().onGround()) {
                InventoryUtility.selectSlot(mc.getPlayer(), previousSlot);
                previousSlot = -1;
            }
        } else {
            if (falling) {
                swapToMace(mc);
            } else if (!falling && previousSlot != -1 && mc.getPlayer().onGround()) {
                InventoryUtility.selectSlot(mc.getPlayer(), previousSlot);
                previousSlot = -1;
            }
        }
    }
    private void swapToMace(MinecraftWrapper mc) {
        int maceSlot = findMaceInHotbar(mc);
        if (maceSlot != -1) {
            int current = InventoryUtility.getSelectedSlot(mc.getPlayer());
            if (current != maceSlot) {
                if (previousSlot == -1) previousSlot = current;
                InventoryUtility.selectSlot(mc.getPlayer(), maceSlot);
            }
        }
    }
    private int findMaceInHotbar(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isItem(stack, "mace")) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MaceSwap").getEnabled();
    }
    public static MaceSwap itz() {
        return ravex.manager.ModuleManager.delegate(MaceSwap.class);
    }


}