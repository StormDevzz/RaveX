package ravex.modules.world;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import java.util.HashSet;
import java.util.Set;
@ModuleInfo(name = "AutoReplant", category = "World")
public class AutoReplant extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 4.0, 1.0, 6.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 300, 100, 1000, 50);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    private long lastReplantTime = 0;
    private static final Set<Block> farmBlocks = new HashSet<>();
    static {
        farmBlocks.add(net.minecraft.world.level.block.Blocks.FARMLAND);
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastReplantTime < delay.getValue()) return;
        int seedSlot = findSeedSlot(mc);
        if (seedSlot == -1) return;
        double r = range.getValue();
        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (!state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;
                    if (net.minecraft.world.phys.Vec3.atCenterOf(pos).distanceToSqr(mc.player.getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, seedSlot);
                    BlockHitResult hit = new BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false
                    );
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    if (silent.getValue()) {
                        InventoryUtility.selectSlot(mc.player, prevSlot);
                    }
                    lastReplantTime = now;
                    return;
                }
            }
        }
    }
    private int findSeedSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
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
    public static AutoReplant itz() {
        return ravex.manager.ModuleManager.delegate(AutoReplant.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}