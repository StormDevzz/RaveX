package ravex.utility.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SwingUtility {
    public static void swing(LocalPlayer player, InteractionHand hand) {
        if (player != null) player.swing(hand);
    }

    public static void swingMainHand(LocalPlayer player) {
        swing(player, InteractionHand.MAIN_HAND);
    }

    public static void swingOffHand(LocalPlayer player) {
        swing(player, InteractionHand.OFF_HAND);
    }

    public static void swingServer(LocalPlayer player, InteractionHand hand) {
        if (player != null && player.connection != null)
            player.connection.send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(hand));
    }

    public static Vec3 centerOf(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    public static Vec3 vec3(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static BlockHitResult hitResult(Vec3 loc, Direction face, BlockPos pos) {
        return new BlockHitResult(loc, face, pos, false);
    }
}
