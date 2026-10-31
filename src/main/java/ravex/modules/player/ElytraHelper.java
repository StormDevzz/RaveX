package ravex.modules.player;

import org.lwjgl.glfw.GLFW;
import ravex.utility.misc.ScreenUtility;

import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.ElytraUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.client.ClientAlertUtility;
@Module(name = "ElytraHelper", category = "Player")
public class ElytraHelper {
    // swap key: elytra on, or chestplate back
    public ravex.parameter.Parameter<Integer> bind = new ravex.parameter.KeybindParameter("Bind", GLFW.GLFW_KEY_UNKNOWN);

    @Parameter(name = "Swap", group = true)
    public boolean swapGroup = true;
    @Parameter(name = "Mode", modes = {"Swap", "Replace", "Auto"}, visible = "swapGroup")
    public String mode = "Swap";
    @Parameter(name = "SwapMode", modes = {"Instant", "Safe", "Legit"}, visible = "swapGroup && mode=Swap")
    public String swapMode = "Instant";
    @Parameter(name = "MinDurability", min = 1.0, max = 50.0, step = 1.0, visible = "swapGroup && mode=Replace || swapGroup && mode=Auto")
    public double minDurability = 10.0;
    @Parameter(name = "PreferBetter", visible = "swapGroup && mode=Replace || swapGroup && mode=Auto")
    public boolean preferBetter = true;

    @Parameter(name = "Rocket", group = true)
    public boolean rocketGroup = true;
    @Parameter(name = "RocketMode", modes = {"Off", "Auto", "Boost"}, visible = "rocketGroup")
    public String rocketMode = "Off";
    @Parameter(name = "RocketSpeed", min = 0.05, max = 2.0, step = 0.05, visible = "rocketGroup && rocketMode=Auto || rocketGroup && rocketMode=Boost")
    public double rocketSpeed = 0.3;
    @Parameter(name = "RocketDelay", min = 500.0, max = 5000.0, step = 100.0, visible = "rocketGroup && rocketMode=Auto")
    public double rocketDelay = 1500.0;

    @Parameter(name = "Other", group = true)
    public boolean otherGroup = true;
    @Parameter(name = "AutoPitch", visible = "otherGroup")
    public boolean autoPitch = false;
    @Parameter(name = "PitchAngle", min = -90.0, max = 90.0, step = 5.0, visible = "otherGroup && autoPitch")
    public double pitchAngle = -45.0;
    @Parameter(name = "ChestSwapOnLand", visible = "otherGroup")
    public boolean chestSwapOnLand = false;

    private int state = 0, targetInvSlot = -1;
    private long lastActionTime = 0;
    private long lastRocketTime = 0;
    private boolean bindWasDown = false;

    public void onEnable() {
        state = 0;
        targetInvSlot = -1;
        bindWasDown = false;
        // no bind set: swap once on enable
        if ("Swap".equals(mode) && !isBindSet()) {
            initSwap(true);
        }
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) { Modules.setEnabled(ElytraHelper.class, false); return; }

        if (handleBind(mc)) return;

        String m = mode;
        if ("Swap".equals(m)) tickSwap(mc, p);
        else if ("Replace".equals(m) || "Auto".equals(m)) tickReplace(mc, p);

        if (!rocketMode.equals("Off") && ElytraUtility.isFallFlying(p)) {
            double accel = rocketSpeed;
            net.minecraft.world.phys.Vec3 look = p.getLookAngle();
            net.minecraft.world.phys.Vec3 motion = p.getDeltaMovement();
            MoveUtility.setMotion(motion.x + look.x * accel, motion.y + Math.abs(look.y) * accel * 0.5, motion.z + look.z * accel);
            if (rocketMode.equals("Auto")) {
                long now = System.currentTimeMillis();
                if (now - lastRocketTime >= (long) rocketDelay) {
                    if (ElytraUtility.useFirework(p)) lastRocketTime = now;
                }
            }
        }
        if (autoPitch && ElytraUtility.isFallFlying(p)) {
            ElytraUtility.setPitch(p, (float) pitchAngle);
        }
        if (chestSwapOnLand && p.onGround() && !p.isFallFlying() && ElytraUtility.isElytraEquipped(p)) {
            ElytraUtility.swapToChestplate(p);
        }
    }

    private boolean isBindSet() {
        Integer key = bind == null ? null : bind.getValue();
        return key != null && key > 0;
    }

    // runs even while the module is off
    public void handleGlobalBind() {
        if (Modules.enabled(ElytraHelper.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) return;
        if (state != 0) tickSwap(mc, p);
        else handleBind(mc);
    }

    // one key toggles both ways
    private boolean handleBind(MinecraftWrapper mc) {
        if (!isBindSet()) return false;
        boolean down = mc.getCurrentScreen() == null
            && GLFW.glfwGetKey(mc.getWindowHandle(), bind.getValue()) == GLFW.GLFW_PRESS;
        boolean pressed = down && !bindWasDown;
        bindWasDown = down;
        if (!pressed) return false;
        if (state == 0) initSwap(false);
        return true;
    }

    private void initSwap(boolean autoDisable) {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) { Modules.setEnabled(ElytraHelper.class, false); return; }
        boolean hasElytra = ElytraUtility.isElytraEquipped(p);
        int foundSlot = hasElytra ? ElytraUtility.findChestplateSlot(p) : ElytraUtility.findElytraSlot(p);
        if (foundSlot == -1) {
            ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t(
                hasElytra ? "elytrahelper_chest" : "elytrahelper_none"), 0xFFFF5555);
            if (autoDisable) Modules.setEnabled(ElytraHelper.class, false);
            return;
        }
        targetInvSlot = foundSlot;
        state = 0;
        lastActionTime = System.currentTimeMillis();
        String cm = swapMode;
        if ("Instant".equals(cm)) {
            swapClicks(mc, p, foundSlot);
            targetInvSlot = -1;
            if (autoDisable) Modules.setEnabled(ElytraHelper.class, false);
        } else if ("Legit".equals(cm)) {
            InventoryUtility.openInventoryScreen(p);
        }
    }

    private void swapClicks(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, int foundSlot) {
        InventoryUtility.clickSlot(mc, p, foundSlot, 0, InventoryUtility.PICKUP);
        InventoryUtility.clickChestSlot(mc, p, 6, InventoryUtility.PICKUP);
        InventoryUtility.clickSlot(mc, p, foundSlot, 0, InventoryUtility.PICKUP);
    }

    private void tickSwap(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if ("Instant".equals(swapMode)) return;
        if (targetInvSlot == -1) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;
        if (state == 0) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, InventoryUtility.PICKUP); state = 1; lastActionTime = now; }
        else if (state == 1) { InventoryUtility.clickChestSlot(mc, p, 6, InventoryUtility.PICKUP); state = 2; lastActionTime = now; }
        else if (state == 2) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, InventoryUtility.PICKUP); state = 3; lastActionTime = now; }
        else if (state == 3) {
            if ("Legit".equals(swapMode)) ScreenUtility.closeScreen(mc);
            state = 0;
            targetInvSlot = -1;
            if (!isBindSet()) Modules.setEnabled(ElytraHelper.class, false);
        }
    }
    private void tickReplace(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if (!ElytraUtility.isElytraEquipped(p)) return;
        if (ElytraUtility.getElytraDurability(p) > (int) minDurability) return;
        int slot = ElytraUtility.findElytraSlot(p, preferBetter ? (int) minDurability : 0);
        if (slot >= 0) {
            swapClicks(mc, p, slot);
        }
    }
}
