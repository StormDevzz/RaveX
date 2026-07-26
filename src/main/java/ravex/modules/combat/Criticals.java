package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.EntityUtility;

import net.minecraft.world.phys.EntityHitResult;
import ravex.utility.misc.MobUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
@ModuleInfo(name = "Criticals", category = "Combat")
public class Criticals extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Packet",
        java.util.List.of("Legit", "Packet", "Grim", "MiniJump", "Watchdog"));
    public final BooleanParameter autoAttack = new BooleanParameter("AutoAttack", true);
    public final BooleanParameter stopOnWater = new BooleanParameter("StopOnWater", true);
    public final BooleanParameter pauseAura = new BooleanParameter("PauseAura", false);
    private enum Sequence { NONE, JUMPING, LANDING }
    private Sequence seq = Sequence.NONE;
    private int seqTicks = 0;
    protected void onDisable() {
        seq = Sequence.NONE;
        seqTicks = 0;
    }
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
                net.minecraft.world.entity.Entity target = ehr.getEntity();
                net.minecraft.world.entity.LivingEntity lt = MobUtility.asLivingEntity(target);
                if (lt != null && MobUtility.isAlive(lt) && target != mc.player
                    && mc.player.getAttackStrengthScale(0.0f) >= 0.85f) {
                    mc.player.connection.send(
                        ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Criticals").getEnabled();
    }
    public static Criticals itz() {
        return ravex.manager.ModuleManager.delegate(Criticals.class);
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