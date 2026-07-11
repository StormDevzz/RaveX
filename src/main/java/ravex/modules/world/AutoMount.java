package ravex.modules.world;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.SwingUtility;
public class AutoMount extends Module {
    public final ravex.parameter.ModeParameter mode = new ravex.parameter.ModeParameter("Mode", "Normal", java.util.List.of("Normal", "Fast"));
    private int cooldown = 0;

    @Override
    public void onTick() {
        if ("Normal".equals(mode.getValue()) && cooldown > 0) {
            cooldown--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
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
            MobUtility.interact(mc, target);
            SwingUtility.swingMainHand(p);
            cooldown = 20;
        }
    }
    public static AutoMount itz() {
        return ModuleManager.get(AutoMount.class);
    }
}
