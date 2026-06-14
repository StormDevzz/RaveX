package ravex.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

import java.util.HashSet;
import java.util.Set;

public class AutoReplant extends Module {
    public static final AutoReplant INSTANCE = new AutoReplant();

    public final NumberParameter range = new NumberParameter("Range", 4.0, 1.0, 6.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 300, 100, 1000, 50);
    public final BooleanParameter silent = new BooleanParameter("Silent Swap", true);

    private long lastReplantTime = 0;

    private static final Set<Block> farmBlocks = new HashSet<>();
    static {
        farmBlocks.add(Blocks.FARMLAND);
    }

    private AutoReplant() {
        super("AutoReplant", Category.WORLD);
        addParameter(range);
        addParameter(delay);
        addParameter(silent);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        if (now - lastReplantTime < delay.getValue()) return;

        int seedSlot = findSeedSlot(mc);
        if (seedSlot == -1) return;

        double r = range.getValue();
        BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (!state.is(Blocks.FARMLAND)) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;
                    if (Vec3.atCenterOf(pos).distanceToSqr(mc.player.getEyePosition()) > r * r) continue;

                    int prevSlot = mc.player.getInventory().getSelectedSlot();
                    mc.player.getInventory().setSelectedSlot(seedSlot);

                    BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(pos), Direction.UP, pos, false
                    );
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);

                    if (silent.getValue()) {
                        mc.player.getInventory().setSelectedSlot(prevSlot);
                    }

                    lastReplantTime = now;
                    return;
                }
            }
        }
    }

    private int findSeedSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null && id.getNamespace().equals("minecraft") &&
                    (id.getPath().endsWith("_seeds") || id.getPath().contains("seed"))) {
                    return i;
                }
            }
        }
        return -1;
    }
}
