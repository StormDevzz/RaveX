package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.SwingUtility;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (stopOnWater && (mc.player.isInWater() || mc.player.isInLava())) {
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
            if (autoAttack && mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                net.minecraft.world.entity.Entity target = ehr.getEntity();
                net.minecraft.world.entity.LivingEntity lt = MobUtility.asLivingEntity(target);
                if (lt != null && MobUtility.isAlive(lt) && target != mc.player
                    && mc.player.getAttackStrengthScale(0.0f) >= 0.85f) {
                    mc.player.connection.send(
                        net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
                    SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            seq = Sequence.NONE;
            seqTicks = 0;
            return;
        }
        if (!mc.player.onGround()) return;
        if (mc.player.horizontalCollision) return;
        boolean wantAttack = mc.options.keyAttack.isDown()
            || (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult && autoAttack);
        if (!wantAttack) return;
        if (mc.player.getAttackStrengthScale(0.0f) < 0.85f) return;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        String m = mode;
        switch (m) {
            case "Legit" -> {
                mc.player.jumpFromGround();
                seq = Sequence.JUMPING;
                seqTicks = 0;
            }
            case "Packet" -> {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0625, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y, z, false, false));
                seq = Sequence.LANDING;
            }
            case "Grim" -> {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.001, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y - 0.001, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y - 0.0625, z, false, false));
                seq = Sequence.LANDING;
            }
            case "MiniJump" -> {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.02, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y - 0.02, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.001, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y - 0.0625, z, false, false));
                seq = Sequence.LANDING;
            }
            case "Watchdog" -> {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0001, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y + 0.0001, z, false, false));
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    x, y - 0.1, z, false, false));
                seq = Sequence.LANDING;
            }
        }
    }




}