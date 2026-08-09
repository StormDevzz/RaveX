package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "MaceAura", category = "Combat")
public class MaceAura {
    @Parameter(name = "Height", min = 2.0, max = 40.0, step = 1.0)
    public double height = 10.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        if (InventoryUtility.isItem(p.getMainHandItem(), "mace") && mc.getOptions().keyAttack.isDown()) {
            if (mc.getHitResult() instanceof net.minecraft.world.phys.EntityHitResult hit) {
                net.minecraft.world.entity.LivingEntity target = EntityUtility.asLivingEntity(hit.getEntity());
                if (target == null) return;
                if (EntityUtility.isDead(target) || EntityUtility.isSelf(target)) return;
                double h = height;
                double x = p.getX();
                double y = p.getY();
                double z = p.getZ();
                double step = 0.25;
                int loops = (int) Math.ceil(h / step);
                for (int i = 0; i < loops; i++) {
                    NetworkUtility.sendMoveRelative(x, y + 0.001, z, false, p.horizontalCollision);
                    NetworkUtility.sendMoveRelative(x, y - step, z, false, p.horizontalCollision);
                    NetworkUtility.sendMoveRelative(x, y, z, false, p.horizontalCollision);
                }
                NetworkUtility.sendInteractAttack(target, PlayerUtility.isSneaking(p));
                SwingUtility.swing(p, net.minecraft.world.InteractionHand.MAIN_HAND);
                NetworkUtility.sendMoveRelative(x, y, z, true, p.horizontalCollision);
                p.fallDistance = (float) h;
            }
        }
    }




}