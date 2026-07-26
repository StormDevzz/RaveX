package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.utility.misc.MobUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "AutoClicker", category = "Combat")
public class AutoClicker extends ravex.modules.Module {
public final NumberParameter minCps = new NumberParameter("MinCPS", 8.0, 1.0, 40.0, 0.5);
    public final NumberParameter maxCps = new NumberParameter("MaxCPS", 12.0, 1.0, 40.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Left", java.util.List.of("Left", "Right", "Both"));
    public final BooleanParameter weaponOnly = new BooleanParameter("WeaponOnly", false);
    public final BooleanParameter onlyOnTarget = new BooleanParameter("OnlyOnTarget", true);
    public final BooleanParameter randomize = new BooleanParameter("Randomize", true);
    public final BooleanParameter breakBlocks = new BooleanParameter("BreakBlocks", false);
    public final NumberParameter jitterStrength = new NumberParameter("Jitter", 0.0, 0.0, 2.0, 0.1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_autoclicker");
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
        if (weaponOnly.getValue()) {
            var held = mc.player.getMainHandItem();
            if (!InventoryUtility.isSwordItem(held) && !InventoryUtility.isTrident(held)) return;
        }
        boolean targetValid = false;
        if (onlyOnTarget.getValue()) {
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
        double cpsMin = minCps.getValue();
        double cpsMax = Math.max(cpsMin, maxCps.getValue());
        long delay;
        if (NATIVE.isLoaded()) {
            delay = nativeCalculateDelay(cpsMin, cpsMax, randomize.getValue());
        } else {
            double cps = randomize.getValue() ? cpsMin + rng.nextDouble() * (cpsMax - cpsMin) : (cpsMin + cpsMax) / 2.0;
            delay = (long)(1000.0 / cps);
        }
        if (now >= nextClick) {
            String m = mode.getValue();
            if (m.equals("Left") || m.equals("Both")) {
                clickLeft(mc);
            }
            if (m.equals("Right") || m.equals("Both")) {
                clickRight(mc);
            }
            nextClick = now + delay + (randomize.getValue() ? rng.nextInt((int)(delay * 0.15f)) : 0);
            lastClickTime = now;
            holding = true;
        }
    }
    private void clickLeft(Minecraft mc) {
        mc.options.keyAttack.setDown(true);
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && MobUtility.asLivingEntity(hit.getEntity()) != null) {
            mc.gameMode.attack(mc.player, hit.getEntity());
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.options.keyAttack.setDown(false);
        if (jitterStrength.getValue() > 0 && mc.player != null) {
            double str = jitterStrength.getValue();
            mc.player.setYRot((float)(mc.player.getYRot() + (rng.nextFloat() - 0.5) * str));
            mc.player.setXRot((float)(mc.player.getXRot() + (rng.nextFloat() - 0.5) * str));
        }
    }
    private void clickRight(Minecraft mc) {
        mc.options.keyUse.setDown(true);
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
        mc.options.keyUse.setDown(false);
    }
    private void releaseClick(Minecraft mc) {
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }
    protected void onDisable() {
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

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}