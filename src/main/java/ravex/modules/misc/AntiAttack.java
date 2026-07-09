package ravex.modules.misc;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.Villager;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;
import ravex.utility.misc.MobUtility;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.BooleanParameter;
public class AntiAttack extends Module {
    public final BooleanParameter villagers = new BooleanParameter("Villager", true);
    public final BooleanParameter horses    = new BooleanParameter("Horse", true);
    public final BooleanParameter wolves    = new BooleanParameter("Wolf", false);
    public final BooleanParameter cats      = new BooleanParameter("Cat", true);
    public final BooleanParameter llamas    = new BooleanParameter("Llama", true);
    public final BooleanParameter friends   = new BooleanParameter("Friend", true);

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (shouldCancel(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    public boolean shouldCancel(Entity target) {
        if (!getEnabled() || target == null) return false;
        if (target instanceof Villager && villagers.getValue()) return true;
        if (target instanceof Horse && horses.getValue()) return true;
        if (target instanceof Wolf && wolves.getValue()) return true;
        if (target instanceof Cat && cats.getValue()) return true;
        if (target instanceof Llama && llamas.getValue()) return true;
        if (MobUtility.isPlayer(MobUtility.asLivingEntity(target)) && friends.getValue()) return true;
        return false;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(AntiAttack.class);
    }

    public static AntiAttack itz() {
        return ModuleManager.get(AntiAttack.class);
    }
}
