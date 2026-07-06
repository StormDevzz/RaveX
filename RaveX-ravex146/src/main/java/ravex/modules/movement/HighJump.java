package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;

import java.util.List;

public class HighJump extends Module {
    public static final HighJump INSTANCE = new HighJump();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "GrimShulker"));
    public final NumberParameter height = new NumberParameter("Height", 2.0, 0.5, 10.0, 0.1);

    private HighJump() {
        super("HighJump", Category.MOVEMENT);
        addParameter(mode);
        addParameter(height);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.options.keyJump.isDown() && mc.player.onGround()) {
            if ("Vanilla".equals(mode.getValue())) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, height.getValue(), mc.player.getDeltaMovement().z);
            } else if ("GrimShulker".equals(mode.getValue())) {
                int shulkerSlot = findShulkerBox();
                if (shulkerSlot != -1) {
                    int oldSlot = ravex.manager.HotbarManager.INSTANCE.getSelectedSlot();
                    ravex.manager.HotbarManager.INSTANCE.swapToSlot(shulkerSlot);

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

                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, height.getValue(), mc.player.getDeltaMovement().z);

                    ravex.manager.HotbarManager.INSTANCE.swapToSlot(oldSlot);
                }
            }
        }
    }

    private int findShulkerBox() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 9; i++) {
            net.minecraft.world.item.ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                if (blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                    return i;
                }
            }
        }
        return -1;
    }
}
