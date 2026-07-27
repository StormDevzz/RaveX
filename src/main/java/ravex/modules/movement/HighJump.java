package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "HighJump", category = "Movement")
public class HighJump {
    @Parameter(name = "Mode", modes = {"Vanilla", "GrimShulker"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 0.5, max = 10.0, step = 0.1)
    public double height = 2.0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.keyJump.isDown() && mc.player.onGround()) {
            if ("Vanilla".equals(mode)) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, height, mc.player.getDeltaMovement().z);
            } else if ("GrimShulker".equals(mode)) {
                int shulkerSlot = findShulkerBox();
                if (shulkerSlot != -1) {
                    int oldSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, shulkerSlot);
                    net.minecraft.core.BlockPos pos = mc.player.blockPosition().below();
                    net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, pos, false
                    );
                    mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND, hit, 0
                    ));
                    net.minecraft.core.BlockPos shulkerPos = pos.above();
                    net.minecraft.world.phys.BlockHitResult openHit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(shulkerPos.getX() + 0.5, shulkerPos.getY() + 0.5, shulkerPos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, shulkerPos, false
                    );
                    mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND, openHit, 0
                    ));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, height, mc.player.getDeltaMovement().z);
                    InventoryUtility.selectSlot(mc.player, oldSlot);
                }
            }
        }
    }
    private int findShulkerBox() {
        var mc = MinecraftWrapper.getInstance();
        for (int i = 0; i < 9; i++) {
            net.minecraft.world.item.ItemStack stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                if (blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                    return i;
                }
            }
        }
        return -1;
    }




}