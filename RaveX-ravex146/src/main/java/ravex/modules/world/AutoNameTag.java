package ravex.modules.world;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

public class AutoNameTag extends Module {
    public static final AutoNameTag INSTANCE = new AutoNameTag();

    private AutoNameTag() {
        super("AutoNameTag", Category.WORLD);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;


        int tagSlot = -1;
        ItemStack tagStack = null;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.NAME_TAG) && stack.getCustomName() != null) {
                tagSlot = i;
                tagStack = stack;
                break;
            }
        }

        if (tagSlot == -1 || tagStack == null) return;
        String tagName = tagStack.getHoverName().getString();


        LivingEntity target = null;
        double closestDist = 4.5;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !(entity instanceof ArmorStand)) {
                if (living.isAlive()) {

                    if (living.getCustomName() != null && tagName.equals(living.getCustomName().getString())) {
                        continue;
                    }
                    double dist = p.distanceTo(living);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = living;
                    }
                }
            }
        }

        if (target == null) return;


        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(tagSlot);

        mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
        p.swing(InteractionHand.MAIN_HAND);

        if (tagSlot != prevSlot) {
            p.getInventory().setSelectedSlot(prevSlot);
        }
    }
}
