package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;




@ModuleInfo(name = "MaceAura", category = "Combat")
public class MaceAura implements ModuleAccess {
    @Parameter(name = "Height", min = 2.0, max = 40.0, step = 1.0)
    public double height = 10.0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (InventoryUtility.isItem(p.getMainHandItem(), "mace") && mc.options.keyAttack.isDown()) {
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) {
                net.minecraft.world.entity.LivingEntity target = MobUtility.asLivingEntity(hit.getEntity());
                if (target == null) return;
                if (MobUtility.isDead(target) || MobUtility.isSelf(target)) return;
                double h = height;
                double x = p.getX();
                double y = p.getY();
                double z = p.getZ();
                double step = 0.25;
                int loops = (int) Math.ceil(h / step);
                for (int i = 0; i < loops; i++) {
                    p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y + 0.001, z, false, p.horizontalCollision));
                    p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y - step, z, false, p.horizontalCollision));
                    p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y, z, false, p.horizontalCollision));
                }
                p.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(target, p.isShiftKeyDown()));
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y, z, true, p.horizontalCollision));
                p.fallDistance = (float) h;
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MaceAura").getEnabled();
    }
    public static MaceAura itz() {
        return ravex.manager.ModuleManager.delegate(MaceAura.class);
    }


}