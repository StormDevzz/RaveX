package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoMount", category = "World")
public class AutoMount {
    @Parameter(name = "Mode", modes = {"Normal", "Fast"})
    public String mode = "Normal";
    private int cooldown = 0;
    public void onTick() {
        if ("Normal".equals(mode) && cooldown > 0) {
            cooldown--;
            return;
        }
        var mc = MinecraftWrapper.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (p.getVehicle() != null) {
            return;
        }
        var target = (net.minecraft.world.entity.Entity) null;
        double closestDist = 4.5;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity.isAlive() && entity != p) {
                if (MobUtility.isMountable(entity) && !MobUtility.isVehicle(entity)) {
                    double dist = p.distanceTo(entity);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = entity;
                    }
                }
            }
        }
        if (target != null) {
            MobUtility.interact(ravex.mcwrapper.MinecraftWrapper.getWrapper(), target);
            SwingUtility.swingMainHand(p);
            cooldown = 20;
        }
    }



}