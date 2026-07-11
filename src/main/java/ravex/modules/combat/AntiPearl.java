package ravex.modules.combat;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.ArrayList;
import java.util.List;
public class AntiPearl extends Module {
    public final NumberParameter range = new NumberParameter("Range", 8.0, 1.0, 16.0, 0.5);
    public final BooleanParameter autoAttack = new BooleanParameter("AutoAttack", true);
    public final BooleanParameter autoWarn = new BooleanParameter("Warn", true);
    public final BooleanParameter predict = new BooleanParameter("Predict", true);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_antipearl");
    static {
        NATIVE.load();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double r = range.getValue();
        List<ThrownEnderpearl> pearls = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ThrownEnderpearl pearl) {
                double dist = mc.player.distanceTo(pearl);
                if (dist <= r) pearls.add(pearl);
            }
        }
        if (pearls.isEmpty()) return;
        for (ThrownEnderpearl pearl : pearls) {
            Vec3 pos = pearl.position();
            Vec3 vel = pearl.getDeltaMovement();
            if (predict.getValue() && NATIVE.isLoaded()) {
                double[] result = new double[6];
                nativePredictLanding(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, result);
                Vec3 landing = new Vec3(result[0], result[1], result[2]);
                double distToMe = landing.distanceTo(mc.player.position());
                double impactTicks = result[3];
                if (autoWarn.getValue() && distToMe < 3.0) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§7[§cRaveX§7] §ePearl incoming! §f" + String.format("%.1f", distToMe) + "mAway"
                        ), true
                    );
                }
            } else if (predict.getValue()) {
                Vec3 landing = pearlPosAtTicks(pos, vel, 30);
                double distToMe = landing.distanceTo(mc.player.position());
                if (autoWarn.getValue() && distToMe < 3.0) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§7[§cRaveX§7] §ePearl incoming!"
                        ), true
                    );
                }
            }
        }
    }
    private Vec3 pearlPosAtTicks(Vec3 pos, Vec3 vel, int ticks) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        double mx = vel.x;
        double my = vel.y;
        double mz = vel.z;
        for (int t = 0; t < ticks; t++) {
            x += mx;
            y += my;
            z += mz;
            my -= 0.03;
            mx *= 0.99;
            my *= 0.99;
            mz *= 0.99;
            if (y < -64) break;
        }
        return new Vec3(x, y, z);
    }
    private static native void nativePredictLanding(double x, double y, double z, double mx, double my, double mz, double[] out);
    public static boolean maybeEnabled() {
        return maybeEnabled(AntiPearl.class);
    }
    public static AntiPearl itz() {
        return ModuleManager.get(AntiPearl.class);
    }

}