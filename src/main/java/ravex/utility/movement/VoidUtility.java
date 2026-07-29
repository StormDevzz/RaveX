package ravex.utility.movement;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;

public class VoidUtility {
    public static boolean isFallingIntoVoid(LocalPlayer player) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.level == null) return false;
        return player.getY() < mc.level.getMinY() && player.getDeltaMovement().y < 0;
    }
}
