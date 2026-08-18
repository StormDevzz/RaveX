package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.nativelib.NativeLibraryUtility;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "AntiPearl", category = "Combat")
public class AntiPearl {
    @Parameter(name = "Range", min = 1.0, max = 16.0, step = 0.5)
    public double range = 8.0;
    @Parameter(name = "AutoAttack")
    public boolean autoAttack = true;
    @Parameter(name = "Warn")
    public boolean autoWarn = true;
    @Parameter(name = "Predict")
    public boolean predict = true;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_antipearl");
    static {
        NATIVE.load();
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        double r = range;
        List<ThrownEnderpearl> pearls = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (e instanceof ThrownEnderpearl pearl) {
                double dist = mc.getPlayer().distanceTo(pearl);
                if (dist <= r) pearls.add(pearl);
            }
        }
        if (pearls.isEmpty()) return;
        for (ThrownEnderpearl pearl : pearls) {
            net.minecraft.world.phys.Vec3 pos = pearl.position();
            net.minecraft.world.phys.Vec3 vel = pearl.getDeltaMovement();
            if (predict && NATIVE.isLoaded()) {
                double[] result = new double[6];
                nativePredictLanding(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, result);
                net.minecraft.world.phys.Vec3 landing = new net.minecraft.world.phys.Vec3(result[0], result[1], result[2]);
                double distToMe = landing.distanceTo(mc.getPlayer().position());
                double impactTicks = result[3];
                if (autoWarn && distToMe < 3.0) {
                    mc.getPlayer().displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§7[§cRaveX§7] §ePearl incoming! §f" + String.format("%.1f", distToMe) + "mAway"
                        ), true
                    );
                }
            } else if (predict) {
                net.minecraft.world.phys.Vec3 landing = pearlPosAtTicks(pos, vel, 30);
                double distToMe = landing.distanceTo(mc.getPlayer().position());
                if (autoWarn && distToMe < 3.0) {
                    mc.getPlayer().displayClientMessage(
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




}