package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@Module(name = "AutoTool", category = "Player")
public class AutoTool {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Normal";
    @Parameter(name = "SwitchBack")
    public boolean switchBack = true;
    @Parameter(name = "PullFromInventory")
    public boolean pullFromInventory = true;

    private int prevSlot = -1;
    private boolean wasMining = false;

    public void onDisable() {
        restoreIfNeeded();
    }

    private void restoreIfNeeded() {
        if (switchBack && wasMining && prevSlot != -1) {
            var player = MinecraftWrapper.getWrapper().getPlayer();
            if (player != null) {
                if ("Silent".equals(swap))
                    InventoryUtility.silentSelectSlot(player, prevSlot);
                else
                    InventoryUtility.selectSlot(player, prevSlot);
            }
        }
        prevSlot = -1;
        wasMining = false;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) {
            restoreIfNeeded();
            return;
        }
        if (!(mc.getHitResult() instanceof net.minecraft.world.phys.BlockHitResult blockHit)) {
            restoreIfNeeded();
            return;
        }
        boolean active = mc.isAttackKeyDown() || (mc.getGameMode() != null && mc.getGameMode().isDestroying());
        if (!active) {
            restoreIfNeeded();
            return;
        }
        BlockState state = mc.getLevel().getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            restoreIfNeeded();
            return;
        }
        int slot = ToolUtility.findBestToolSlot(player, state);
        if (slot == -1 && pullFromInventory) {
            slot = pullBestTool(mc, player, state);
        }
        if (slot < 0) {
            restoreIfNeeded();
            return;
        }
        if (!wasMining) {
            prevSlot = InventoryUtility.getSelectedSlot(player);
            wasMining = true;
        }
        if ("Silent".equals(swap))
            InventoryUtility.silentSelectSlot(player, slot);
        else
            InventoryUtility.selectSlot(player, slot);
    }

    private int pullBestTool(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, BlockState state) {
        int selected = InventoryUtility.getSelectedSlot(player);
        float bestSpeed = InventoryUtility.getItem(player, selected).getDestroySpeed(state);
        int bestHotbar = -1;
        for (int i = 0; i < 9; i++) {
            if (i == selected) continue;
            ItemStack stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestHotbar = i;
            }
        }
        int bestInv = -1;
        float invSpeed = bestSpeed;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > invSpeed) {
                invSpeed = speed;
                bestInv = i;
            }
        }
        if (bestInv == -1) return bestHotbar;
        int free = InventoryUtility.findEmptyHotbarSlot(player);
        if (free != -1) {
            InventoryUtility.handleInventoryClick(mc, player, bestInv, free, InventoryUtility.SWAP);
            return free;
        }
        int target = bestHotbar != -1 ? bestHotbar : selected;
        if (target == selected && InventoryUtility.getItem(player, selected).getDestroySpeed(state) >= invSpeed) {
            return -1;
        }
        InventoryUtility.handleInventoryClick(mc, player, bestInv, target, InventoryUtility.SWAP);
        return target;
    }
}
