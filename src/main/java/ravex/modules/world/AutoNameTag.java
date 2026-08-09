package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.EntityUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoNameTag", category = "World")
public class AutoNameTag {
public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        int tagSlot = -1;
        var tagStack = (net.minecraft.world.item.ItemStack) null;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (InventoryUtility.isItem(stack, "name_tag") && stack.getCustomName() != null) {
                tagSlot = i;
                tagStack = stack;
                break;
            }
        }
        if (tagSlot == -1 || tagStack == null) return;
        String tagName = tagStack.getHoverName().getString();
        var target = (net.minecraft.world.entity.LivingEntity) null;
        double closestDist = 4.5;
        for (var entity : mc.getLevel().entitiesForRendering()) {
            if (EntityUtility.isNameable(entity) && !EntityUtility.hasName(entity, tagName)) {
                double dist = p.distanceTo(entity);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = (net.minecraft.world.entity.LivingEntity) entity;
                }
            }
        }
        if (target == null) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, tagSlot);
        EntityUtility.interact(mc, target);
        SwingUtility.swingMainHand(p);
        if (tagSlot != prevSlot) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
    }
}
