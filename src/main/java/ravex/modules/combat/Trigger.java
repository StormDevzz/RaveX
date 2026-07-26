package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;

import ravex.utility.misc.MobUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import java.util.List;
@ModuleInfo(name = "Trigger", category = "Combat")
public class Trigger implements ModuleAccess {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Cooldown", min = 0.0, max = 1.0, step = 0.05)
    public double cooldown = 0.9;
    @Parameter(name = "CPS", min = 1, max = 20, step = 1)
    public double cps = 10;
    @Parameter(name = "Randomization", min = 0.0, max = 5.0, step = 0.5)
    public double randomization = 0.0;
    @Parameter(name = "FOV", min = 10, max = 180, step = 5)
    public double fov = 180;
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Monsters")
    public boolean monsters = true;
    @Parameter(name = "Passives")
    public boolean passives = false;
    @Parameter(name = "Invisibles")
    public boolean invisibles = true;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;
    @Parameter(name = "WeaponOnly")
    public boolean weaponOnly = false;
    @Parameter(name = "Raytrace")
    public boolean raytrace = true;
    @Parameter(name = "Swing", modes = {"Client", "Server", "Off"})
    public String swingMode = "Client";
    @Parameter(name = "ClickMode", modes = {"Hold", "Toggle"})
    public String clickMode = "Hold";
    private boolean toggled = false;
    private long lastAttackTime = 0;
    private net.minecraft.world.entity.LivingEntity currentTarget = null;

    public net.minecraft.world.entity.LivingEntity getCurrentTarget() {
        return currentTarget;
    }
    public void onDisable() {
        toggled = false;
        currentTarget = null;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            currentTarget = null;
            return;
        }
        String clickModeVal = clickMode;
        boolean shouldAttack;
        if (clickModeVal.equals("Toggle")) {
            if (mc.options.keyAttack.consumeClick()) toggled = !toggled;
            shouldAttack = toggled;
        } else {
            shouldAttack = mc.options.keyAttack.isDown();
        }
        if (!shouldAttack) {
            currentTarget = null;
            return;
        }
        if (weaponOnly && !InventoryUtility.isWeapon(mc.player.getMainHandItem().getItem())) {
            currentTarget = null;
            return;
        }
        var target = MobUtility.asLivingEntity(InventoryUtility.getHitEntity(mc));
        if (target == null || !MobUtility.isAlive(target) || MobUtility.isSelf(target) || MobUtility.isArmorStand(target)) {
            currentTarget = null;
            return;
        }
        if (MobUtility.distanceToPlayer(target) > range) {
            currentTarget = null;
            return;
        }
        if (!invisibles && target.isInvisible()) {
            currentTarget = null;
            return;
        }
        if (!players && MobUtility.isPlayer(target)) {
            currentTarget = null;
            return;
        }
        if (!monsters && MobUtility.isHostile(target)) {
            currentTarget = null;
            return;
        }
        if (!passives && MobUtility.isPassive(target)) {
            currentTarget = null;
            return;
        }
        if (!throughWalls && !mc.player.hasLineOfSight(target)) {
            currentTarget = null;
            return;
        }
        if (raytrace && !InventoryUtility.isLookingAtEntity(mc, target, 20.0)) {
            currentTarget = null;
            return;
        }
        float[] angles = RotationUtility.anglesTo(mc.player, target.position().add(0, target.getEyeHeight(target.getPose()) * 0.75, 0));
        float diffYaw = net.minecraft.util.Mth.wrapDegrees(angles[0] - mc.player.getYRot());
        float diffPitch = net.minecraft.util.Mth.wrapDegrees(angles[1] - mc.player.getXRot());
        if (Math.abs(diffYaw) > fov || Math.abs(diffPitch) > fov) {
            currentTarget = null;
            return;
        }
        currentTarget = target;
        long interval = 1000L / (long) (double) cps;
        double r = randomization;
        if (r > 0.01) interval += (long) ((Math.random() - 0.5) * r * 100.0);
        if (System.currentTimeMillis() - lastAttackTime < interval) return;
        if (mc.player.getAttackStrengthScale(0.0f) < (float) cooldown) return;
        InventoryUtility.attackEntity(mc, target, swingMode);
        lastAttackTime = System.currentTimeMillis();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Trigger").getEnabled();
    }
    public static Trigger itz() {
        return ravex.manager.ModuleManager.delegate(Trigger.class);
    }


}