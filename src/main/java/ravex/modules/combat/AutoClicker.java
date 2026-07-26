package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.MobUtility;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "AutoClicker", category = "Combat")
public class AutoClicker implements ModuleAccess {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        long now = System.currentTimeMillis();
        if (weaponOnly) {
            var held = mc.player.getMainHandItem();
            if (!InventoryUtility.isSwordItem(held) && !InventoryUtility.isTrident(held)) return;
        }
        boolean targetValid = false;
        if (onlyOnTarget) {
            if (MobUtility.asLivingEntity(mc.crosshairPickEntity) != null) {
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
    private void clickLeft(Minecraft mc) {
        mc.options.keyAttack.setDown(true);
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && MobUtility.asLivingEntity(hit.getEntity()) != null) {
            mc.gameMode.attack(mc.player, hit.getEntity());
        }
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        mc.options.keyAttack.setDown(false);
        if (jitterStrength > 0 && mc.player != null) {
            double str = jitterStrength;
            mc.player.setYRot((float)(mc.player.getYRot() + (rng.nextFloat() - 0.5) * str));
            mc.player.setXRot((float)(mc.player.getXRot() + (rng.nextFloat() - 0.5) * str));
        }
    }
    private void clickRight(Minecraft mc) {
        mc.options.keyUse.setDown(true);
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        mc.options.keyUse.setDown(false);
    }
    private void releaseClick(Minecraft mc) {
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            releaseClick(mc);
        }
        holding = false;
        nextClick = 0;
    }
    private static native long nativeCalculateDelay(double minCps, double maxCps, boolean randomize);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoClicker").getEnabled();
    }
    public static AutoClicker itz() {
        return ravex.manager.ModuleManager.delegate(AutoClicker.class);
    }


}