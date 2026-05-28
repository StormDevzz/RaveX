package ravex.modules.world;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.DependencyParameter;
import ravex.utility.render.animate.FadeAnimation;
import ravex.utility.render.animate.SlideAnimation;
import ravex.utility.render.animate.SizeAnimation;
import ravex.utility.render.animate.FillAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Scaffold extends Module {
    public static final Scaffold INSTANCE = new Scaffold();

    // ── Parameters ──────────────────────────────────────────────────────
    public final ModeParameter    mode      = new ModeParameter("Mode", "Normal", List.of("Normal", "Expand"));
    public final BooleanParameter tower     = new BooleanParameter("Tower",      true);
    public final BooleanParameter silentRot = new BooleanParameter("Silent Rot", true);
    public final BooleanParameter keepY     = new BooleanParameter("Keep Y",     false);
    public final BooleanParameter render    = new BooleanParameter("Render",     true);
    public final DependencyParameter<String, ModeParameter> animStyle = new DependencyParameter<>(
        new ModeParameter("Anim Style", "Fill Up", List.of("Fill Up", "Expand", "Shrink", "None")),
        render, true
    );
    public final DependencyParameter<Boolean, BooleanParameter> snapToGrid = new DependencyParameter<>(
        new BooleanParameter("Snap to Grid", true),
        render, true
    );

    // ── Animation utilities instances ────────────────────────────────────
    private final FadeAnimation fadeAnim = new FadeAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();

    // List of placed blocks for highly premium "tail" animations!
    public static final List<PlacedBlock> placedBlocks = new CopyOnWriteArrayList<>();

    public static class PlacedBlock {
        public final BlockPos pos;
        private final FadeAnimation fade = new FadeAnimation();
        private final SizeAnimation size = new SizeAnimation();
        private final FillAnimation fill = new FillAnimation();

        public PlacedBlock(BlockPos pos) {
            this.pos = pos;
            this.fade.reset();
            this.size.reset();
            this.fill.reset();
            
            // Start at 1.0 alpha and size and decay from there
            this.fade.update(true, 1.0f);
            this.size.update(true, 1.0f);
            this.fill.update(true, 1.0f);
        }

        public boolean update() {
            fade.update(false, 0.04f); // Smooth decay speed
            size.update(false, 0.04f);
            fill.update(true, 0.15f); // Animates towards 1.0
            return fade.getAlpha() > 0.005f;
        }

        public float getAlpha(float partialTicks) {
            return fade.getAlpha(partialTicks);
        }

        public double getSize(float partialTicks) {
            return size.getSize(partialTicks);
        }

        public double getFillProgress(float partialTicks) {
            return fill.getProgress(partialTicks);
        }
    }

    private double targetY = -1;
    private long   lastPlaceMs  = 0;
    private BlockPos targetBlockPos = null;

    private Scaffold() {
        super("Scaffold", Category.WORLD);
        addParameter(mode);
        addParameter(tower);
        addParameter(silentRot);
        addParameter(keepY);
        addParameter(render);
        addParameter(animStyle);
        addParameter(snapToGrid);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        targetY = (mc.player != null) ? Math.floor(mc.player.getY()) : -1;
        targetBlockPos = null;
        fadeAnim.reset();
        slideAnim.reset();
        placedBlocks.clear();
        ravex.manager.RotationManager.INSTANCE.reset();
    }

    @Override
    protected void onDisable() {
        ravex.manager.RotationManager.INSTANCE.reset();
        placedBlocks.clear();
    }

    @Override
    public void onTick() {
        // Update placing decay states
        placedBlocks.removeIf(b -> !b.update());

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            decayRender();
            return;
        }

        // ── Keep Y / tower ─────────────────────────────────────────────
        if (p.onGround()) targetY = Math.floor(p.getY());
        if (tower.getValue() && mc.options.keyJump.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
            targetY = Math.floor(p.getY());
        }

        // ── Find block in hotbar ───────────────────────────────────────
        int slot = findBlockSlot(p);
        if (slot == -1) { decayRender(); return; }

        // ── Compute target position ────────────────────────────────────
        BlockPos below = BlockPos.containing(
            p.getX(),
            (keepY.getValue() && targetY != -1) ? (targetY - 1) : (p.getY() - 1),
            p.getZ()
        );

        if (!isAir(below)) { decayRender(); return; }

        // Expand mode: prefer direction of movement
        BlockPos targetPos = below;
        if ("Expand".equals(mode.getValue())) {
            double dx = p.getDeltaMovement().x;
            double dz = p.getDeltaMovement().z;
            BlockPos dirOffset = below.offset(
                dx > 0.05 ? 1 : (dx < -0.05 ? -1 : 0), 0,
                dz > 0.05 ? 1 : (dz < -0.05 ? -1 : 0)
            );
            if (isAir(dirOffset)) targetPos = dirOffset;
        }

        // ── Update render state ────────────────────────────────────────
        if (render.getValue()) {
            fadeAnim.update(true, 0.18f);
            slideAnim.update(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0.25);
            targetBlockPos = targetPos;
        } else {
            decayRender();
        }

        // ── Find adjacent support block ────────────────────────────────
        BlockPos neighbor = null;
        Direction placeFace = null;
        for (Direction face : Direction.values()) {
            BlockPos side = targetPos.relative(face);
            if (!isAir(side)) { neighbor = side; placeFace = face.getOpposite(); break; }
        }
        if (neighbor == null) { neighbor = targetPos.below(); placeFace = Direction.UP; }

        // ── Silent Rotations ───────────────────────────────────────────
        float[] rots = rotationsTo(neighbor);
        if (silentRot.getValue()) {
            ravex.manager.RotationManager.INSTANCE.setRotations(rots[0], rots[1]);
        } else {
            ravex.manager.RotationManager.INSTANCE.reset();
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }

        // ── Place block ────────────────────────────────────────────────
        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);

        Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
            new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
        );
        BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
        
        // Track the block states before useItemOn in a 3x3x3 scan area centered on targetPos
        BlockPos scanCenter = targetPos;
        java.util.Map<BlockPos, BlockState> beforeStates = new java.util.HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pPos = scanCenter.offset(x, y, z);
                    beforeStates.put(pPos, mc.level.getBlockState(pPos));
                }
            }
        }

        // Check if block was successfully placed by checking the interaction result
        InteractionResult result = mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
        if (result.consumesAction()) {
            p.swing(InteractionHand.MAIN_HAND);

            // Double check: scan the level states after placement to locate the exact position that changed to a block!
            BlockPos actualPlacedPos = null;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pPos = scanCenter.offset(x, y, z);
                        BlockState before = beforeStates.get(pPos);
                        BlockState after = mc.level.getBlockState(pPos);
                        if (before != null) {
                            boolean wasAir = before.isAir() || before.getBlock() == Blocks.SNOW || !before.getFluidState().isEmpty();
                            boolean isSolidNow = !after.isAir() && after.getBlock() != Blocks.SNOW && after.getFluidState().isEmpty();
                            if (wasAir && isSolidNow) {
                                actualPlacedPos = pPos;
                                break;
                            }
                        }
                    }
                    if (actualPlacedPos != null) break;
                }
                if (actualPlacedPos != null) break;
            }

            // Fallback to computed targetPos if no air block change was detected
            if (actualPlacedPos == null) {
                actualPlacedPos = targetPos;
            }

            // Add block to placed list ONLY on successful placement for premium visual tail effect
            if (render.getValue()) {
                placedBlocks.add(new PlacedBlock(actualPlacedPos));
            }
            lastPlaceMs = System.currentTimeMillis();
        }

        if (slot != prevSlot) p.getInventory().setSelectedSlot(prevSlot);
    }

    /** Smoothly decay the render highlight when not placing. */
    private void decayRender() {
        fadeAnim.update(false, 0.12f);
        ravex.manager.RotationManager.INSTANCE.reset();
        targetBlockPos = null;
    }

    // ── Getters for interpolated render properties ──────────────────────
    public float getRenderAlpha(float partialTicks) {
        return fadeAnim.getAlpha(partialTicks);
    }

    public Vec3 getHighlightPos(float partialTicks) {
        if (snapToGrid.getValue()) {
            if (targetBlockPos != null) {
                return new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ());
            }
            // Fallback snapped coordinates when fading out
            Vec3 slidePos = slideAnim.getPos(partialTicks);
            return new Vec3(Math.round(slidePos.x), Math.round(slidePos.y), Math.round(slidePos.z));
        }
        return slideAnim.getPos(partialTicks);
    }

    private boolean isAir(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.isAir() || state.getBlock() == Blocks.SNOW || !state.getFluidState().isEmpty();
    }

    private int findBlockSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private float[] rotationsTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return new float[]{0, 0};
        Vec3 target = Vec3.atCenterOf(pos);
        double dx = target.x - p.getX();
        double dy = (target.y + 0.5) - (p.getY() + p.getEyeHeight());
        double dz = target.z - p.getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);
        return new float[]{
            (float) Math.toDegrees(Math.atan2(-dx, dz)),
            (float)-Math.toDegrees(Math.atan2(dy, dist))
        };
    }
}
