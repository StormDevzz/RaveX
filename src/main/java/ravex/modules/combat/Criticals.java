package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Criticals", category = "Combat")
public class Criticals {
    @Parameter(name = "Mode", modes = {"Legit", "Packet", "Grim", "MiniJump", "Watchdog"})
    public String mode = "Packet";
    @Parameter(name = "AutoAttack")
    public boolean autoAttack = true;
    @Parameter(name = "StopOnWater")
    public boolean stopOnWater = true;
    @Parameter(name = "PauseAura")
    public boolean pauseAura = false;
    private enum Sequence { NONE, JUMPING, LANDING }
    private Sequence seq = Sequence.NONE;
    private int seqTicks = 0;
    public void onDisable() {
        seq = Sequence.NONE;
        seqTicks = 0;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (stopOnWater && (player.isInWater() || player.isInLava())) {
            seq = Sequence.NONE;
            return;
        }
        if (seq == Sequence.JUMPING) {
            seqTicks++;
            if (seqTicks > 2) {
                seq = Sequence.LANDING;
            }
            return;
        }
        if (seq == Sequence.LANDING) {
            if (autoAttack && mc.getHitResult() instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                net.minecraft.world.entity.Entity target = ehr.getEntity();
                net.minecraft.world.entity.LivingEntity lt = EntityUtility.asLivingEntity(target);
                if (lt != null && EntityUtility.isAlive(lt) && target != player
                    && player.getAttackStrengthScale(0.0f) >= 0.85f) {
                    NetworkUtility.sendInteractAttack(target, PlayerUtility.isSneaking(player));
                    SwingUtility.swing(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            seq = Sequence.NONE;
            seqTicks = 0;
            return;
        }
        if (!PlayerUtility.isOnGround(player)) return;
        if (MoveUtility.horizontalCollision()) return;
        boolean wantAttack = mc.getOptions().keyAttack.isDown()
            || (mc.getHitResult() instanceof net.minecraft.world.phys.EntityHitResult && autoAttack);
        if (!wantAttack) return;
        if (player.getAttackStrengthScale(0.0f) < 0.85f) return;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String m = mode;
        switch (m) {
            case "Legit" -> {
                player.jumpFromGround();
                seq = Sequence.JUMPING;
                seqTicks = 0;
            }
            case "Packet" -> {
                NetworkUtility.sendMoveRelative(x, y + 0.0625, z, false, false);
                NetworkUtility.sendMoveRelative(x, y, z, false, false);
                seq = Sequence.LANDING;
            }
            case "Grim" -> {
                NetworkUtility.sendMoveRelative(x, y + 0.001, z, false, false);
                NetworkUtility.sendMoveRelative(x, y, z, false, false);
                NetworkUtility.sendMoveRelative(x, y - 0.001, z, false, false);
                NetworkUtility.sendMoveRelative(x, y - 0.0625, z, false, false);
                seq = Sequence.LANDING;
            }
            case "MiniJump" -> {
                NetworkUtility.sendMoveRelative(x, y + 0.02, z, false, false);
                NetworkUtility.sendMoveRelative(x, y - 0.02, z, false, false);
                NetworkUtility.sendMoveRelative(x, y + 0.001, z, false, false);
                NetworkUtility.sendMoveRelative(x, y - 0.0625, z, false, false);
                seq = Sequence.LANDING;
            }
            case "Watchdog" -> {
                NetworkUtility.sendMoveRelative(x, y + 0.0001, z, false, false);
                NetworkUtility.sendMoveRelative(x, y + 0.0001, z, false, false);
                NetworkUtility.sendMoveRelative(x, y - 0.1, z, false, false);
                seq = Sequence.LANDING;
            }
        }
    }
}
