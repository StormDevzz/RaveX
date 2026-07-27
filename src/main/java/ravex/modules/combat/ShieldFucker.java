package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;

import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "ShieldFucker", category = "Combat")
public class ShieldFucker {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "WallRange", min = 1.0, max = 6.0, step = 0.1)
    public double wallRange = 3.0;
    @Parameter(name = "SwitchDelay", min = 0, max = 500, step = 10)
    public double switchDelay = 100;
    @Parameter(name = "AttackDelay", min = 50, max = 1000, step = 10)
    public double attackDelay = 200;
    @Parameter(name = "RotateSpeed", min = 10, max = 180, step = 5)
    public double rotateSpeed = 180;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;
    @Parameter(name = "Players")
    public boolean targetPlayers = true;
    @Parameter(name = "Monsters")
    public boolean targetMonsters = false;
    @Parameter(name = "OnlyAxe")
    public boolean onlyAxe = true;
    @Parameter(name = "AutoSwitch")
    public boolean autoSwitch = true;
    @Parameter(name = "Rotate", modes = {"Silent", "Normal", "None"})
    public String rotate = "Silent";
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_shieldfucker");
    static {
        NATIVE.load();
    }


    public static class BreakAction {
        public final int targetId;
        public final float yaw;
        public final float pitch;
        public final boolean shouldBreak;
        public final boolean shouldSwitch;
        public final int switchSlot;
        public BreakAction(int targetId, float yaw, float pitch,
                           boolean shouldBreak, boolean shouldSwitch, int switchSlot) {
            this.targetId = targetId;
            this.yaw = yaw;
            this.pitch = pitch;
            this.shouldBreak = shouldBreak;
            this.shouldSwitch = shouldSwitch;
            this.switchSlot = switchSlot;
        }
    }

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public void onDisable() {
        silentRotation.hasRotation = false;
        if (NATIVE.isLoaded()) {
            nativeReset();
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;
        if (NATIVE.isLoaded()) {
            nativeTick(mc);
        } else {
            javaTick(mc);
        }
    }
    private void nativeTick(MinecraftWrapper mc) {
        var pos = mc.getPlayer().position();
        double[] entityData = collectEntityData(mc);
        BreakAction action = nativeTick(
            pos.x, pos.y, pos.z,
            mc.getPlayer().getYRot(), mc.getPlayer().getXRot(),
            entityData,
            range, wallRange,
            switchDelay, attackDelay,
            rotateSpeed,
            throughWalls, autoSwitch,
            targetPlayers, targetMonsters,
            onlyAxe,
            mc.getPlayer().getMainHandItem().getItem().toString(),
            InventoryUtility.getSelectedSlot(mc.getPlayer())
        );
        if (action == null) return;
        processAction(mc, action);
    }
    private void javaTick(MinecraftWrapper mc) {
        double maxDist = range;
        var target = (net.minecraft.world.entity.LivingEntity) null;
        double bestDist = Double.MAX_VALUE;
        for (var e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (MobUtility.isSelf(le) || MobUtility.isDead(le)) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!targetPlayers && MobUtility.isPlayer(le)) continue;
            if (!targetMonsters && MobUtility.isHostile(le)) continue;
            if (!hasShield(le)) continue;
            if (!le.isBlocking()) continue;
            double dist = mc.getPlayer().distanceTo(le);
            if (dist > maxDist) continue;
            if (!throughWalls && !mc.getPlayer().hasLineOfSight(le)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                target = le;
            }
        }
        if (target == null) {
            silentRotation.hasRotation = false;
            return;
        }
        handleAction(mc, target);
    }
    private boolean hasShield(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            return InventoryUtility.isItem(player.getOffhandItem(), "shield")
                || InventoryUtility.isItem(player.getMainHandItem(), "shield");
        }
        return false;
    }
    private void handleAction(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        String rotMode = rotate;
        boolean doRotate = !rotMode.equals("None");
        if (doRotate) {
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target.position().add(0, 0.25, 0));
            if (rotMode.equals("Silent")) {
                silentRotation.set(angles[0], angles[1]);
            } else {
                mc.getPlayer().setYRot(angles[0]);
                mc.getPlayer().setXRot(angles[1]);
            }
        }
        if (onlyAxe && !InventoryUtility.isAxeItem(mc.getPlayer().getMainHandItem())) {
            if (autoSwitch) {
                int axeSlot = findAxeSlot(mc);
                if (axeSlot != -1 && axeSlot != InventoryUtility.getSelectedSlot(mc.getPlayer())) {
                    InventoryUtility.selectSlot(mc.getPlayer(), axeSlot);
                }
            }
            return;
        }
        if (mc.getPlayer().getAttackStrengthScale(0.0f) >= 0.85f) {
            MobUtility.attack(mc, target);
            ravex.utility.player.SwingUtility.swingMainHand(mc.getPlayer());
        }
    }
    private int findAxeSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isAxeItem(InventoryUtility.getItem(mc.getPlayer(), i))) return i;
        }
        return -1;
    }
    private void processAction(MinecraftWrapper mc, BreakAction action) {
        if (action.targetId < 0) {
            silentRotation.hasRotation = false;
            return;
        }
        var target = mc.getLevel().getEntity(action.targetId);
        if (!(target instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive()) {
            silentRotation.hasRotation = false;
            return;
        }
        String rotMode = rotate;
        if (!rotMode.equals("None")) {
            if (rotMode.equals("Silent")) {
                silentRotation.set(action.yaw, action.pitch);
            } else {
                mc.getPlayer().setYRot(action.yaw);
                mc.getPlayer().setXRot(action.pitch);
            }
        }
        if (action.shouldSwitch && autoSwitch) {
            int slot = action.switchSlot >= 0 ? action.switchSlot : findAxeSlot(mc);
            if (slot != -1 && slot != InventoryUtility.getSelectedSlot(mc.getPlayer())) {
                InventoryUtility.selectSlot(mc.getPlayer(), slot);
            }
        }
        if (action.shouldBreak) {
            if (mc.getPlayer().getAttackStrengthScale(0.0f) >= 0.85f) {
                MobUtility.attack(mc, le);
                ravex.utility.player.SwingUtility.swingMainHand(mc.getPlayer());
            }
        }
    }
    private double[] collectEntityData(MinecraftWrapper mc) {
        List<Double> data = new ArrayList<>();
        double maxDist = range;
        for (var e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (MobUtility.isSelf(le) || MobUtility.isDead(le)) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!targetPlayers && MobUtility.isPlayer(le)) continue;
            if (!targetMonsters && MobUtility.isHostile(le)) continue;
            if (MobUtility.distanceToPlayer(le) > maxDist) continue;
            if (!(le instanceof net.minecraft.world.entity.player.Player player)) continue;
            boolean shield = InventoryUtility.isItem(player.getOffhandItem(), "shield")
                || InventoryUtility.isItem(player.getMainHandItem(), "shield");
            boolean blocking = player.isBlocking();
            if (!shield || !blocking) continue;
            data.add((double) le.getId());
            data.add(le.getX());
            data.add(le.getY());
            data.add(le.getZ());
            data.add((double) le.getHealth());
            data.add(shield ? 1.0 : 0.0);
            data.add(blocking ? 1.0 : 0.0);
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < data.size(); i++) arr[i] = data.get(i);
        return arr;
    }
    private static native BreakAction nativeTick(
        double pX, double pY, double pZ,
        float pYaw, float pPitch,
        double[] entityData,
        double range, double wallRange,
        double switchDelay, double attackDelay,
        double rotateSpeed,
        boolean throughWalls, boolean autoSwitch,
        boolean targetPlayers, boolean targetMonsters,
        boolean onlyAxe,
        String currentItem, int currentSlot
    );
    private static native void nativeReset();


}