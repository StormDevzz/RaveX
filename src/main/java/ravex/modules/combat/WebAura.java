package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.List;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "WebAura", category = "Combat")
public class WebAura extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Positive", "Custom"));
    public final NumberParameter customRange = new NumberParameter("CustomRange", 4.0, 2.0, 6.0, 0.1);
    private int delay = 0;
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
                p.connection.send(new ServerboundSetCarriedItemPacket(webSlot));
            }
            BlockHitResult hit = new BlockHitResult(
                new net.minecraft.world.phys.Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                net.minecraft.core.Direction.UP,
                targetPos,
                false
            );
            p.connection.send(new ServerboundUseItemOnPacket(net.minecraft.world.InteractionHand.MAIN_HAND, hit, 0));
            p.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
            if (webSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
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