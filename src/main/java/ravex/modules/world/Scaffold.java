package ravex.modules.world;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.manager.ModuleManager;
import ravex.utility.render.animate.EasingAnimation;
import ravex.utility.render.animate.SlideAnimation;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.SilentRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class Scaffold extends Module {
<<<<<<< HEAD
=======
    public static final Scaffold INSTANCE = new Scaffold();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Expand"));
    public final BooleanParameter tower = new BooleanParameter("Tower", true);
    public final BooleanParameter silentRot = new BooleanParameter("SilentRot", true);
    public final BooleanParameter keepY = new BooleanParameter("KeepY", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter highlightColor = new ColorParameter("Color", 0xFFFF33CC);
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 1.0f;
    public static float renderG = 0.2f;
    public static float renderB = 0.8f;
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    public static float silentYaw = 0.0f;
    public static float silentPitch = 0.0f;
    public static boolean hasSilentRotation = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private final EasingAnimation fadeAnim = new EasingAnimation();
    private final EasingAnimation sizeAnim = new EasingAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();
    private int currX, currY, currZ;
    private boolean hasCurr;

    public net.minecraft.core.BlockPos getCurrentPos() {
        return hasCurr ? ravex.utility.misc.block.BlockUtility.pos(currX, currY, currZ) : null;
    }
    private int lastSlot = -1;
    private double targetY = -1;

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            targetY = Math.floor(mc.player.getY());
        } else {
            targetY = -1;
        }
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
        fadeAnim.reset();
        sizeAnim.reset();
        slideAnim.reset();
    }
    @Override
    protected void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        if (p.onGround()) {
            targetY = Math.floor(p.getY());
        }
        if (tower.getValue() && mc.options.keyJump.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
            targetY = Math.floor(p.getY());
        }
        int slot = findBlockSlot(p);
        if (slot == -1) {
            hasCurr = false;
            return;
        }
<<<<<<< HEAD
        int bx = (int) Math.floor(p.getX());
        int by = (int) ((keepY.getValue() && targetY != -1) ? (targetY - 1) : (p.getY() - 1));
        int bz = (int) Math.floor(p.getZ());
        int tx = bx, ty = by, tz = bz;
=======
        BlockPos below = BlockPos.containing(
            p.getX(),
            (keepY.getValue() && targetY != -1) ? (targetY - 1) : (p.getY() - 1),
            p.getZ()
        );
        BlockPos targetPos = below;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if ("Expand".equals(mode.getValue())) {
            double dx = p.getDeltaMovement().x;
            double dz = p.getDeltaMovement().z;
            int offX = dx > 0.05 ? 1 : (dx < -0.05 ? -1 : 0);
            int offZ = dz > 0.05 ? 1 : (dz < -0.05 ? -1 : 0);
            int ex = bx + offX, ez = bz + offZ;
            if (isAir(ex, by, ez)) { tx = ex; tz = ez; }
        }
<<<<<<< HEAD
        if (!isAir(tx, ty, tz)) {
            hasCurr = false;
            return;
        }
        currX = tx; currY = ty; currZ = tz; hasCurr = true;
=======
        if (!isAir(targetPos)) {
            currentTarget = null;
            return;
        }
        currentTarget = targetPos;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (render.getValue()) {
            int hc = highlightColor.getValue();
            renderR = ((hc >> 16) & 0xFF) / 255.0f;
            renderG = ((hc >> 8) & 0xFF) / 255.0f;
            renderB = (hc & 0xFF) / 255.0f;
        }
<<<<<<< HEAD
        var neighbor = (net.minecraft.core.BlockPos) null;
        var placeFace = net.minecraft.core.Direction.UP;
        for (var face : net.minecraft.core.Direction.values()) {
            int sx = tx + face.getStepX(), sy = ty + face.getStepY(), sz = tz + face.getStepZ();
            if (!isAir(sx, sy, sz)) {
                neighbor = BlockUtility.pos(sx, sy, sz);
=======
        BlockPos neighbor = null;
        Direction placeFace = null;
        for (Direction face : Direction.values()) {
            BlockPos side = targetPos.relative(face);
            if (!isAir(side)) {
                neighbor = side;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                placeFace = face.getOpposite();
                break;
            }
        }
        if (neighbor == null) {
            int by2 = BlockUtility.belowY(ty);
            neighbor = BlockUtility.pos(tx, by2, tz);
            placeFace = net.minecraft.core.Direction.UP;
        }
        if (silentRot.getValue()) {
<<<<<<< HEAD
            silentRotation.setAnglesTo(mc, neighbor.getCenter());
        } else {
            silentRotation.hasRotation = false;
        }
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, slot);
        var center = Vec3.atCenterOf(neighbor);
        var hitVec = center.add(new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5));
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(hitVec, placeFace, neighbor, false));
        BlockUtility.swing(mc);
        if (slot != prevSlot) InventoryUtility.selectSlot(p, prevSlot);
    }
    private boolean isAir(int x, int y, int z) {
=======
            float[] rots = rotationsTo(neighbor);
            silentYaw = rots[0];
            silentPitch = rots[1];
            hasSilentRotation = true;
        } else {
            hasSilentRotation = false;
        }
        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
            new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
        );
        BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
        p.swing(InteractionHand.MAIN_HAND);
        if (slot != prevSlot) p.getInventory().setSelectedSlot(prevSlot);
    }
    private boolean isAir(BlockPos pos) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        var state = BlockUtility.getState(mc.level, x, y, z);
        return state.isAir() || BlockUtility.isBlock(state, "snow") || !state.getFluidState().isEmpty();
    }
<<<<<<< HEAD
    private int findBlockSlot(net.minecraft.client.player.LocalPlayer p) {
=======
    private int findBlockSlot(LocalPlayer p) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (!stack.isEmpty() && InventoryUtility.isBlockItem(stack)) return i;
        }
        return -1;
    }
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(Scaffold.class);
    }
    public static Scaffold itz() {
        return ModuleManager.get(Scaffold.class);
=======
    private float[] rotationsTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return new float[]{0, 0};
        Vec3 target = Vec3.atCenterOf(pos);
        double dx = target.x - p.getX();
        double dy = (target.y + 0.5) - (p.getY() + p.getEyeHeight());
        double dz = target.z - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
