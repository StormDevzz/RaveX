package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
<<<<<<< HEAD
import ravex.utility.misc.MobUtility;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
public class Criticals extends Module {
<<<<<<< HEAD
=======
    public static final Criticals INSTANCE = new Criticals();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Packet",
        java.util.List.of("Legit", "Packet", "Grim", "MiniJump", "Watchdog"));
    public final BooleanParameter autoAttack = new BooleanParameter("AutoAttack", true);
    public final BooleanParameter stopOnWater = new BooleanParameter("StopOnWater", true);
    public final BooleanParameter pauseAura = new BooleanParameter("PauseAura", false);
    private enum Sequence { NONE, JUMPING, LANDING }
    private Sequence seq = Sequence.NONE;
    private int seqTicks = 0;

    @Override
    protected void onDisable() {
        seq = Sequence.NONE;
        seqTicks = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (stopOnWater.getValue() && (mc.player.isInWater() || mc.player.isInLava())) {
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
            if (autoAttack.getValue() && mc.hitResult instanceof EntityHitResult ehr) {
                Entity target = ehr.getEntity();
<<<<<<< HEAD
                LivingEntity lt = MobUtility.asLivingEntity(target);
                if (lt != null && MobUtility.isAlive(lt) && target != mc.player
=======
                if (target instanceof LivingEntity lt && lt.isAlive() && target != mc.player
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    && mc.player.getAttackStrengthScale(0.0f) >= 0.85f) {
                    mc.player.connection.send(
                        ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
            seq = Sequence.NONE;
            seqTicks = 0;
            return;
        }
        if (!mc.player.onGround()) return;
        if (mc.player.horizontalCollision) return;
        boolean wantAttack = mc.options.keyAttack.isDown()
            || (mc.hitResult instanceof EntityHitResult && autoAttack.getValue());
        if (!wantAttack) return;
        if (mc.player.getAttackStrengthScale(0.0f) < 0.85f) return;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        String m = mode.getValue();
        switch (m) {
            case "Legit" -> {
                mc.player.jumpFromGround();
                seq = Sequence.JUMPING;
                seqTicks = 0;
            }
            case "Packet" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0625, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y, z, false, false));
                seq = Sequence.LANDING;
            }
            case "Grim" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.001, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.001, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.0625, z, false, false));
                seq = Sequence.LANDING;
            }
            case "MiniJump" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.02, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.02, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.001, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.0625, z, false, false));
                seq = Sequence.LANDING;
            }
            case "Watchdog" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0001, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0001, z, false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.1, z, false, false));
                seq = Sequence.LANDING;
            }
        }
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Criticals.class);
    }
    public static Criticals itz() {
        return ModuleManager.get(Criticals.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
