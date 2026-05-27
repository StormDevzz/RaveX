package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;

public class Trigger extends Module {
    public static final Trigger INSTANCE = new Trigger();

    private Trigger() {
        super("Trigger", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Check if the user is looking directly at a living entity/target
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            if (!target.isAlive() || target == mc.player) return;

            // Attack cooldown check
            if (mc.player.getAttackStrengthScale(0.0f) >= 0.9f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
