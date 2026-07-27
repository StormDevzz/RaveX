package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import java.util.List;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;



@ModuleInfo(name = "WebAura", category = "Combat")
public class WebAura implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Normal", "Positive", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "CustomRange", min = 2.0, max = 6.0, step = 0.1)
    public double customRange = 4.0;
    private int delay = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (delay > 0) {
            delay--;
            return;
        }
        double range = 4.5;
        String m = mode;
        if (m.equals("Custom")) {
            range = customRange;
        }
        net.minecraft.world.entity.player.Player target = null;
        double closest = range;
        for (net.minecraft.world.entity.player.Player player : mc.level.players()) {
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
        net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(target.getX(), target.getY(), target.getZ());
        if (mc.level.getBlockState(targetPos).isAir()) {
            int prevSlot = InventoryUtility.getSelectedSlot(p);
            if (webSlot != prevSlot) {
                NetworkUtility.sendSetCarriedItem(webSlot);
            }
            net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                net.minecraft.core.Direction.UP,
                targetPos,
                false
            );
            p.connection.send(new ServerboundUseItemOnPacket(net.minecraft.world.InteractionHand.MAIN_HAND, hit, 0));
            p.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
            if (webSlot != prevSlot) {
                NetworkUtility.sendSetCarriedItem(prevSlot);
            }
            delay = 4;
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("WebAura").getEnabled();
    }
    public static WebAura itz() {
        return ravex.manager.ModuleManager.delegate(WebAura.class);
    }


}