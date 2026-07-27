package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.MobUtility;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoClicker", category = "Combat")
public class AutoClicker {
    @Parameter(name = "MinCPS", min = 1.0, max = 40.0, step = 0.5)
    public double minCps = 8.0;
    @Parameter(name = "MaxCPS", min = 1.0, max = 40.0, step = 0.5)
    public double maxCps = 12.0;
    @Parameter(name = "Mode", modes = {"Left", "Right", "Both"})
    public String mode = "Left";
    @Parameter(name = "WeaponOnly")
    public boolean weaponOnly = false;
    @Parameter(name = "OnlyOnTarget")
    public boolean onlyOnTarget = true;
    @Parameter(name = "Randomize")
    public boolean randomize = true;
    @Parameter(name = "BreakBlocks")
    public boolean breakBlocks = false;
    @Parameter(name = "Jitter", min = 0.0, max = 2.0, step = 0.1)
    public double jitterStrength = 0.0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_autoclicker");
    static {
        NATIVE.load();
    }
    private long nextClick = 0;
    private long lastClickTime = 0;
    private boolean holding = false;
    private java.util.Random rng = new java.util.Random();
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (mc.getCurrentScreen() != null) return;
        long now = System.currentTimeMillis();
        if (weaponOnly) {
            var held = mc.getPlayer().getMainHandItem();
            if (!InventoryUtility.isSwordItem(held) && !InventoryUtility.isTrident(held)) return;
        }
        boolean targetValid = false;
        if (onlyOnTarget) {
            if (MobUtility.asLivingEntity(mc.getCrosshairPickEntity()) != null) {
                targetValid = true;
            }
        } else {
            targetValid = true;
        }
        if (!targetValid) {
            if (holding) {
                releaseClick(mc);
                holding = false;
            }
            return;
        }
        double cpsMin = minCps;
        double cpsMax = Math.max(cpsMin, maxCps);
        long delay;
        if (NATIVE.isLoaded()) {
            delay = nativeCalculateDelay(cpsMin, cpsMax, randomize);
        } else {
            double cps = randomize ? cpsMin + rng.nextDouble() * (cpsMax - cpsMin) : (cpsMin + cpsMax) / 2.0;
            delay = (long)(1000.0 / cps);
        }
        if (now >= nextClick) {
            String m = mode;
            if (m.equals("Left") || m.equals("Both")) {
                clickLeft(mc);
            }
            if (m.equals("Right") || m.equals("Both")) {
                clickRight(mc);
            }
            nextClick = now + delay + (randomize ? rng.nextInt((int)(delay * 0.15f)) : 0);
            lastClickTime = now;
            holding = true;
        }
    }
    private void clickLeft(MinecraftWrapper mc) {
        mc.getOptions().keyAttack.setDown(true);
        if (mc.getHitResult() instanceof net.minecraft.world.phys.EntityHitResult hit && MobUtility.asLivingEntity(hit.getEntity()) != null) {
            mc.getGameMode().attack(mc.getPlayer(), hit.getEntity());
        }
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        mc.getOptions().keyAttack.setDown(false);
        if (jitterStrength > 0 && mc.getPlayer() != null) {
            double str = jitterStrength;
            mc.getPlayer().setYRot((float)(mc.getPlayer().getYRot() + (rng.nextFloat() - 0.5) * str));
            mc.getPlayer().setXRot((float)(mc.getPlayer().getXRot() + (rng.nextFloat() - 0.5) * str));
        }
    }
    private void clickRight(MinecraftWrapper mc) {
        mc.getOptions().keyUse.setDown(true);
        if (mc.getGameMode() != null) {
            mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        mc.getOptions().keyUse.setDown(false);
    }
    private void releaseClick(MinecraftWrapper mc) {
        mc.getOptions().keyAttack.setDown(false);
        mc.getOptions().keyUse.setDown(false);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            releaseClick(mc);
        }
        holding = false;
        nextClick = 0;
    }
    private static native long nativeCalculateDelay(double minCps, double maxCps, boolean randomize);




}