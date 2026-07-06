package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
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
        return mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
            && mc.hitResult.getLocation().distanceToSqr(p.getEyePosition()) <= p.blockInteractionRange() * p.blockInteractionRange();
    }
    private void click(Minecraft mc) {
        var player = mc.player;
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
        };
    }
    private static void fastXpCallback() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        int slot = InventoryUtility.findHotbarSlot(mc.player, "experience_bottle");
        if (slot == -1) return;
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (INSTANCE.silent.getValue()) InventoryUtility.selectSlot(mc.player, prev);
    }
    private static native void nativeStartFastXp();
    private static native void nativeStopFastXp();
}
