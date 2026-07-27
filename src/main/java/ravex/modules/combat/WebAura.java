package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;


@Module(name = "WebAura", category = "Combat")
public class WebAura {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Delay", min = 0.0, max = 500.0, step = 10.0)
    public double placeDelay = 100.0;
    @Parameter(name = "Rotate", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String rotate = "NCP";
    @Parameter(name = "Swap", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String swapMode = "NCP";
    @Parameter(name = "Target", modes = {"Closest", "LowestHP"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "Passives", "All"})
    public String targetType = "Players";
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    private long lastPlaceTime = 0;
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();


    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public void onEnable() {
        lastPlaceTime = 0;
        silentRotation.reset();
    }
    public void onDisable() {
        silentRotation.reset();
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < (long) placeDelay) return;
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) return;
        net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(target.getX(), target.getY(), target.getZ());
        if (!mc.getLevel().getBlockState(targetPos).isAir()) return;
        int webSlot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "cobweb");
        if (webSlot == -1) {
            if (InventoryUtility.findSlot(mc.getPlayer(), "cobweb") != -1) {
                int hotbarSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                int invSlot = InventoryUtility.findSlot(mc.getPlayer(), "cobweb");
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), invSlot, hotbarSlot, InventoryUtility.SWAP);
                webSlot = hotbarSlot;
            } else {
                return;
            }
        }
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(targetPos);
        rotateTo(mc, hitVec);

        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        String swap = swapMode;
        if (swap.equals("NCP")) {
            if (originalSlot != webSlot) {
                InventoryUtility.silentSelectSlot(mc.getPlayer(), webSlot);
            }
        } else if (swap.equals("Vanilla")) {
            InventoryUtility.selectSlot(mc.getPlayer(), webSlot);
        } else if (swap.equals("Legit")) {
            if (originalSlot != webSlot) {
                InventoryUtility.selectSlot(mc.getPlayer(), webSlot);
            }
        } else {
            if (originalSlot != webSlot) return;
        }
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
            hitVec, net.minecraft.core.Direction.UP, targetPos, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        if (swap.equals("NCP") && originalSlot != webSlot) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        } else if ((swap.equals("Vanilla") || swap.equals("Legit")) && originalSlot != webSlot) {
            InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
        }
        lastPlaceTime = now;
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (mode.equals("NCP")) {
            if (!silentRotation.initialized) {
                silentRotation.init(currentYaw, currentPitch);
            }
            currentYaw = silentRotation.lastYaw;
            currentPitch = silentRotation.lastPitch;
            float[] limited = AimUtility.limitAngles(currentYaw, RotationUtility.fixAngle(angles[0]), currentPitch, RotationUtility.fixAngle(angles[1]), 180.0f);
            silentRotation.set(limited[0], limited[1]);
            silentRotation.lastYaw = limited[0];
            silentRotation.lastPitch = limited[1];
        } else if (mode.equals("Vanilla")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        } else if (mode.equals("Legit")) {
            float maxSpeed = 90.0f;
            float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, angles[1], maxSpeed);
            limited = AimUtility.randomize(limited[0], limited[1], 1.5f);
            mc.getPlayer().setYRot(limited[0]);
            mc.getPlayer().setXRot(limited[1]);
        }
    }
    private boolean isRotationAligned(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        if (rotate.equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    private net.minecraft.world.entity.LivingEntity findTarget(MinecraftWrapper mc) {
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = range;
        String mode = targetMode;
        String typeFilter = targetType;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (typeFilter.equals("Players")) {
                if (!MobUtility.isPlayer(le)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!MobUtility.isHostile(le)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (MobUtility.isPlayer(le) || MobUtility.isHostile(le)) continue;
            }
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"   -> dist;
                case "LowestHP" -> MobUtility.getHealth(le);
                default          -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }


}
