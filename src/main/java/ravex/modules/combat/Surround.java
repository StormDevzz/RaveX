package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.animate.FadeAnimation;
import ravex.utility.render.animate.SlideAnimation;
import ravex.utility.render.animate.SizeAnimation;

import java.util.ArrayList;
import java.util.List;

public class Surround extends Module {
    public static final Surround INSTANCE = new Surround();

    public static final List<BlockPos> surroundBlocks = new ArrayList<>();
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 0.3f;
    public static float renderG = 0.8f;
    public static float renderB = 1.0f;
    public static Vec3 animatedCenter = null;

    public final ModeParameter mode = new ModeParameter("Mode", "Full",
        java.util.List.of("Full", "AntiFace", "Extra"));
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);

    private final FadeAnimation fadeAnim = new FadeAnimation();
    private final SizeAnimation sizeAnim = new SizeAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();
    private int placeTimer = 0;
    private boolean placed = false;

    private Surround() {
        super("Surround", Category.COMBAT);
        addParameter(mode);
        addParameter(autoDisable);
        addParameter(render);
        addParameter(animate);
        addParameter(color);
    }

    @Override
    protected void onEnable() {
        surroundBlocks.clear();
        renderAlpha = 0.0f;
        renderSize = 0.0;
        animatedCenter = null;
        placed = false;
        placeTimer = 0;
        fadeAnim.reset();
        sizeAnim.reset();
        slideAnim.reset();
    }

    @Override
    protected void onDisable() {
        surroundBlocks.clear();
        renderAlpha = 0.0f;
        renderSize = 0.0;
        animatedCenter = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            surroundBlocks.clear();
            renderAlpha = fadeAnim.update(false, 0.2f);
            renderSize = sizeAnim.update(false, 0.15);
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();

        int blockSlot = findBlockSlot(mc.player);
        if (blockSlot == -1) {
            surroundBlocks.clear();
            renderAlpha = fadeAnim.update(false, 0.2f);
            return;
        }

        List<BlockPos> targets = new ArrayList<>();
        String m = mode.getValue();
        switch (m) {
            case "Full" -> {
                targets.add(playerPos.north());
                targets.add(playerPos.south());
                targets.add(playerPos.east());
                targets.add(playerPos.west());
                targets.add(playerPos.north().east());
                targets.add(playerPos.north().west());
                targets.add(playerPos.south().east());
                targets.add(playerPos.south().west());
            }
            case "AntiFace" -> {
                Direction facing = mc.player.getDirection();
                targets.add(playerPos.relative(facing.getClockWise()));
                targets.add(playerPos.relative(facing.getCounterClockWise()));
                targets.add(playerPos.relative(facing.getOpposite()));
                targets.add(playerPos.relative(facing.getOpposite()).relative(facing.getClockWise()));
                targets.add(playerPos.relative(facing.getOpposite()).relative(facing.getCounterClockWise()));
            }
            case "Extra" -> {
                targets.add(playerPos.north());
                targets.add(playerPos.south());
                targets.add(playerPos.east());
                targets.add(playerPos.west());
                targets.add(playerPos.north().east());
                targets.add(playerPos.north().west());
                targets.add(playerPos.south().east());
                targets.add(playerPos.south().west());
                targets.add(playerPos.above());
            }
        }

        List<BlockPos> toPlace = new ArrayList<>();
        for (BlockPos target : targets) {
            if (mc.level.getBlockState(target).isAir()) {
                toPlace.add(target);
            }
        }

        surroundBlocks.clear();
        surroundBlocks.addAll(toPlace);

        // Animation & color
        if (render.getValue()) {
            int c = color.getValue();
            renderR = ((c >> 16) & 0xFF) / 255.0f;
            renderG = ((c >> 8) & 0xFF) / 255.0f;
            renderB = (c & 0xFF) / 255.0f;

            renderAlpha = fadeAnim.update(!toPlace.isEmpty(), 0.2f);
            renderSize = sizeAnim.update(!toPlace.isEmpty(), 0.15);

            if (animate.getValue() && !toPlace.isEmpty()) {
                double avgX = toPlace.stream().mapToDouble(b -> b.getX() + 0.5).average().orElse(0);
                double avgY = toPlace.stream().mapToDouble(b -> b.getY() + 0.5).average().orElse(0);
                double avgZ = toPlace.stream().mapToDouble(b -> b.getZ() + 0.5).average().orElse(0);
                animatedCenter = slideAnim.update(avgX, avgY, avgZ, 0.2);
            } else {
                animatedCenter = null;
            }
        } else {
            renderAlpha = 0.0f;
            renderSize = 0.0;
            animatedCenter = null;
        }

        // Place blocks with delay
        if (toPlace.isEmpty()) {
            if (autoDisable.getValue() && placed) {
                setEnabled(false);
            }
            return;
        }

        placeTimer++;
        if (placeTimer < 2) return;
        placeTimer = 0;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(blockSlot);

        for (BlockPos target : toPlace) {
            BlockPos neighbor = findNeighbor(target);
            if (neighbor == null) continue;

            Direction face = null;
            for (Direction d : Direction.values()) {
                if (target.equals(neighbor.relative(d))) {
                    face = d;
                    break;
                }
            }
            if (face == null) face = Direction.UP;

            Vec3 hitVec = Vec3.atCenterOf(neighbor)
                .add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
            BlockHitResult blockHit = new BlockHitResult(hitVec, face, neighbor, false);

            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
            mc.player.swing(InteractionHand.MAIN_HAND);
            placed = true;
        }

        if (prevSlot != blockSlot) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private BlockPos findNeighbor(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        for (Direction face : Direction.values()) {
            BlockPos side = pos.relative(face);
            if (!mc.level.getBlockState(side).isAir()) {
                return side;
            }
        }
        return pos.below();
    }

    private int findBlockSlot(net.minecraft.client.player.LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
