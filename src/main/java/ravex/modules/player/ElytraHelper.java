package ravex.modules.player;
import ravex.utility.misc.ScreenUtility;

import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.ElytraUtility;
import ravex.utility.player.InventoryUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "ElytraHelper", category = "Player")
public class ElytraHelper {
    @Parameter(name = "Mode", modes = {"Swap", "Replace", "Auto"})
    public String mode = "Swap";
    @Parameter(name = "SwapMode", modes = {"Positive1", "Positive2", "Positive3"})
    public String swapMode = "Positive1";
    @Parameter(name = "MinDurability", min = 1.0, max = 50.0, step = 1.0)
    public double minDurability = 10.0;
    @Parameter(name = "PreferBetter")
    public boolean preferBetter = true;

    @Parameter(name = "RocketMode", modes = {"Off", "Auto", "Boost"})
    public String rocketMode = "Off";
    @Parameter(name = "RocketSpeed", min = 0.05, max = 2.0, step = 0.05)
    public double rocketSpeed = 0.3;
    @Parameter(name = "RocketDelay", min = 500.0, max = 5000.0, step = 100.0)
    public double rocketDelay = 1500.0;

    @Parameter(name = "AutoPitch")
    public boolean autoPitch = false;
    @Parameter(name = "PitchAngle", min = -90.0, max = 90.0, step = 5.0)
    public double pitchAngle = -45.0;

    @Parameter(name = "ChestSwapOnLand")
    public boolean chestSwapOnLand = false;
    private int state = 0, targetInvSlot = -1;
    private long lastActionTime = 0;
    private long lastRocketTime = 0;

    public void onEnable() {
        if ("Swap".equals(mode)) initSwap();
    }
    private void initSwap() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) { Modules.setEnabled(ElytraHelper.class, false); return; }
        boolean hasElytra = ElytraUtility.isElytraEquipped(p);
        int foundSlot = hasElytra ? ElytraUtility.findChestplateSlot(p) : ElytraUtility.findElytraSlot(p);
        if (foundSlot == -1) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5ElytraHelper§7] §cNo replacement chest item found!"), false);
            Modules.setEnabled(ElytraHelper.class, false); return;
        }
        targetInvSlot = foundSlot;
        state = 0;
        lastActionTime = System.currentTimeMillis();
        String cm = swapMode;
        if ("Positive1".equals(cm)) {
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, InventoryUtility.PICKUP);
            InventoryUtility.clickChestSlot(mc, p, 6, InventoryUtility.PICKUP);
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, InventoryUtility.PICKUP);
            Modules.setEnabled(ElytraHelper.class, false);
        } else if ("Positive3".equals(cm)) {
            InventoryUtility.openInventoryScreen(p);
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) { Modules.setEnabled(ElytraHelper.class, false); return; }
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
        if (chestSwapOnLand && !p.onGround() && ElytraUtility.isElytraEquipped(p)) {

        }
        if (chestSwapOnLand && p.onGround() && !p.isFallFlying() && ElytraUtility.isElytraEquipped(p)) {
            ElytraUtility.swapToChestplate(p);
        }
    }
    private void tickSwap(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if ("Positive1".equals(swapMode)) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;
        if (state == 0) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, InventoryUtility.PICKUP); state = 1; lastActionTime = now; }
        else if (state == 1) { InventoryUtility.clickChestSlot(mc, p, 6, InventoryUtility.PICKUP); state = 2; lastActionTime = now; }
        else if (state == 2) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, InventoryUtility.PICKUP); state = 3; lastActionTime = now; }
        else if (state == 3) { if ("Positive3".equals(swapMode)) ScreenUtility.closeScreen(mc); Modules.setEnabled(ElytraHelper.class, false); }
    }
    private void tickReplace(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if (!ElytraUtility.isElytraEquipped(p)) return;
        if (ElytraUtility.getElytraDurability(p) > (int) minDurability) return;
        int slot = ElytraUtility.findElytraSlot(p, preferBetter ? (int) minDurability : 0);
        if (slot >= 0) {
            InventoryUtility.clickSlot(mc, p, slot, 0, InventoryUtility.PICKUP);
            InventoryUtility.clickChestSlot(mc, p, 6, InventoryUtility.PICKUP);
            InventoryUtility.clickSlot(mc, p, slot, 0, InventoryUtility.PICKUP);
        }
    }




}