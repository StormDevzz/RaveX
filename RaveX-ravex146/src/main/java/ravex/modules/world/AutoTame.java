package ravex.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;

public class AutoTame extends Module {
    public static final AutoTame INSTANCE = new AutoTame();

    public final ModeParameter animal = new ModeParameter("Animal", "Wolf",
        List.of("Wolf", "Cat", "Llama"));
    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 6.0, 0.5);
    public final BooleanParameter autoSwitch = new BooleanParameter("Auto Switch", true);

    private AutoTame() {
        super("AutoTame", Category.WORLD);
        addParameter(animal);
        addParameter(range);
        addParameter(autoSwitch);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        double r = range.getValue();
        AABB box = p.getBoundingBox().inflate(r);
        List<Entity> entities = mc.level.getEntities(p, box, e -> isTarget(e) && e.isAlive());

        for (Entity e : entities) {
            LivingEntity target = (LivingEntity) e;
            if (!p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
                break;
            } else if (autoSwitch.getValue()) {
                int slot = findTameItem();
                if (slot != -1) {
                    p.getInventory().setSelectedSlot(slot);
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
        var inv = mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            boolean match = switch (mode) {
                case "Wolf" -> stack.is(Items.BONE);
                case "Cat" -> stack.is(Items.COD) || stack.is(Items.SALMON);
                case "Llama" -> stack.is(Items.HAY_BLOCK);
                default -> false;
            };
            if (match) return i;
        }
        return -1;
    }
}
