package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.Villager;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;
import ravex.modules.Modules;

@Module(name = "AntiAttack", category = "Misc")
public class AntiAttack {
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
        if (!Modules.enabled(AntiAttack.class) || target == null) return false;
        if (target instanceof Villager && villagers) return true;
        if (target instanceof Horse && horses) return true;
        if (target instanceof Wolf && wolves) return true;
        if (target instanceof Cat && cats) return true;
        if (target instanceof Llama && llamas) return true;
        if (EntityUtility.isPlayer(EntityUtility.asLivingEntity(target)) && friends) return true;
        return false;
    }






}
