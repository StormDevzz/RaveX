package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "MaceSwap", category = "Combat")
public class MaceSwap extends ravex.modules.Module {
public final ModeParameter mode       = new ModeParameter("Mode", "Smart",
            List.of("Basic", "Smart"));
    public final NumberParameter fallSpeed = new NumberParameter("FallSpeed", 0.5, 0.1, 3.0, 0.05);
    private int previousSlot = -1;
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
                InventoryUtility.selectSlot(mc.player, previousSlot);
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("MaceSwap").getEnabled();
    }
    public static MaceSwap itz() {
        return ravex.manager.ModuleManager.delegate(MaceSwap.class);
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