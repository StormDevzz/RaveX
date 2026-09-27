package ravex.modules.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.client.ClientAlertUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;

@Module(name = "Scaffold", category = "World")
public class Scaffold {
    @Parameter(name = "Mode", modes = {"Vanilla", "Grim", "Verus", "Strict"})
    public String mode = "Grim";
    @Parameter(name = "Expand")
    public boolean expand = false;
    @Parameter(name = "ExpandMode", modes = {"Forward", "Around"}, visible = "expand")
    public String expandMode = "Forward";
    @Parameter(name = "ExpandForward", min = 1, max = 10, step = 1, visible = "expand && expandMode=Forward")
    public int expandForward = 3;
    @Parameter(name = "ExpandBackward", min = 1, max = 10, step = 1, visible = "expand && expandMode=Forward")
    public int expandBackward = 4;
    @Parameter(name = "ExpandSide", min = 1, max = 10, step = 1, visible = "expand && expandMode=Forward")
    public int expandSide = 5;
    @Parameter(name = "ExpandRadius", min = 1, max = 5, step = 1, visible = "expand && expandMode=Around")
    public int expandRadius = 1;
    @Parameter(name = "BuildSpeed", min = 1, max = 20, step = 1)
    public int buildSpeed = 20;
    @Parameter(name = "Tower")
    public boolean tower = true;
    @Parameter(name = "TowerMode", modes = {"Normal", "Fast", "Strict"}, visible = "tower")
    public String towerMode = "Normal";
    @Parameter(name = "Eagle", visible = "mode!=Strict")
    public boolean eagle = true;
    @Parameter(name = "KeepY")
    public boolean keepY = false;
    @Parameter(name = "NoRotate")
    public boolean noRotate = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Animate")
    public boolean animate = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int highlightColor = 0xFFFF33CC;
    @Parameter(name = "Debug")
    public boolean debug = false;
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 1.0f;
    public static float renderG = 0.2f;
    public static float renderB = 0.8f;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    public static float viewYaw = 0f;
    public static float viewPitch = 0f;
    public static boolean capturingView = false;
    private int currX, currY, currZ;
    private boolean hasCurr;
    private boolean wasPlacing;
    private long lastDebugTime;
    private String lastDebugMsg = "";
    private double targetY = -1;
    private float prevAimYaw;
    private float prevAimPitch;
    private boolean hasPrevAim;
    private int[] lastDir;
    private int[] currDir;
    private long lastTargetMs;
    private int lastPlaceX = Integer.MIN_VALUE;
    private int lastPlaceY;
    private int lastPlaceZ;
    private long lastPlaceMs;
    private int lastExpandLen = 3;

    public BlockPos getCurrentPos() {
        if (!hasCurr) return null;
        if (System.currentTimeMillis() - lastTargetMs > 800) {
            hasCurr = false;
            return null;
        }
        return BlockUtility.pos(currX, currY, currZ);
    }

    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        targetY = p != null ? Math.floor(p.getY()) : -1;
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
        wasPlacing = false;
        capturingView = false;
        hasPrevAim = false;
        lastDir = null;
        currDir = null;
        lastTargetMs = 0;
        lastDebugMsg = "";
    }

    public void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p != null && noRotate && wasPlacing) {
            p.setYRot(viewYaw);
            p.setXRot(viewPitch);
        }
        if (eagleActive() && mc.getOptions() != null) {
            mc.getOptions().keyShift.setDown(physicalSneak(mc));
        }
        wasPlacing = false;
        capturingView = false;
        hasPrevAim = false;
        lastDir = null;
        currDir = null;
        lastTargetMs = 0;
        lastDebugMsg = "";
    }

    private static boolean physicalSneak(MinecraftWrapper mc) {
        long win = mc.getWindowHandle();
        if (win == 0L) return false;
        return org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || mc.getOptions() == null) return;
        boolean onGround = p.onGround();
        if (onGround) {
            targetY = Math.floor(p.getY());
        }
        boolean towerJump = tower && mc.getOptions().keyJump.isDown();
        boolean towerMoving = mc.getOptions().keyUp.isDown() || mc.getOptions().keyDown.isDown()
            || mc.getOptions().keyLeft.isDown() || mc.getOptions().keyRight.isDown();
        if (towerJump && !towerMoving) {
            if ("Strict".equals(towerMode)) {
                if (onGround) {
                    MoveUtility.setMotion(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
                }
            } else {
                double boost = "Fast".equals(towerMode) ? 0.6 : 0.42;
                MoveUtility.setMotion(p.getDeltaMovement().x, boost, p.getDeltaMovement().z);
            }
        }
        if (!onGround && !towerJump) {
            bail(mc, p, "wait air");
            return;
        }
        int[] dir = inputDir(mc, p);
        currDir = dir;
        int bx = (int) Math.floor(p.getX());
        int bz = (int) Math.floor(p.getZ());
        int ry;
        if (keepY && targetY != -1) {
            ry = (int) Math.floor(targetY) - 1;
        } else {
            ry = (int) Math.floor(p.getY()) - 1;
        }
        var target = pickTarget(mc, p, towerJump, bx, ry, bz, dir);
        if (target == null) {
            bail(mc, p, dir == null && !towerJump ? "no input" : "no target");
            return;
        }
        int slot = findPlaceableSlot(mc, p, target);
        if (slot == -1) {
            bail(mc, p, "no block slot");
            return;
        }
        currX = target.pos.getX();
        currY = target.pos.getY();
        currZ = target.pos.getZ();
        hasCurr = true;
        lastTargetMs = System.currentTimeMillis();
        if (render) {
            int hc = highlightColor;
            renderR = ((hc >> 16) & 0xFF) / 255.0f;
            renderG = ((hc >> 8) & 0xFF) / 255.0f;
            renderB = (hc & 0xFF) / 255.0f;
        }
        var nb = target.neighbor;
        var face = target.face;
        var faceVec = SwingUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        boolean aimReady = !strictMode() || (hasPrevAim && aimHitsBlock(p, nb, prevAimYaw, prevAimPitch));
        if (noRotate) {
            if (!wasPlacing) {
                viewYaw = p.getYRot();
                viewPitch = p.getXRot();
                capturingView = true;
            }
            wasPlacing = true;
            p.setYRot(viewYaw);
            p.setXRot(viewPitch);
            prevAimYaw = viewYaw;
            prevAimPitch = viewPitch;
            hasPrevAim = true;
        } else {
            wasPlacing = false;
            capturingView = false;
            var aim = SwingUtility.centerOf(nb).add(faceVec.scale(0.5)).subtract(faceVec.scale(0.05));
            float[] aimRot = RotationUtility.anglesTo(p.getEyePosition(), aim);
            float[] limited = AimUtility.limitAngles(
                p.getYRot(), aimRot[0],
                p.getXRot(), aimRot[1],
                18.0f
            );
            p.setYRot(limited[0]);
            p.setXRot(limited[1]);
            prevAimYaw = limited[0];
            prevAimPitch = limited[1];
            hasPrevAim = true;
        }
        if (!aimReady) {
            applyEagle(mc, p, target);
            debug("wait aim");
            return;
        }
        long nowPlace = System.currentTimeMillis();
        boolean dup = target.pos.getX() == lastPlaceX && target.pos.getY() == lastPlaceY
            && target.pos.getZ() == lastPlaceZ && nowPlace - lastPlaceMs < 600;
        if (dup) {
            applyEagle(mc, p, target);
            debug("wait sync");
            return;
        }
        if (buildSpeed < 20 && nowPlace - lastPlaceMs < 1000L / buildSpeed) {
            applyEagle(mc, p, target);
            debug("wait rate");
            return;
        }
        try {
            place(mc, p, slot, nb, face);
        } catch (Throwable t) {
            debug("err " + t);
            return;
        }
        applyEagle(mc, p, target);
    }

    @Nullable
    private TargetResult pickTarget(MinecraftWrapper mc, LocalPlayer p, boolean towerJump,
                                    int bx, int ry, int bz, int[] dir) {
        boolean grim = strictMode() || "Grim".equals(mode);
        if (towerJump && !p.onGround()) {
            int fillY = (int) Math.floor(p.getY()) - 1;
            var fill = checkCandidate(mc, p, grim, bx, fillY, bz, false);
            if (fill != null) return fill;
        }
        var under = checkCandidate(mc, p, grim, bx, ry, bz, false);
        if (under != null) return under;
        if (isAir(bx, ry, bz)) {
            int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] s : sides) {
                if (!isAir(bx + s[0], ry, bz + s[1])) continue;
                var side = checkCandidate(mc, p, grim, bx + s[0], ry, bz + s[1], false);
                if (side != null) return side;
            }
        }
        if (towerJump || !expand) return null;
        if ("Around".equals(expandMode)) {
            return expandTargetAround(mc, p, grim, bx, ry, bz, expandRadius);
        }
        if (dir == null) return null;
        int sx = Integer.signum(dir[0]);
        int sz = Integer.signum(dir[1]);
        if (sx == 0 && sz == 0) return null;
        return expandTarget(mc, p, grim, bx, ry, bz, sx, sz, expandLen(mc));
    }

    @Nullable
    private TargetResult expandTargetAround(MinecraftWrapper mc, LocalPlayer p, boolean grim,
                                            int bx, int ry, int bz, int radius) {
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = bx + dx;
                    int z = bz + dz;
                    if (!isAir(x, ry, z)) continue;
                    var cand = checkCandidate(mc, p, grim, x, ry, z, false);
                    if (cand != null) return cand;
                }
            }
        }
        return null;
    }

    private int expandLen(MinecraftWrapper mc) {
        var opts = mc.getOptions();
        if (opts.keyUp.isDown()) lastExpandLen = expandForward;
        else if (opts.keyDown.isDown()) lastExpandLen = expandBackward;
        else if (opts.keyLeft.isDown() || opts.keyRight.isDown()) lastExpandLen = expandSide;
        return lastExpandLen;
    }

    @Nullable
    private TargetResult expandTarget(MinecraftWrapper mc, LocalPlayer p, boolean grim,
                                      int bx, int ry, int bz, int sx, int sz, int len) {
        boolean diagonal = sx != 0 && sz != 0;
        for (int i = 1; i <= len; i++) {
            int[][] cells;
            if (diagonal) {
                cells = new int[][]{{i, i}, {i, i - 1}, {i - 1, i}};
            } else if (sx != 0) {
                cells = new int[][]{{i, 0}};
            } else {
                cells = new int[][]{{0, i}};
            }
            boolean anyAir = false;
            for (int[] c : cells) {
                int x = bx + sx * c[0];
                int z = bz + sz * c[1];
                if (!isAir(x, ry, z)) continue;
                anyAir = true;
                var cand = checkCandidate(mc, p, grim, x, ry, z, false);
                if (cand != null) return cand;
            }
            if (anyAir) return null;
        }
        return null;
    }

    @Nullable
    private TargetResult checkCandidate(MinecraftWrapper mc, LocalPlayer p, boolean grim,
                                        int x, int y, int z, boolean overVoid) {
        if (!isAir(x, y, z)) return null;
        if (overVoid && !isAir(x, y - 1, z)) return null;
        var pos = BlockUtility.pos(x, y, z);
        if (p.getBoundingBox().intersects(new AABB(pos))) return null;
        var neighbor = findNeighbor(x, y, z, grim);
        if (neighbor == null && grim) neighbor = findNeighbor(x, y, z, false);
        if (neighbor == null) return null;
        return new TargetResult(pos, neighbor.neighbor, neighbor.face);
    }

    private void place(MinecraftWrapper mc, LocalPlayer p, int slot,
                       BlockPos nb, Direction face) {
        var hit = hitFor(nb, face);
        var faceVec = SwingUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        var aim = hit.getLocation().subtract(faceVec.scale(0.05));
        float[] exact = RotationUtility.anglesTo(p.getEyePosition(), aim);
        float prevY = p.getYRot();
        float prevX = p.getXRot();
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        if (slot != prevSlot) {
            InventoryUtility.selectSlot(p, slot);
        }
        p.setYRot(exact[0]);
        p.setXRot(exact[1]);
        var res = BlockUtility.useItemOn(mc, hit);
        BlockUtility.swing(mc);
        if (slot != prevSlot) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
        p.setYRot(prevY);
        p.setXRot(prevX);
        var target = nb.relative(face);
        if (res != null && res.consumesAction()) {
            lastPlaceX = target.getX();
            lastPlaceY = target.getY();
            lastPlaceZ = target.getZ();
            lastPlaceMs = System.currentTimeMillis();
            debug("placed " + target.toShortString());
        } else {
            debug(diagnose(mc, p, slot, nb, face, hit, res));
        }
    }

    private String diagnose(MinecraftWrapper mc, LocalPlayer p, int slot,
                            BlockPos nb, Direction face,
                            BlockHitResult hit, InteractionResult res) {
        var level = mc.getLevel();
        var target = nb.relative(face);
        var stack = InventoryUtility.getItem(p, slot);
        var item = stack.getItem();
        var sb = new StringBuilder("fail res=").append(res);
        sb.append(" h=").append(BuiltInRegistries.ITEM.getKey(item));
        sb.append(" t=").append(target.toShortString());
        sb.append(" tst=").append(BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock()));
        sb.append(" nb=").append(BuiltInRegistries.BLOCK.getKey(level.getBlockState(nb).getBlock()));
        sb.append(" f=").append(face.getName());
        sb.append(" ov=").append(p.getBoundingBox().intersects(new AABB(target)));
        sb.append(" sec=").append(p.isSecondaryUseActive());
        if (item instanceof BlockItem bi) {
            var ctx = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack, hit);
            var st = bi.getBlock().getStateForPlacement(ctx);
            sb.append(" pst=").append(st);
            if (st != null) {
                sb.append(" surv=").append(st.canSurvive(level, target));
                sb.append(" obst=").append(level.isUnobstructed(st, target,
                    CollisionContext.placementContext(p)));
            }
        }
        sb.append(" y=").append(p.getY());
        return sb.toString();
    }

    private void bail(MinecraftWrapper mc, LocalPlayer p, String msg) {
        releaseView(p);
        if (eagleActive() && mc.getOptions() != null) {
            boolean want = false;
            if (p.onGround() && currDir != null && (currDir[0] != 0 || currDir[1] != 0)) {
                int fx = (int) Math.floor(p.getX()) + currDir[0];
                int fz = (int) Math.floor(p.getZ()) + currDir[1];
                want = isAir(fx, (int) Math.floor(p.getY()) - 1, fz);
            }
            mc.getOptions().keyShift.setDown(physicalSneak(mc) || want);
        }
        debug(msg);
    }

    private boolean strictMode() {
        return "Strict".equals(mode);
    }

    private boolean eagleActive() {
        return strictMode() || eagle;
    }

    private void applyEagle(MinecraftWrapper mc, LocalPlayer p, TargetResult target) {
        if (!eagleActive() || mc.getOptions() == null) return;
        int aboveY = target.pos.getY() + 1;
        boolean wantSneak = p.onGround() && isAir(target.pos.getX(), aboveY, target.pos.getZ());
        mc.getOptions().keyShift.setDown(physicalSneak(mc) || wantSneak);
    }

    private static boolean aimHitsBlock(LocalPlayer p, BlockPos nb, float yaw, float pitch) {
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        double[] dir = {
            -Math.sin(yr) * Math.cos(pr),
            -Math.sin(pr),
            Math.cos(yr) * Math.cos(pr)
        };
        var eye = p.getEyePosition();
        double[] origin = {eye.x, eye.y, eye.z};
        double[] lo = {nb.getX(), nb.getY(), nb.getZ()};
        double[] hi = {nb.getX() + 1, nb.getY() + 1, nb.getZ() + 1};
        double maxDist = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        double tMin = 0.0;
        double tMax = maxDist;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dir[i]) < 1.0E-9) {
                if (origin[i] < lo[i] || origin[i] > hi[i]) return false;
            } else {
                double t1 = (lo[i] - origin[i]) / dir[i];
                double t2 = (hi[i] - origin[i]) / dir[i];
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                if (t1 > tMin) tMin = t1;
                if (t2 < tMax) tMax = t2;
                if (tMin > tMax) return false;
            }
        }
        return true;
    }

    private void releaseView(LocalPlayer p) {
        if (noRotate && wasPlacing) {
            wasPlacing = false;
            capturingView = false;
            hasPrevAim = false;
            p.setYRot(viewYaw);
            p.setXRot(viewPitch);
        }
    }

    private void debug(String msg) {
        if (!debug) return;
        long now = System.currentTimeMillis();
        if (msg.equals("no target") && now - lastDebugTime < 1500) {
            lastDebugMsg = msg;
            return;
        }
        if (now - lastDebugTime < 350) {
            lastDebugMsg = msg;
            return;
        }
        boolean changed = !msg.equals(lastDebugMsg);
        if (changed || now - lastDebugTime > 2000) {
            lastDebugMsg = msg;
            lastDebugTime = now;
            ClientAlertUtility.alert("§8[§5Scaffold§8] §7" + msg);
        }
    }

    @Nullable
    private int[] inputDir(MinecraftWrapper mc, LocalPlayer p) {
        var opts = mc.getOptions();
        float fwd = 0f;
        float str = 0f;
        if (opts.keyUp.isDown()) fwd += 1f;
        if (opts.keyDown.isDown()) fwd -= 1f;
        if (opts.keyRight.isDown()) str += 1f;
        if (opts.keyLeft.isDown()) str -= 1f;
        if (fwd == 0f && str == 0f) return lastDir;
        float yaw = (noRotate && capturingView) ? viewYaw : p.getYRot();
        double rad = Math.toRadians(yaw);
        double wx = -Math.sin(rad) * fwd - Math.cos(rad) * str;
        double wz = Math.cos(rad) * fwd - Math.sin(rad) * str;
        lastDir = new int[]{(int) Math.round(wx), (int) Math.round(wz)};
        return lastDir;
    }

    @Nullable
    private NeighborResult findNeighbor(int tx, int ty, int tz, boolean grim) {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        var eye = player != null ? player.getEyePosition() : null;
        var bestNeighbor = (BlockPos) null;
        var bestFace = Direction.UP;
        double bestDist = Double.MAX_VALUE;

        for (var face : Direction.values()) {
            int sx = tx + face.getStepX(), sy = ty + face.getStepY(), sz = tz + face.getStepZ();
            if (isAir(sx, sy, sz)) continue;
            var candidate = BlockUtility.pos(sx, sy, sz);
            var clickFace = face.getOpposite();
            if (grim && eye != null) {
                boolean safe = switch (clickFace) {
                    case UP -> eye.y >= candidate.getY() + 1;
                    case DOWN -> eye.y <= candidate.getY();
                    case NORTH -> eye.z <= candidate.getZ();
                    case SOUTH -> eye.z >= candidate.getZ() + 1;
                    case WEST -> eye.x <= candidate.getX();
                    case EAST -> eye.x >= candidate.getX() + 1;
                    default -> false;
                };
                if (!safe) continue;
            }
            double dist = eye != null
                ? eye.distanceToSqr(SwingUtility.centerOf(candidate))
                : 0;
            if (dist < bestDist) {
                bestDist = dist;
                bestNeighbor = candidate;
                bestFace = clickFace;
            }
        }
        if (bestNeighbor == null) return null;
        return new NeighborResult(bestNeighbor, bestFace);
    }

    private boolean isAir(int x, int y, int z) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return false;
        var state = BlockUtility.getState(mc.getLevel(), x, y, z);
        return state.isAir() || BlockUtility.isBlock(state, "snow") || !state.getFluidState().isEmpty();
    }

    private static BlockHitResult hitFor(BlockPos nb, Direction face) {
        var faceVec = SwingUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        var hitVec = SwingUtility.centerOf(nb).add(faceVec.scale(0.5));
        return SwingUtility.hitResult(hitVec, face, nb);
    }

    private int findPlaceableSlot(MinecraftWrapper mc, LocalPlayer p, TargetResult target) {
        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.isEmpty() || !InventoryUtility.isBlockItem(stack)) continue;
            if (fallback == -1) fallback = i;
            if (canPlaceAt(p, mc.getLevel(), stack, target)) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.isEmpty() || !InventoryUtility.isBlockItem(stack)) continue;
            if (!canPlaceAt(p, mc.getLevel(), stack, target)) continue;
            int free = InventoryUtility.findEmptyHotbarSlot(p);
            if (free == -1) free = fallback;
            if (free == -1) {
                for (int j = 0; j < 9; j++) {
                    if (!InventoryUtility.isBlockItem(InventoryUtility.getItem(p, j))) {
                        free = j;
                        break;
                    }
                }
            }
            if (free == -1) return -1;
            InventoryUtility.clickSlot(mc, p, i, free, InventoryUtility.SWAP);
            return -1;
        }
        return fallback;
    }

    private static boolean canPlaceAt(LocalPlayer p, ClientLevel level, ItemStack stack,
                                      TargetResult target) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        var hit = hitFor(target.neighbor, target.face);
        var ctx = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack, hit);
        var state = bi.getBlock().getStateForPlacement(ctx);
        if (state == null) return false;
        if (!state.canSurvive(level, target.pos)) return false;
        return level.isUnobstructed(state, target.pos, CollisionContext.placementContext(p));
    }

    private record TargetResult(BlockPos pos, BlockPos neighbor, Direction face) {}

    private record NeighborResult(BlockPos neighbor, Direction face) {}
}
