package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
public class AutoLight extends Module {
    public static final AutoLight INSTANCE = new AutoLight();
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    public final NumberParameter lightLevel = new NumberParameter("LightLevel", 8, 0, 15, 1);
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 500, 100, 2000, 50);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    private long lastPlaceTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < delay.getValue()) return;
        int torchSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "torch") || InventoryUtility.isItem(stack, "soul_torch")) {
                torchSlot = i;
                break;
            }
        }
        if (torchSlot == -1) return;
        double r = range.getValue();
        var playerPos = mc.player.blockPosition();
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
                    var pos = BlockUtility.pos(x, y, z);
                    var state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (mc.level.getMaxLocalRawBrightness(pos) > targetLight) continue;
                    int aboveY = BlockUtility.aboveY(y);
                    var placeOn = BlockUtility.pos(x, aboveY, z);
                    if (!mc.level.getBlockState(placeOn).isAir()) continue;
                    if (state.getShape(mc.level, pos).isEmpty()) continue;
                    var center = net.minecraft.world.phys.Vec3.atCenterOf(placeOn);
                    if (center.distanceToSqr(mc.player.getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, torchSlot);
                    BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    if (silent.getValue()) {
                        InventoryUtility.selectSlot(mc.player, prevSlot);
                    }
                    lastPlaceTime = now;
                    return;
                }
            }
        }
    }
}
