package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import java.util.List;
public class Trigger extends Module {
    public static final Trigger INSTANCE = new Trigger();
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter cooldown = new NumberParameter("Cooldown", 0.9, 0.0, 1.0, 0.05);
    public final NumberParameter cps = new NumberParameter("CPS", 10, 1, 20, 1);
    public final NumberParameter randomization = new NumberParameter("Randomization", 0.0, 0.0, 5.0, 0.5);
    public final NumberParameter fov = new NumberParameter("FOV", 180, 10, 180, 5);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter passives = new BooleanParameter("Passives", false);
    public final BooleanParameter invisibles = new BooleanParameter("Invisibles", true);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", true);
    public final BooleanParameter weaponOnly = new BooleanParameter("WeaponOnly", false);
    public final BooleanParameter raytrace = new BooleanParameter("Raytrace", true);
    public final ModeParameter swingMode = new ModeParameter("Swing", "Client",
            List.of("Client", "Server", "Off"));
    public final ModeParameter clickMode = new ModeParameter("ClickMode", "Hold",
            List.of("Hold", "Toggle"));
    private boolean toggled = false;
    private long lastAttackTime = 0;

    @Override
    protected void onDisable() { toggled = false; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        String clickModeVal = clickMode.getValue();
        boolean shouldAttack;
        if (clickModeVal.equals("Toggle")) {
            if (mc.options.keyAttack.consumeClick()) toggled = !toggled;
            shouldAttack = toggled;
        } else {
            shouldAttack = mc.options.keyAttack.isDown();
        }
        if (!shouldAttack) return;
        if (weaponOnly.getValue() && !InventoryUtility.isWeapon(mc.player.getMainHandItem().getItem())) return;
        var target = MobUtility.asLivingEntity(InventoryUtility.getHitEntity(mc));
        if (target == null || !MobUtility.isAlive(target) || MobUtility.isSelf(target) || MobUtility.isArmorStand(target)) return;
        if (MobUtility.distanceToPlayer(target) > range.getValue()) return;
        if (!invisibles.getValue() && target.isInvisible()) return;
        if (!players.getValue() && MobUtility.isPlayer(target)) return;
        if (!monsters.getValue() && MobUtility.isHostile(target)) return;
        if (!passives.getValue() && MobUtility.isPassive(target)) return;
        if (!throughWalls.getValue() && !mc.player.hasLineOfSight(target)) return;
        if (raytrace.getValue() && !InventoryUtility.isLookingAtEntity(mc, target, 20.0)) return;
        long interval = 1000L / (long) cps.getValue().doubleValue();
        double r = randomization.getValue();
        if (r > 0.01) interval += (long) ((Math.random() - 0.5) * r * 100.0);
        if (System.currentTimeMillis() - lastAttackTime < interval) return;
        if (mc.player.getAttackStrengthScale(0.0f) < cooldown.getValue().floatValue()) return;
        float[] angles = RotationUtility.anglesTo(mc.player, target.position().add(0, target.getEyeHeight(target.getPose()) * 0.75, 0));
        float diffYaw = net.minecraft.util.Mth.wrapDegrees(angles[0] - mc.player.getYRot());
        float diffPitch = net.minecraft.util.Mth.wrapDegrees(angles[1] - mc.player.getXRot());
        if (Math.abs(diffYaw) > fov.getValue() || Math.abs(diffPitch) > fov.getValue()) return;
        InventoryUtility.attackEntity(mc, target, swingMode.getValue());
        lastAttackTime = System.currentTimeMillis();
    }
}
