package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.BiConsumer;

public class SelfTrap extends Module {
    public static final SelfTrap INSTANCE = new SelfTrap();

    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        Minecraft.getInstance().setScreen(new ravex.gui.blockbrowser.BlockBrowserScreen(
            Minecraft.getInstance().screen,
            SelfTrap.INSTANCE::isBlockSelected,
            SelfTrap.INSTANCE::setBlockSelected
        ));
    });

    public final ModeParameter mode = new ModeParameter("Mode", "Full",
        List.of("Full", "Simple", "Roof"));
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", true);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 1, 0, 5, 1);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", false);

    private final Set<Identifier> selectedBlocks = new HashSet<>();
    private int placeTimer = 0;
    private boolean placed = false;

    private SelfTrap() {
        super("SelfTrap", Category.COMBAT);
        addParameter(blocks);
        addParameter(mode);
        addParameter(autoDisable);
        addParameter(placeDelay);
        addParameter(rotate);
    }

    public boolean isBlockSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }

    public void setBlockSelected(Block block, boolean selected) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (selected) {
            selectedBlocks.add(id);
        } else {
            selectedBlocks.remove(id);
        }
    }

    @Override
    protected void onEnable() {
        placed = false;
        placeTimer = 0;
        if (selectedBlocks.isEmpty()) {
            selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.OBSIDIAN));
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();

        int blockSlot = findBestBlockSlot(mc.player);
        if (blockSlot == -1) return;

        List<BlockPos> targets = new ArrayList<>();
        String m = mode.getValue();
        switch (m) {
            case "Full" -> {
                targets.add(playerPos.north());
                targets.add(playerPos.south());
                targets.add(playerPos.east());
                targets.add(playerPos.west());
                targets.add(playerPos.above());
            }
            case "Simple" -> {
                targets.add(playerPos.north());
                targets.add(playerPos.south());
                targets.add(playerPos.east());
                targets.add(playerPos.west());
            }
            case "Roof" -> {
                targets.add(playerPos.above());
            }
        }

        List<BlockPos> toPlace = new ArrayList<>();
        for (BlockPos target : targets) {
            if (mc.level.getBlockState(target).isAir()) {
                toPlace.add(target);
            }
        }

        if (toPlace.isEmpty()) {
            if (autoDisable.getValue() && placed) {
                setEnabled(false);
            }
            return;
        }

        placeTimer++;
        if (placeTimer < placeDelay.getValue().intValue() + 1) return;
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

    private int findBestBlockSlot(net.minecraft.client.player.LocalPlayer player) {
        if (selectedBlocks.isEmpty()) return -1;
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (selectedBlocks.contains(id)) {
                bestSlot = i;
                break;
            }
        }
        if (bestSlot == -1) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
            }
        }
        return bestSlot;
    }
}
