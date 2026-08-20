package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import org.lwjgl.glfw.GLFW;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "MiddleClick", category = "Player")
public class MiddleClick {
    @Parameter(name = "ElytraAction", modes = {"Firework", "None"})
    public String elytraAction = "Firework";
    @Parameter(name = "BlockAction", modes = {"XPBottle", "XPBottleFast", "None"})
    public String blockAction = "XPBottle";
    @Parameter(name = "AirAction", modes = {"EnderPearl", "None"})
    public String airAction = "EnderPearl";
    @Parameter(name = "Silent")
    public boolean silent = true;
    private boolean pressed, heldBlockAction;
    private int holdTicks;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_fastexp");
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        boolean held = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS;
        if (held) {
            if (!pressed) {
                pressed = true;
                if (useFastXp(mc)) { nativeStartFastXp(); }
                else { click(mc); heldBlockAction = isBlockContext(mc) && "XPBottle".equals(blockAction); holdTicks = 0; }
            } else if (heldBlockAction) { holdTicks++; if (holdTicks % 2 == 0) click(mc); }
        } else {
            if (pressed && NATIVE.isLoaded()) nativeStopFastXp();
            pressed = false;
        }
    }
    private boolean useFastXp(MinecraftWrapper mc) {
        return !mc.getPlayer().isFallFlying() && isBlockContext(mc) && "XPBottleFast".equals(blockAction) && NATIVE.isLoaded();
    }
    private boolean isBlockContext(MinecraftWrapper mc) {
        var p = mc.getPlayer();
        return mc.getHitResult() != null && mc.getHitResult().getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
            && mc.getHitResult().getLocation().distanceToSqr(p.getEyePosition()) <= p.blockInteractionRange() * p.blockInteractionRange();
    }
    private void click(MinecraftWrapper mc) {
        var player = mc.getPlayer();
        var target = player.isFallFlying() ? itemFromMode(elytraAction)
            : isBlockContext(mc) ? itemFromMode(blockAction)
            : itemFromMode(airAction);
        if (target == null) return;
        int slot = InventoryUtility.findHotbarSlot(player, target);
        if (slot == -1) return;
        int prev = InventoryUtility.getSelectedSlot(player);
        InventoryUtility.selectSlot(player, slot);
        mc.getGameMode().useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (silent) InventoryUtility.selectSlot(player, prev);
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getGameMode() == null) return;
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "experience_bottle");
        if (slot == -1) return;
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        if (Modules.get(MiddleClick.class).silent) InventoryUtility.selectSlot(mc.getPlayer(), prev);
    }
    private static native void nativeStartFastXp();
    private static native void nativeStopFastXp();




}