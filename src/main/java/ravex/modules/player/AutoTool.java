package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoTool", category = "Player")
public class AutoTool {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Normal";
    @Parameter(name = "SwitchBack")
    public boolean switchBack = true;
    @Parameter(name = "PullFromInventory")
    public boolean pullFromInventory = true;

    private int prevSlot = -1;
    private boolean switched = false;
    private net.minecraft.core.BlockPos miningPos = null;
    // last seen slot, tells a manual change from our own
    private int lastKnownSlot = -1;

    public void onDisable() {
        restore();
    }

    private void restore() {
        if (switchBack && prevSlot != -1) {
            var player = MinecraftWrapper.getWrapper().getPlayer();
            if (player != null) {
                if ("Silent".equals(swap))
                    InventoryUtility.silentSelectSlot(player, prevSlot);
                else
                    InventoryUtility.selectSlot(player, prevSlot);
            }
        }
        prevSlot = -1;
        switched = false;
        miningPos = null;
        lastKnownSlot = -1;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) {
            prevSlot = -1;
            switched = false;
            miningPos = null;
            lastKnownSlot = -1;
            return;
        }

        int current = InventoryUtility.getSelectedSlot(player);
        if (switched && lastKnownSlot != -1 && current != lastKnownSlot) {
            prevSlot = current;
        }
        lastKnownSlot = current;

        // block broken: give the old slot back at once
        if (switched && miningPos != null && mc.getLevel().getBlockState(miningPos).isAir()) {
            restore();
            return;
        }

        boolean active = mc.isAttackKeyDown() || (mc.getGameMode() != null && mc.getGameMode().isDestroying());
        if (!active) return;
        if (!(mc.getHitResult() instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return;
        var state = BlockUtility.getState(mc.getLevel(), blockHit.getBlockPos());
        if (state.isAir()) return;
        int slot = ToolUtility.findBestToolSlot(player, state);
        if (slot == -1 && pullFromInventory) {
            slot = pullBestTool(mc, player, state);
        }
        if (slot < 0) {
            return;
        }
        if (!switched) {
            prevSlot = InventoryUtility.getSelectedSlot(player);
            switched = true;
        }
        miningPos = blockHit.getBlockPos();
        if ("Silent".equals(swap))
            InventoryUtility.silentSelectSlot(player, slot);
        else
            InventoryUtility.selectSlot(player, slot);
        // do not read our own switch as manual
        lastKnownSlot = InventoryUtility.getSelectedSlot(player);
    }

    private int pullBestTool(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player,
                             net.minecraft.world.level.block.state.BlockState state) {
        int selected = InventoryUtility.getSelectedSlot(player);
        float bestSpeed = InventoryUtility.getItem(player, selected).getDestroySpeed(state);
        int bestHotbar = -1;
        for (int i = 0; i < 9; i++) {
            if (i == selected) continue;
            var stack = InventoryUtility.getItem(player, i);
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
            var stack = InventoryUtility.getItem(player, i);
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
