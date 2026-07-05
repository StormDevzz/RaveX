package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class MiddleClick extends Module {
    public static final MiddleClick INSTANCE = new MiddleClick();
    public final ModeParameter elytraAction = new ModeParameter("ElytraAction", "Firework", List.of("Firework", "None"));
    public final ModeParameter blockAction = new ModeParameter("BlockAction", "XPBottle", List.of("XPBottle", "XPBottleFast", "None"));
    public final ModeParameter airAction = new ModeParameter("AirAction", "EnderPearl", List.of("EnderPearl", "None"));
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    private boolean pressed, heldBlockAction;
    private int holdTicks;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_fastexp");
    static {
        NATIVE.load();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        boolean held = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS;
        if (held) {
            if (!pressed) {
                pressed = true;
                if (useFastXp(mc)) { nativeStartFastXp(); }
                else { click(mc); heldBlockAction = isBlockContext(mc) && "XPBottle".equals(blockAction.getValue()); holdTicks = 0; }
            } else if (heldBlockAction) { holdTicks++; if (holdTicks % 2 == 0) click(mc); }
        } else {
            if (pressed && NATIVE.isLoaded()) nativeStopFastXp();
            pressed = false;
        }
    }
    private boolean useFastXp(Minecraft mc) {
        return !mc.player.isFallFlying() && isBlockContext(mc) && "XPBottleFast".equals(blockAction.getValue()) && NATIVE.isLoaded();
    }
    private boolean isBlockContext(Minecraft mc) {
        var p = mc.player;
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
            && mc.hitResult.getLocation().distanceToSqr(p.getEyePosition()) <= p.blockInteractionRange() * p.blockInteractionRange();
    }
    private void click(Minecraft mc) {
        var player = mc.player;
        Item target = player.isFallFlying() ? itemFromMode(elytraAction.getValue(), Items.FIREWORK_ROCKET)
            : isBlockContext(mc) ? itemFromMode(blockAction.getValue(), Items.EXPERIENCE_BOTTLE)
            : itemFromMode(airAction.getValue(), Items.ENDER_PEARL);
        if (target == null) return;
        int slot = InventoryUtility.findHotbarSlot(player, target);
        if (slot == -1) return;
        int prev = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (silent.getValue()) player.getInventory().setSelectedSlot(prev);
    }
    private Item itemFromMode(String mode, Item defaultItem) {
        return switch (mode) {
            case "None" -> null;
            case "Firework" -> Items.FIREWORK_ROCKET;
            case "XPBottle", "XPBottleFast" -> Items.EXPERIENCE_BOTTLE;
            case "EnderPearl" -> Items.ENDER_PEARL;
            default -> defaultItem;
        };
    }
    private static void fastXpCallback() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        int slot = InventoryUtility.findHotbarSlot(mc.player, Items.EXPERIENCE_BOTTLE);
        if (slot == -1) return;
        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (INSTANCE.silent.getValue()) mc.player.getInventory().setSelectedSlot(prev);
    }
    private static native void nativeStartFastXp();
    private static native void nativeStopFastXp();
}
