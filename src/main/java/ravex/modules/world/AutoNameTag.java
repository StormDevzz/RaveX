package ravex.modules.world;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.MobUtility;
import net.minecraft.client.Minecraft;
<<<<<<< HEAD
public class AutoNameTag extends Module {
=======
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
public class AutoNameTag extends Module {
    public static final AutoNameTag INSTANCE = new AutoNameTag();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

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
<<<<<<< HEAD
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
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                }
            }
        }
        if (target == null) return;
<<<<<<< HEAD
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, tagSlot);
        MobUtility.interact(mc, target);
        SwingUtility.swingMainHand(p);
=======
        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(tagSlot);
        mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
        p.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (tagSlot != prevSlot) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
    }
    public static AutoNameTag itz() {
        return ModuleManager.get(AutoNameTag.class);
    }
}
