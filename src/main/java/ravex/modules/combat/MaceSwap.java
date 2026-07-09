package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
public class MaceSwap extends Module {
=======
public class MaceSwap extends Module {
    public static final MaceSwap INSTANCE = new MaceSwap();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode       = new ModeParameter("Mode", "Smart",
            List.of("Basic", "Smart"));
    public final NumberParameter fallSpeed = new NumberParameter("FallSpeed", 0.5, 0.1, 3.0, 0.05);
    private int previousSlot = -1;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double velY = mc.player.getDeltaMovement().y;
        boolean falling = velY < -fallSpeed.getValue() && !mc.player.onGround();
        if ("Smart".equals(mode.getValue())) {
            boolean targetingEntity = mc.crosshairPickEntity != null;
            if (falling && targetingEntity) {
                swapToMace(mc);
            } else if (!falling && previousSlot != -1 && mc.player.onGround()) {
<<<<<<< HEAD
                InventoryUtility.selectSlot(mc.player, previousSlot);
=======
                mc.player.getInventory().setSelectedSlot(previousSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                previousSlot = -1;
            }
        } else {
            if (falling) {
                swapToMace(mc);
            } else if (!falling && previousSlot != -1 && mc.player.onGround()) {
                InventoryUtility.selectSlot(mc.player, previousSlot);
                previousSlot = -1;
            }
        }
    }
    private void swapToMace(Minecraft mc) {
        int maceSlot = findMaceInHotbar(mc);
        if (maceSlot != -1) {
            int current = InventoryUtility.getSelectedSlot(mc.player);
            if (current != maceSlot) {
                if (previousSlot == -1) previousSlot = current;
                InventoryUtility.selectSlot(mc.player, maceSlot);
            }
        }
    }
    private int findMaceInHotbar(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "mace")) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(MaceSwap.class);
    }
    public static MaceSwap itz() {
        return ModuleManager.get(MaceSwap.class);
    }

}