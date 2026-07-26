package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.Villager;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;
import ravex.utility.misc.MobUtility;

@ModuleInfo(name = "AntiAttack", category = "Misc")
public class AntiAttack implements ModuleAccess {
    @Parameter(name = "Villager")
    public boolean villagers = true;
    @Parameter(name = "Horse")
    public boolean horses = true;
    @Parameter(name = "Wolf")
    public boolean wolves = false;
    @Parameter(name = "Cat")
    public boolean cats = true;
    @Parameter(name = "Llama")
    public boolean llamas = true;
    @Parameter(name = "Friend")
    public boolean friends = true;

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (shouldCancel(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    public boolean shouldCancel(net.minecraft.world.entity.Entity target) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("AntiAttack").getEnabled() || target == null) return false;
        if (target instanceof Villager && villagers) return true;
        if (target instanceof Horse && horses) return true;
        if (target instanceof Wolf && wolves) return true;
        if (target instanceof Cat && cats) return true;
        if (target instanceof Llama && llamas) return true;
        if (MobUtility.isPlayer(MobUtility.asLivingEntity(target)) && friends) return true;
        return false;
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AntiAttack").getEnabled();
    }

    public static AntiAttack itz() {
        return ravex.manager.ModuleManager.delegate(AntiAttack.class);
    }


}