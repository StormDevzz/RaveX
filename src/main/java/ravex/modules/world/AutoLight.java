package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoLight extends Module {
    public static final AutoLight INSTANCE = new AutoLight();
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    public final NumberParameter lightLevel = new NumberParameter("Light Level", 8, 0, 15, 1);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 500, 100, 2000, 50);
    public final BooleanParameter silent = new BooleanParameter("Silent Swap", true);
    private long lastPlaceTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < delay.getValue()) return;
        int torchSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
                torchSlot = i;
                break;
            }
        }
        if (torchSlot == -1) return;
        double r = range.getValue();
        BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        int targetLight = lightLevel.getValue().intValue();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.level.getBlockState(pos).isAir()) continue;
                    if (mc.level.getMaxLocalRawBrightness(pos) > targetLight) continue;
                    BlockPos placeOn = pos.above();
                    if (!mc.level.getBlockState(placeOn).isAir()) continue;
                    if (mc.level.getBlockState(pos).getShape(mc.level, pos).isEmpty()) continue;
                    Vec3 center = Vec3.atCenterOf(placeOn);
                    if (center.distanceToSqr(mc.player.getEyePosition()) > r * r) continue;
                    int prevSlot = mc.player.getInventory().getSelectedSlot();
                    mc.player.getInventory().setSelectedSlot(torchSlot);
                    BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(pos), Direction.UP, pos, false
                    );
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                    if (silent.getValue()) {
                        mc.player.getInventory().setSelectedSlot(prevSlot);
                    }
                    lastPlaceTime = now;
                    return;
                }
            }
        }
    }
}
