package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
<<<<<<< HEAD
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import org.lwjgl.glfw.GLFW;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class MiddleClick extends Module {
<<<<<<< HEAD
=======
    public static final MiddleClick INSTANCE = new MiddleClick();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        return mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
=======
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            && mc.hitResult.getLocation().distanceToSqr(p.getEyePosition()) <= p.blockInteractionRange() * p.blockInteractionRange();
    }
    private void click(Minecraft mc) {
        var player = mc.player;
<<<<<<< HEAD
        var target = player.isFallFlying() ? itemFromMode(elytraAction.getValue())
            : isBlockContext(mc) ? itemFromMode(blockAction.getValue())
            : itemFromMode(airAction.getValue());
        if (target == null) return;
        int slot = InventoryUtility.findHotbarSlot(player, target);
        if (slot == -1) return;
        int prev = InventoryUtility.getSelectedSlot(player);
        InventoryUtility.selectSlot(player, slot);
        mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (silent.getValue()) InventoryUtility.selectSlot(player, prev);
    }
    private String itemFromMode(String mode) {
        return switch (mode) {
            case "None" -> null;
            case "Firework" -> "firework_rocket";
            case "XPBottle", "XPBottleFast" -> "experience_bottle";
            case "EnderPearl" -> "ender_pearl";
            default -> null;
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        };
    }
    private static void fastXpCallback() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
<<<<<<< HEAD
        int slot = InventoryUtility.findHotbarSlot(mc.player, "experience_bottle");
        if (slot == -1) return;
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (ModuleManager.get(MiddleClick.class).silent.getValue()) InventoryUtility.selectSlot(mc.player, prev);
    }
    private static native void nativeStartFastXp();
    private static native void nativeStopFastXp();
    public static boolean maybeEnabled() {
        return maybeEnabled(MiddleClick.class);
    }
    public static MiddleClick itz() {
        return ModuleManager.get(MiddleClick.class);
    }

}
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
