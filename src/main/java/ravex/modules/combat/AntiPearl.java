package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibraryUtility;
import java.util.ArrayList;
import java.util.List;
@ModuleInfo(name = "AntiPearl", category = "Combat")
public class AntiPearl extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 8.0, 1.0, 16.0, 0.5);
    public final BooleanParameter autoAttack = new BooleanParameter("AutoAttack", true);
    public final BooleanParameter autoWarn = new BooleanParameter("Warn", true);
    public final BooleanParameter predict = new BooleanParameter("Predict", true);
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_antipearl");
    static {
        NATIVE.load();
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double r = range.getValue();
        List<ThrownEnderpearl> pearls = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ThrownEnderpearl pearl) {
                double dist = mc.player.distanceTo(pearl);
                if (dist <= r) pearls.add(pearl);
            }
        }
        if (pearls.isEmpty()) return;
        for (ThrownEnderpearl pearl : pearls) {
            net.minecraft.world.phys.Vec3 pos = pearl.position();
            net.minecraft.world.phys.Vec3 vel = pearl.getDeltaMovement();
            if (predict.getValue() && NATIVE.isLoaded()) {
                double[] result = new double[6];
                nativePredictLanding(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, result);
                net.minecraft.world.phys.Vec3 landing = new net.minecraft.world.phys.Vec3(result[0], result[1], result[2]);
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
                net.minecraft.world.phys.Vec3 landing = pearlPosAtTicks(pos, vel, 30);
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
    private net.minecraft.world.phys.Vec3 pearlPosAtTicks(net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 vel, int ticks) {
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
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }
    private static native void nativePredictLanding(double x, double y, double z, double mx, double my, double mz, double[] out);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AntiPearl").getEnabled();
    }
    public static AntiPearl itz() {
        return ravex.manager.ModuleManager.delegate(AntiPearl.class);
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