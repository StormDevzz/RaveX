package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
import ravex.utility.player.InventoryUtility;
public class RocketUtils extends Module {
    public static final RocketUtils INSTANCE = new RocketUtils();
    public final ModeParameter mode = new ModeParameter("Mode", "Auto", List.of("Auto", "Boost"));
    public final NumberParameter speed = new NumberParameter("Speed", 0.3, 0.05, 2.0, 0.05);
    public final NumberParameter delay = new NumberParameter("Delay", 25.0, 5.0, 60.0, 1.0);
    private int timer = 0;

    @Override
    protected void onEnable() {
        timer = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isFallFlying()) return;
        double accel = speed.getValue();
        Vec3 look = mc.player.getLookAngle();
        Vec3 motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(
            motion.x + look.x * accel,
            motion.y + Math.abs(look.y) * accel * 0.5,
            motion.z + look.z * accel
        );
        if (!mode.getValue().equals("Boost")) {
            timer++;
            if (timer >= delay.getValue().intValue()) {
                useFirework(mc);
                timer = 0;
            }
        }
    }
    private void useFirework(Minecraft mc) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(InventoryUtility.getItem(mc.player, i), "firework_rocket")) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        InventoryUtility.selectSlot(mc.player, slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }
}
