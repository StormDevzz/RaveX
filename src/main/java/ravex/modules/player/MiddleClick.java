package ravex.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;

public class MiddleClick extends Module {
    public static final MiddleClick INSTANCE = new MiddleClick();

    public final ModeParameter elytraAction = new ModeParameter("Elytra Action", "Firework", List.of("Firework", "None"));
    public final ModeParameter blockAction = new ModeParameter("Block Action", "XP Bottle", List.of("XP Bottle", "None"));
    public final ModeParameter airAction = new ModeParameter("Air Action", "Ender Pearl", List.of("Ender Pearl", "None"));
    public final BooleanParameter silent = new BooleanParameter("Silent", true);

    private boolean pressed;

    private MiddleClick() {
        super("MiddleClick", Category.PLAYER);
        addParameter(elytraAction);
        addParameter(blockAction);
        addParameter(airAction);
        addParameter(silent);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        long handle = mc.getWindow().handle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS) {
            if (!pressed) {
                pressed = true;
                click(mc);
            }
        } else {
            pressed = false;
        }
    }

    private void click(Minecraft mc) {
        var player = mc.player;
        Item target = null;

        if (player.isFallFlying()) {
            target = itemFromMode(elytraAction.getValue(), Items.FIREWORK_ROCKET);
        } else if (mc.hitResult instanceof BlockHitResult) {
            target = itemFromMode(blockAction.getValue(), Items.EXPERIENCE_BOTTLE);
        } else {
            target = itemFromMode(airAction.getValue(), Items.ENDER_PEARL);
        }

        if (target == null) return;
        useItem(mc, target);
    }

    private Item itemFromMode(String mode, Item defaultItem) {
        return switch (mode) {
            case "None" -> null;
            case "Firework" -> Items.FIREWORK_ROCKET;
            case "XP Bottle" -> Items.EXPERIENCE_BOTTLE;
            case "Ender Pearl" -> Items.ENDER_PEARL;
            default -> defaultItem;
        };
    }

    private void useItem(Minecraft mc, Item target) {
        var player = mc.player;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(target)) {
                slot = i;
                break;
            }
        }
        if (slot == -1) return;

        int prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (silent.getValue()) {
            player.getInventory().setSelectedSlot(prevSlot);
        }
    }
}
