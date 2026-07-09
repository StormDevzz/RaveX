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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.List;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
public class WebAura extends Module {
=======
public class WebAura extends Module {
    public static final WebAura INSTANCE = new WebAura();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Positive", "Custom"));
    public final NumberParameter customRange = new NumberParameter("CustomRange", 4.0, 2.0, 6.0, 0.1);
    private int delay = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (delay > 0) {
            delay--;
            return;
        }
        double range = 4.5;
        String m = mode.getValue();
        if (m.equals("Custom")) {
            range = customRange.getValue();
        }
        Player target = null;
        double closest = range;
        for (Player player : mc.level.players()) {
            if (player == p || !player.isAlive()) continue;
            double dist = p.distanceTo(player);
            if (dist < closest) {
                if (m.equals("Positive")) {
                    boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.002;
                    if (!player.onGround() || !isMoving) {
                        continue;
                    }
                }
                closest = dist;
                target = player;
            }
        }
        if (target == null) return;
        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (InventoryUtility.isItem(stack, "cobweb")) {
                webSlot = i;
                break;
            }
        }
        if (webSlot == -1) return;
        BlockPos targetPos = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        if (mc.level.getBlockState(targetPos).isAir()) {
<<<<<<< HEAD
            int prevSlot = InventoryUtility.getSelectedSlot(p);
=======
            int prevSlot = p.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (webSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(webSlot));
            }
            BlockHitResult hit = new BlockHitResult(
                new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                Direction.UP,
                targetPos,
                false
            );
            p.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
            p.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            if (webSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
            delay = 4; 
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(WebAura.class);
    }
    public static WebAura itz() {
        return ModuleManager.get(WebAura.class);
    }

}