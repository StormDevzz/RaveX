package ravex.utility.player;

import net.minecraft.client.Minecraft;

public class PlayerUtility {
    public static boolean isPlayerInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }

    public static boolean isOverVoid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        int minHeight = mc.level.getMinY();

        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(px, py, pz);
        for (int y = (int) py; y >= minHeight; y--) {
            pos.setY(y);
            if (!mc.level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }
}
