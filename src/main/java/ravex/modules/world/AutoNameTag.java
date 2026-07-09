package ravex.modules.world;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.MobUtility;
import net.minecraft.client.Minecraft;
public class AutoNameTag extends Module {

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
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
        for (var entity : mc.level.entitiesForRendering()) {
            if (MobUtility.isNameable(entity) && !MobUtility.hasName(entity, tagName)) {
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
        MobUtility.interact(mc, target);
        SwingUtility.swingMainHand(p);
        if (tagSlot != prevSlot) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
    }
    public static AutoNameTag itz() {
        return ModuleManager.get(AutoNameTag.class);
    }
}
