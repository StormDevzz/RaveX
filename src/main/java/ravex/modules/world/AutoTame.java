package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class AutoTame extends Module {
    public static final AutoTame INSTANCE = new AutoTame();
    public final ModeParameter animal = new ModeParameter("Animal", "Wolf",
        List.of("Wolf", "Cat", "Llama"));
    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 6.0, 0.5);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", true);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        double r = range.getValue();
        AABB box = p.getBoundingBox().inflate(r);
        List<Entity> entities = mc.level.getEntities(p, box, e -> isTarget(e) && e.isAlive());
        for (Entity e : entities) {
            var target = MobUtility.asLivingEntity(e);
            if (!p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
                break;
            } else if (autoSwitch.getValue()) {
                int slot = findTameItem();
                if (slot != -1) {
                    InventoryUtility.selectSlot(p, slot);
                    mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
                    break;
                }
            }
        }
    }
    private boolean isTarget(Entity e) {
        String mode = animal.getValue();
        return switch (mode) {
            case "Wolf" -> e instanceof Wolf;
            case "Cat" -> e instanceof Cat;
            case "Llama" -> e instanceof Llama;
            default -> false;
        };
    }
    private int findTameItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        String mode = animal.getValue();
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty()) continue;
            boolean match = switch (mode) {
                case "Wolf" -> InventoryUtility.isItem(stack, "bone");
                case "Cat" -> InventoryUtility.isItem(stack, "cod") || InventoryUtility.isItem(stack, "salmon");
                case "Llama" -> InventoryUtility.isItem(stack, "hay_block");
                default -> false;
            };
            if (match) return i;
        }
        return -1;
    }
}
