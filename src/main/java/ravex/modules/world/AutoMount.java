package ravex.modules.world;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.SwingUtility;
=======
import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.InteractionHand;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        var target = (net.minecraft.world.entity.Entity) null;
        double closestDist = 4.5;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity.isAlive() && entity != p) {
                if (MobUtility.isMountable(entity) && !MobUtility.isVehicle(entity)) {
                    double dist = p.distanceTo(entity);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = entity;
=======
        Entity target = null;
        double closestDist = 4.5;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity.isAlive() && entity != p) {
                boolean mountable = false;
                if (entity instanceof Horse || entity instanceof Donkey || entity instanceof Mule ||
                    entity instanceof SkeletonHorse || entity instanceof ZombieHorse || entity instanceof Llama) {
                    mountable = true;
                } else if (entity instanceof Pig pig && pig.isSaddled()) {
                    mountable = true;
                } else if (entity instanceof Strider strider && strider.isSaddled()) {
                    mountable = true;
                }
                if (mountable) {
                    if (!entity.isVehicle()) {
                        double dist = p.distanceTo(entity);
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = entity;
                        }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
