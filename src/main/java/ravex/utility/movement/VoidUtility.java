package ravex.utility.movement;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;

public class VoidUtility {
    public static boolean isFallingIntoVoid(LocalPlayer player) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return false;
        return player.getY() < mc.getLevel().getMinY() && player.getDeltaMovement().y < 0;
    }
}
