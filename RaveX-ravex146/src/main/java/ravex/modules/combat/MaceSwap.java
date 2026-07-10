package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.util.List;


public class MaceSwap extends Module {
    public static final MaceSwap INSTANCE = new MaceSwap();

    public final ModeParameter mode       = new ModeParameter("Mode", "Smart",
            List.of("Basic", "Smart"));
    public final NumberParameter fallSpeed = new NumberParameter("Fall Speed", 0.5, 0.1, 3.0, 0.05);


    private int previousSlot = -1;

    private MaceSwap() {
        super("MaceSwap", Category.COMBAT);
        addParameter(mode);
        addParameter(fallSpeed);
    }

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

                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        } else {

            if (falling) {
                swapToMace(mc);
            } else if (!falling && previousSlot != -1 && mc.player.onGround()) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        }
    }

    private void swapToMace(Minecraft mc) {
        int maceSlot = findMaceInHotbar(mc);
        if (maceSlot != -1) {
            int current = mc.player.getInventory().getSelectedSlot();
            if (current != maceSlot) {
                if (previousSlot == -1) previousSlot = current;
                mc.player.getInventory().setSelectedSlot(maceSlot);
            }
        }
    }

    private int findMaceInHotbar(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE) return i;
        }
        return -1;
    }
}
