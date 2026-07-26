package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.utility.player.SwingUtility;

import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.CameraUtility;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PhysicUtility;
import org.joml.Matrix4f;

import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.food.FoodUtility;
import ravex.utility.render.Render3DUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@ModuleInfo(name = "PearlTarget", category = "Combat")
public class PearlTarget implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Combat", "Pearl", "Follow"})
    public String mode = "Combat";
    @Parameter(name = "Range", min = 1.0, max = 32.0, step = 0.5)
    public double range = 16.0;
    @Parameter(name = "TargetMode", modes = {"Nearest", "Health", "Crosshair", "Distance"})
    public String targetMode = "Nearest";
    @Parameter(name = "SwitchDelay", min = 0.0, max = 2000.0, step = 50.0)
    public double switchDelay = 500.0;
    @Parameter(name = "Speed", min = 0.5, max = 5.0, step = 0.1)
    public double speed = 1.8;
    @Parameter(name = "SpeedSneak", min = 0.1, max = 2.0, step = 0.1)
    public double speedSneak = 0.6;
    @Parameter(name = "Strafe")
    public boolean strafe = true;
    @Parameter(name = "Jump")
    public boolean jump = true;
    @Parameter(name = "JumpHeight", min = 0.3, max = 0.6, step = 0.01)
    public double jumpHeight = 0.42;
    @Parameter(name = "PredictTicks", min = 20.0, max = 300.0, step = 10.0)
    public double predictTicks = 100.0;
    @Parameter(name = "ChaseTime", min = 500.0, max = 10000.0, step = 100.0)
    public double chaseTime = 3000.0;
    @Parameter(name = "StopDistance", min = 1.0, max = 6.0, step = 0.5)
    public double stopDistance = 3.5;
    @Parameter(name = "Sprint")
    public boolean sprint = true;
    @Parameter(name = "AutoWeapon")
    public boolean autoWeapon = true;
    @Parameter(name = "WeaponMode", modes = {"Sword", "Axe", "Both"})
    public String weaponMode = "Sword";
    @Parameter(name = "AutoGap")
    public boolean autoGap = false;
    @Parameter(name = "GapHealth", min = 1.0, max = 20.0, step = 1.0)
    public double gapHealth = 10.0;
    @Parameter(name = "AutoPearl")
    public boolean autoPearl = false;
    @Parameter(name = "PearlRange", min = 5.0, max = 50.0, step = 5.0)
    public double pearlRange = 20.0;
    @Parameter(name = "AutoTotem")
    public boolean autoTotem = false;
    @Parameter(name = "TotemMode", modes = {"Always", "LowHP", "AfterKill"})
    public String totemMode = "Always";
    @Parameter(name = "TotemHealth", min = 1.0, max = 20.0, step = 1.0)
    public double totemHealth = 6.0;
    @Parameter(name = "Attack")
    public boolean attack = true;
    @Parameter(name = "AttackRange", min = 2.0, max = 6.0, step = 0.1)
    public double attackRange = 4.0;
    @Parameter(name = "AttackCPS", min = 1.0, max = 20.0, step = 1.0)
    public double attackCps = 12.0;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "KeepRotate")
    public boolean keepRotate = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "LineColor", color = true)
    public int lineColor = 0xFFFF5500;
    @Parameter(name = "LandingColor", color = true)
    public int landingColor = 0xFFFF3333;
    @Parameter(name = "PearlColor", color = true)
    public int pearlColor = 0xFFFFFF00;
    @Parameter(name = "LineWidth", min = 0.5, max = 5.0, step = 0.5)
    public double lineWidth = 2.0;
    @Parameter(name = "RenderLine")
    public boolean renderLine = true;
    @Parameter(name = "RenderLanding")
    public boolean renderLanding = true;
    @Parameter(name = "RenderTrail")
    public boolean renderTrail = true;
    @Parameter(name = "RenderInfo")
    public boolean renderInfo = true;
    @Parameter(name = "ThroughWalls")
    public boolean renderThroughWalls = false;
    @Parameter(name = "PredictionLine")
    public boolean renderPredictionLine = true;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_pearltarget");
    private final Map<Integer, PearlData> trackedPearls = new HashMap<>();
    private net.minecraft.world.entity.player.Player target = null;
    private net.minecraft.world.phys.Vec3 targetPos = null;
    private net.minecraft.world.entity.player.Player lastPearlThrower = null;
    private net.minecraft.world.phys.Vec3 lastPearlLanding = null;
    private net.minecraft.world.phys.Vec3 lastPearlPos = null;
    private net.minecraft.world.phys.Vec3 lastPearlVel = null;
    private long lastPearlTime = 0;
    private long lastAttackTime = 0;
    private long lastTargetSwitchTime = 0;
    private int currentTargetIndex = 0;
    private boolean wasSprinting = false;
    public net.minecraft.world.phys.Vec3 renderPearlPos = null;
    public net.minecraft.world.phys.Vec3 renderLandingPos = null;
    public net.minecraft.world.phys.Vec3 renderTargetPos = null;
    static {
        NATIVE.load();
    }
    private PearlTarget() {
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double r = range;
        List<ThrownEnderpearl> pearls = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> players = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ThrownEnderpearl pearl) {
                if (MobUtility.distanceToPlayer(pearl) <= r) {
                    pearls.add(pearl);
                }
            }
            if (e instanceof net.minecraft.world.entity.player.Player p && !MobUtility.isSelf(p) && !p.isSpectator()) {
                if (MobUtility.distanceToPlayer(p) <= r * 1.5) {
                    players.add(p);
                }
            }
        }
        for (ThrownEnderpearl pearl : pearls) {
            net.minecraft.world.entity.Entity owner = pearl.getOwner();
            if (owner == mc.player) continue;
            int id = pearl.getId();
            net.minecraft.world.phys.Vec3 pos = pearl.position();
            net.minecraft.world.phys.Vec3 vel = pearl.getDeltaMovement();
            net.minecraft.world.phys.Vec3 landing = predictLanding(pos, vel);
            trackedPearls.put(id, new PearlData(
                id, owner != null ? owner.getUUID() : null,
                pos, vel, landing,
                System.currentTimeMillis(), pearl
            ));
            lastPearlTime = System.currentTimeMillis();
            lastPearlPos = pos;
            lastPearlVel = vel;
            lastPearlLanding = landing;
            if (owner instanceof net.minecraft.world.entity.player.Player playerOwner && playerOwner != target) {
                lastPearlThrower = playerOwner;
                if (System.currentTimeMillis() - lastTargetSwitchTime > switchDelay) {
                    target = playerOwner;
                    targetPos = landing;
                    lastTargetSwitchTime = System.currentTimeMillis();
                }
            } else if ("Pearl".equals(mode)) {
                targetPos = landing;
            }
        }
        Iterator<Map.Entry<Integer, PearlData>> it = trackedPearls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PearlData> entry = it.next();
            PearlData data = entry.getValue();
            boolean alive = mc.level.getEntity(data.entityId) != null;
            if (!alive || System.currentTimeMillis() - data.time > chaseTime) {
                it.remove();
            }
        }
        if (target == null && !players.isEmpty() && !"Pearl".equals(mode)) {
            target = findBestTarget(mc, players);
            if (target != null) targetPos = target.position();
        }
        if (target != null && (target.isRemoved() || MobUtility.isDead(target)
            || MobUtility.distanceToPlayer(target) > r * 2)) {
            target = null;
            targetPos = null;
        }
        if (target == null && targetPos == null) return;
        net.minecraft.world.phys.Vec3 myPos = mc.player.position();
        net.minecraft.world.phys.Vec3 moveTarget = "Follow".equals(mode) && target != null
            ? target.position() : targetPos;
        if (moveTarget == null) return;
        double dist = myPos.distanceTo(moveTarget);
        double attackDist = attackRange;
        boolean inRange = dist <= attackDist;
        updateRenderData(moveTarget);
        if (!inRange) {
            doChaseMovement(mc, moveTarget);
        }
        if (inRange && attack && target != null) {
            doAttack(mc, target);
        }
        if (autoTotem) {
            doAutoTotem(mc);
        }
        if (autoGap) {
            doAutoGap(mc);
        }
        if (autoPearl && target != null && dist > pearlRange * 0.8 && dist < pearlRange) {
            doAutoPearl(mc);
        }
    }
    private void doChaseMovement(Minecraft mc, net.minecraft.world.phys.Vec3 moveTarget) {
        net.minecraft.world.phys.Vec3 myPos = mc.player.position();
        net.minecraft.world.phys.Vec3 diff = moveTarget.subtract(myPos);
        double dist = diff.length();
        if (dist < 0.1) return;
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(diff.x, 0, diff.z).normalize();
        double speedVal = mc.player.onGround() ? speed : speed * 0.8;
        if (sprint) {
            mc.player.setSprinting(true);
            wasSprinting = true;
        }
        net.minecraft.world.phys.Vec3 motion = mc.player.getDeltaMovement();
        double targetVx = dir.x * speedVal;
        double targetVz = dir.z * speedVal;
        if (strafe && mc.player.onGround()) {
            motion = new net.minecraft.world.phys.Vec3(targetVx, motion.y, targetVz);
        } else {
            motion = new net.minecraft.world.phys.Vec3(targetVx, motion.y, targetVz);
        }
        mc.player.setDeltaMovement(motion);
        if (jump && mc.player.onGround() && dist > 1.5) {
            mc.player.jumpFromGround();
        }
        if (rotate && target != null) {
            float[] angles = RotationUtility.anglesTo(mc.player, target.position().add(0, target.getEyeHeight() * 0.8, 0));
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(RotationUtility.clampPitch(angles[1]));
        }
    }
    private void doAttack(Minecraft mc, net.minecraft.world.entity.player.Player target) {
        long now = System.currentTimeMillis();
        long attackDelay = (long) (1000.0 / attackCps);
        if (now - lastAttackTime < attackDelay) return;
        if (autoWeapon) {
            int bestSlot = findBestWeaponSlot(mc);
            if (bestSlot != -1) InventoryUtility.selectSlot(mc.player, bestSlot);
        }
        if (rotate) {
            float[] angles = RotationUtility.anglesTo(mc.player, target.position().add(0, target.getEyeHeight() * 0.8, 0));
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(RotationUtility.clampPitch(angles[1]));
        }
        mc.gameMode.attack(mc.player, target);
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        lastAttackTime = now;
    }
    private void doAutoTotem(Minecraft mc) {
        String mode = totemMode;
        boolean shouldTotem = "Always".equals(mode)
            || ("LowHP".equals(mode) && MobUtility.getHealthWithAbsorption(mc.player) <= totemHealth);
        if (!shouldTotem) return;
        int totemSlot = -1;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i < 9 ? i + 36 : i);
            if (InventoryUtility.isTotem(stack)) {
                totemSlot = i < 9 ? i + 36 : i;
                break;
            }
        }
        if (totemSlot == -1) return;
        if (InventoryUtility.isTotem(mc.player.getOffhandItem())) return;
        if (!mc.player.getOffhandItem().isEmpty()) return;
        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId,
            totemSlot < 36 ? totemSlot : totemSlot,
            0,
            net.minecraft.world.inventory.ClickType.QUICK_MOVE,
            mc.player
        );
    }
    private void doAutoGap(Minecraft mc) {
        if (MobUtility.getHealthWithAbsorption(mc.player) > gapHealth) return;
        if (mc.player.isUsingItem()) return;
        var gap = FoodUtility.findFood(f -> f.isAnyGoldenApple());
        if (gap == null) return;
        int gapSlot = gap.getSlot();
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, gapSlot);
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (keepRotate) {
            InventoryUtility.selectSlot(mc.player, prevSlot);
        }
    }
    private void doAutoPearl(Minecraft mc) {
        if (mc.player.isUsingItem()) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "ender_pearl")) {
                pearlSlot = i;
                break;
            }
        }
        if (pearlSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, pearlSlot);
        if (rotate && target != null) {
            float[] angles = RotationUtility.anglesTo(mc.player, target.position());
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(RotationUtility.clampPitch(angles[1]));
        }
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!keepRotate) {
            InventoryUtility.selectSlot(mc.player, prevSlot);
        }
    }
    private int findBestWeaponSlot(Minecraft mc) {
        int bestSlot = -1;
        double bestDamage = -1;
        String weaponModeVal = weaponMode;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            double dmg = getWeaponDamage(stack);
            if (dmg <= 0) continue;
            String name = stack.getItem().toString().toLowerCase();
            boolean isAxe = name.contains("_axe");
            boolean isSword = name.contains("_sword");
            if ("Axe".equals(weaponModeVal) && !isAxe) continue;
            if ("Sword".equals(weaponModeVal) && !isSword) continue;
            if (dmg > bestDamage) {
                bestDamage = dmg;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
    private double getWeaponDamage(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("netherite_sword")) return 8.0;
        if (name.contains("diamond_sword")) return 7.0;
        if (name.contains("netherite_axe")) return 7.0;
        if (name.contains("mace")) return 6.5;
        if (name.contains("diamond_axe")) return 6.0;
        if (name.contains("iron_sword")) return 6.0;
        if (name.contains("iron_axe")) return 5.0;
        if (name.contains("stone_sword")) return 5.0;
        if (name.contains("stone_axe")) return 4.5;
        if (name.contains("golden_sword") || name.contains("wooden_sword")) return 4.0;
        if (name.contains("golden_axe") || name.contains("wooden_axe")) return 4.0;
        return 0.0;
    }
    private net.minecraft.world.entity.player.Player findBestTarget(Minecraft mc, List<net.minecraft.world.entity.player.Player> players) {
        if (players.isEmpty()) return null;
        String mode = targetMode;
        return switch (mode) {
            case "Health" -> players.stream()
                .min(java.util.Comparator.comparingDouble(p -> MobUtility.getHealthWithAbsorption(p)))
                .orElse(null);
            case "Crosshair" -> {
                net.minecraft.world.entity.player.Player closest = null;
                double bestAngle = 180;
                net.minecraft.world.phys.Vec3 lookVec = mc.player.getLookAngle();
                for (net.minecraft.world.entity.player.Player p : players) {
                    net.minecraft.world.phys.Vec3 toTarget = p.position().add(0, p.getEyeHeight() * 0.5, 0)
                        .subtract(mc.player.getEyePosition()).normalize();
                    double angle = Math.acos(lookVec.dot(toTarget));
                    if (angle < bestAngle) {
                        bestAngle = angle;
                        closest = p;
                    }
                }
                yield closest;
            }
            case "Distance" -> players.stream()
                .min(java.util.Comparator.comparingDouble(p -> MobUtility.distanceToPlayer(p)))
                .orElse(null);
            default ->
                players.stream()
                    .min(java.util.Comparator.comparingDouble(p -> MobUtility.distanceToPlayer(p)))
                    .orElse(null);
        };
    }
    private net.minecraft.world.phys.Vec3 predictLanding(net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 vel) {
        int ticks = (int) predictTicks;
        if (NATIVE.isLoaded()) {
            try {
                double[] out = new double[7];
                nativePredictPearl(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, ticks, out);
                return new net.minecraft.world.phys.Vec3(out[0], out[1], out[2]);
            } catch (Exception e) {
            }
        }
        return javaPredictLanding(pos, vel, ticks);
    }
    private net.minecraft.world.phys.Vec3 javaPredictLanding(net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 vel, int ticks) {
        double x = pos.x, y = pos.y, z = pos.z;
        double mx = vel.x, my = vel.y, mz = vel.z;
        for (int t = 0; t < ticks; t++) {
            x += mx; y += my; z += mz;
            my -= 0.03;
            mx *= 0.99; my *= 0.99; mz *= 0.99;
            if (y < -64) break;
        }
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }
    private void updateRenderData(net.minecraft.world.phys.Vec3 moveTarget) {
        renderPearlPos = lastPearlPos;
        renderLandingPos = lastPearlLanding;
        renderTargetPos = moveTarget;
    }
    public void onDisable() {
        trackedPearls.clear();
        target = null;
        targetPos = null;
        lastPearlThrower = null;
        lastPearlLanding = null;
        lastPearlPos = null;
        renderPearlPos = null;
        renderLandingPos = null;
        renderTargetPos = null;
    }
    public void render(Matrix4f modelViewMatrix, net.minecraft.client.Camera camera) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PearlTarget").getEnabled() || !render) return;
        if (renderPearlPos == null && renderLandingPos == null && renderTargetPos == null) return;
        net.minecraft.world.phys.Vec3 camPos = camera.position();
        boolean throughWalls = renderThroughWalls;
        float lw = (float) lineWidth;
        int lc = lineColor;
        float lr = ((lc >> 16) & 0xFF) / 255.0f;
        float lg = ((lc >> 8) & 0xFF) / 255.0f;
        float lb = (lc & 0xFF) / 255.0f;
        int pc = pearlColor;
        float pr = ((pc >> 16) & 0xFF) / 255.0f;
        float pg = ((pc >> 8) & 0xFF) / 255.0f;
        float pb = (pc & 0xFF) / 255.0f;
        int ldc = landingColor;
        float ldr = ((ldc >> 16) & 0xFF) / 255.0f;
        float ldg = ((ldc >> 8) & 0xFF) / 255.0f;
        float ldb = (ldc & 0xFF) / 255.0f;
        if (renderPearlPos != null && renderLine) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                net.minecraft.world.phys.Vec3 playerPos = mc.player.position();
                Render3DUtility.batchAxisLine(modelViewMatrix,
                    (float) (playerPos.x - camPos.x), (float) (playerPos.y - camPos.y), (float) (playerPos.z - camPos.z),
                    (float) (renderPearlPos.x - camPos.x), (float) (renderPearlPos.y - camPos.y), (float) (renderPearlPos.z - camPos.z),
                    lw, pr, pg, pb, 0.8f, throughWalls);
            }
        }
        if (renderPearlPos != null && renderLandingPos != null && renderPredictionLine) {
            Render3DUtility.batchAxisLine(modelViewMatrix,
                (float) (renderPearlPos.x - camPos.x), (float) (renderPearlPos.y - camPos.y), (float) (renderPearlPos.z - camPos.z),
                (float) (renderLandingPos.x - camPos.x), (float) (renderLandingPos.y - camPos.y), (float) (renderLandingPos.z - camPos.z),
                lw, lr, lg, lb, 0.6f, throughWalls);
        }
        if (renderLandingPos != null && renderLanding) {
            org.joml.Matrix4f landingMat = new org.joml.Matrix4f(modelViewMatrix);
            landingMat.translate(
                (float)(renderLandingPos.x - camPos.x),
                (float)(renderLandingPos.y - camPos.y),
                (float)(renderLandingPos.z - camPos.z));
            Render3DUtility.batchFilledBox(landingMat, 0.3,
                ldr, ldg, ldb, 0.3f, throughWalls);
            Render3DUtility.batchWireframe(landingMat, 0.3,
                ldr, ldg, ldb, 0.9f, lw, throughWalls);
        }
        if (renderTargetPos != null && renderLine) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                net.minecraft.world.phys.Vec3 playerPos = mc.player.position();
                Render3DUtility.batchAxisLine(modelViewMatrix,
                    (float) (playerPos.x - camPos.x), (float) (playerPos.y - camPos.y), (float) (playerPos.z - camPos.z),
                    (float) (renderTargetPos.x - camPos.x), (float) (renderTargetPos.y - camPos.y), (float) (renderTargetPos.z - camPos.z),
                    lw * 0.5f, lr, lg, lb, 0.4f, throughWalls);
            }
        }
    }
    private static native void nativePredictPearl(double x, double y, double z, double mx, double my, double mz, int maxTicks, double[] out);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("PearlTarget").getEnabled();
    }
    public static PearlTarget itz() {
        return ravex.manager.ModuleManager.delegate(PearlTarget.class);
    }
    private static class PearlData {
        int entityId;
        UUID ownerUUID;
        net.minecraft.world.phys.Vec3 position, velocity, landing;
        long time;
        ThrownEnderpearl pearl;
        PearlData(int id, UUID owner, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 vel, net.minecraft.world.phys.Vec3 land, long t, ThrownEnderpearl p) {
            this.entityId = id;
            this.ownerUUID = owner;
            this.position = pos;
            this.velocity = vel;
            this.landing = land;
            this.time = t;
            this.pearl = p;
        }
    }


}