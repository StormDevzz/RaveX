package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import ravex.utility.misc.MobUtility;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.rotation.RotationUtility;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.ArrayList;
import java.util.List;
public class ShieldFucker extends Module {
<<<<<<< HEAD
=======
    public static final ShieldFucker INSTANCE = new ShieldFucker();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter wallRange = new NumberParameter("WallRange", 3.0, 1.0, 6.0, 0.1);
    public final NumberParameter switchDelay = new NumberParameter("SwitchDelay", 100, 0, 500, 10);
    public final NumberParameter attackDelay = new NumberParameter("AttackDelay", 200, 50, 1000, 10);
    public final NumberParameter rotateSpeed = new NumberParameter("RotateSpeed", 180, 10, 180, 5);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", true);
    public final BooleanParameter targetPlayers = new BooleanParameter("Players", true);
    public final BooleanParameter targetMonsters = new BooleanParameter("Monsters", false);
    public final BooleanParameter onlyAxe = new BooleanParameter("OnlyAxe", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", true);
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent",
            List.of("Silent", "Normal", "None"));
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_shieldfucker");
    static {
        NATIVE.load();
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(ShieldFucker.class);
    }
    public static ShieldFucker itz() {
        return ModuleManager.get(ShieldFucker.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        return silentRotation.hasRotation;
    }
    @Override
    protected void onDisable() {
        silentRotation.hasRotation = false;
=======
        return hasSilentRotations;
    }
    @Override
    protected void onDisable() {
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (NATIVE.isLoaded()) {
            nativeReset();
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
<<<<<<< HEAD
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;
=======
            hasSilentRotations = false;
            return;
        }
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (NATIVE.isLoaded()) {
            nativeTick(mc);
        } else {
            javaTick(mc);
        }
    }
    private void nativeTick(Minecraft mc) {
<<<<<<< HEAD
        var pos = mc.player.position();
=======
        Vec3 pos = mc.player.position();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        double[] entityData = collectEntityData(mc);
        BreakAction action = nativeTick(
            pos.x, pos.y, pos.z,
            mc.player.getYRot(), mc.player.getXRot(),
            entityData,
            range.getValue(), wallRange.getValue(),
            switchDelay.getValue(), attackDelay.getValue(),
            rotateSpeed.getValue(),
            throughWalls.getValue(), autoSwitch.getValue(),
            targetPlayers.getValue(), targetMonsters.getValue(),
            onlyAxe.getValue(),
            mc.player.getMainHandItem().getItem().toString(),
<<<<<<< HEAD
            InventoryUtility.getSelectedSlot(mc.player)
=======
            mc.player.getInventory().getSelectedSlot()
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        );
        if (action == null) return;
        processAction(mc, action);
    }
    private void javaTick(Minecraft mc) {
        double maxDist = range.getValue();
<<<<<<< HEAD
        var target = (net.minecraft.world.entity.LivingEntity) null;
        double bestDist = Double.MAX_VALUE;
        for (var e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (MobUtility.isSelf(le) || MobUtility.isDead(le)) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!targetPlayers.getValue() && MobUtility.isPlayer(le)) continue;
            if (!targetMonsters.getValue() && MobUtility.isHostile(le)) continue;
            if (!hasShield(le)) continue;
            if (!le.isBlocking()) continue;
=======
        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le == mc.player || le.isDeadOrDying()) continue;
            if (le instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            if (!targetPlayers.getValue() && le instanceof Player) continue;
            if (!targetMonsters.getValue() && (le instanceof Monster || le instanceof EnderDragon || le instanceof WitherBoss)) continue;
            if (!hasShield(le)) continue;
            if (!isBlockingWithShield(le)) continue;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;
            if (!throughWalls.getValue() && !mc.player.hasLineOfSight(le)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                target = le;
            }
        }
        if (target == null) {
<<<<<<< HEAD
            silentRotation.hasRotation = false;
=======
            hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return;
        }
        handleAction(mc, target);
    }
<<<<<<< HEAD
    private boolean hasShield(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof Player player) {
            return InventoryUtility.isItem(player.getOffhandItem(), "shield")
                || InventoryUtility.isItem(player.getMainHandItem(), "shield");
        }
        return false;
    }
    private void handleAction(Minecraft mc, net.minecraft.world.entity.LivingEntity target) {
        String rotMode = rotate.getValue();
        boolean doRotate = !rotMode.equals("None");
        if (doRotate) {
            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target.position().add(0, 0.25, 0));
            if (rotMode.equals("Silent")) {
                silentRotation.set(angles[0], angles[1]);
=======
    private boolean hasShield(LivingEntity entity) {
        if (entity instanceof Player player) {
            ItemStack offhand = player.getOffhandItem();
            ItemStack mainhand = player.getMainHandItem();
            return offhand.is(Items.SHIELD) || mainhand.is(Items.SHIELD);
        }
        return false;
    }
    private boolean isBlockingWithShield(LivingEntity entity) {
        return entity.isBlocking();
    }
    private void handleAction(Minecraft mc, LivingEntity target) {
        String rotMode = rotate.getValue();
        boolean doRotate = !rotMode.equals("None");
        if (doRotate) {
            float[] angles = calculateAngles(mc, target.position());
            if (rotMode.equals("Silent")) {
                silentYaw = angles[0];
                silentPitch = angles[1];
                hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            } else {
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(angles[1]);
            }
        }
<<<<<<< HEAD
        if (onlyAxe.getValue() && !InventoryUtility.isAxeItem(mc.player.getMainHandItem())) {
            if (autoSwitch.getValue()) {
                int axeSlot = findAxeSlot(mc);
                if (axeSlot != -1 && axeSlot != InventoryUtility.getSelectedSlot(mc.player)) {
                    InventoryUtility.selectSlot(mc.player, axeSlot);
=======
        if (onlyAxe.getValue() && !(mc.player.getMainHandItem().getItem() instanceof AxeItem)) {
            if (autoSwitch.getValue()) {
                int axeSlot = findAxeSlot(mc);
                if (axeSlot != -1 && axeSlot != mc.player.getInventory().getSelectedSlot()) {
                    mc.player.getInventory().setSelectedSlot(axeSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                }
            }
            return;
        }
        if (mc.player.getAttackStrengthScale(0.0f) >= 0.85f) {
<<<<<<< HEAD
            MobUtility.attack(mc, target);
            ravex.utility.player.SwingUtility.swingMainHand(mc.player);
=======
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
    private int findAxeSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            if (InventoryUtility.isAxeItem(InventoryUtility.getItem(mc.player, i))) return i;
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) return i;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return -1;
    }
    private void processAction(Minecraft mc, BreakAction action) {
        if (action.targetId < 0) {
<<<<<<< HEAD
            silentRotation.hasRotation = false;
            return;
        }
        var target = mc.level.getEntity(action.targetId);
        if (!(target instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive()) {
            silentRotation.hasRotation = false;
=======
            hasSilentRotations = false;
            return;
        }
        Entity target = mc.level.getEntity(action.targetId);
        if (!(target instanceof LivingEntity le) || !le.isAlive()) {
            hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return;
        }
        String rotMode = rotate.getValue();
        if (!rotMode.equals("None")) {
            if (rotMode.equals("Silent")) {
<<<<<<< HEAD
                silentRotation.set(action.yaw, action.pitch);
=======
                silentYaw = action.yaw;
                silentPitch = action.pitch;
                hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            } else {
                mc.player.setYRot(action.yaw);
                mc.player.setXRot(action.pitch);
            }
        }
        if (action.shouldSwitch && autoSwitch.getValue()) {
            int slot = action.switchSlot >= 0 ? action.switchSlot : findAxeSlot(mc);
<<<<<<< HEAD
            if (slot != -1 && slot != InventoryUtility.getSelectedSlot(mc.player)) {
                InventoryUtility.selectSlot(mc.player, slot);
=======
            if (slot != -1 && slot != mc.player.getInventory().getSelectedSlot()) {
                mc.player.getInventory().setSelectedSlot(slot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }
        if (action.shouldBreak) {
            if (mc.player.getAttackStrengthScale(0.0f) >= 0.85f) {
<<<<<<< HEAD
                MobUtility.attack(mc, le);
                ravex.utility.player.SwingUtility.swingMainHand(mc.player);
=======
                mc.gameMode.attack(mc.player, le);
                mc.player.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }
    }
    private double[] collectEntityData(Minecraft mc) {
        List<Double> data = new ArrayList<>();
        double maxDist = range.getValue();
<<<<<<< HEAD
        for (var e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (MobUtility.isSelf(le) || MobUtility.isDead(le)) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!targetPlayers.getValue() && MobUtility.isPlayer(le)) continue;
            if (!targetMonsters.getValue() && MobUtility.isHostile(le)) continue;
            if (MobUtility.distanceToPlayer(le) > maxDist) continue;
            if (!(le instanceof Player player)) continue;
            boolean shield = InventoryUtility.isItem(player.getOffhandItem(), "shield")
                || InventoryUtility.isItem(player.getMainHandItem(), "shield");
=======
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le == mc.player || le.isDeadOrDying()) continue;
            if (le instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            if (!targetPlayers.getValue() && le instanceof Player) continue;
            if (!targetMonsters.getValue() && (le instanceof Monster || le instanceof EnderDragon || le instanceof WitherBoss)) continue;
            if (mc.player.distanceTo(le) > maxDist) continue;
            if (!(le instanceof Player player)) continue;
            boolean shield = player.getOffhandItem().is(Items.SHIELD) || player.getMainHandItem().is(Items.SHIELD);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
=======
    private float[] calculateAngles(Minecraft mc, Vec3 targetPos) {
        return RotationUtility.anglesTo(mc.player.getEyePosition(), targetPos.add(0, 0.25, 0));
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
